/*
 * Copyright (c) 2025 Matthew Nelson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package io.matthewnelson.filterjar.internal

import io.matthewnelson.filterjar.FilterJarConfig
import org.gradle.api.Action

internal class RealFilterJarConfigDSL private constructor(
    group: String,
    artifact: String,
): FilterJarConfig.DSL(group, artifact) {

    private val filters = LinkedHashMap<String, Set<String>>(1, 1.0f)

    public override fun exclude(path: String) {
        path.checkValid(isExclude = filters)
        filters[path] = emptySet()
    }

    public override fun exclude(path: String, action: Action<Keep>) {
        path.checkValid(isExclude = filters)

        val keeps = LinkedHashSet<String>(1, 1.0f)
        var hasExecuted = false
        val k = object : Keep(path) {
            override fun keep(suffix: String) {
                if (hasExecuted) return
                suffix.checkValid(isExclude = null)
                require(!suffix.startsWith(exclude)) { "keep[$suffix] cannot start with exclude[$exclude]" }
                require(keeps.add(suffix)) { "keep[$suffix] is already defined" }
            }
        }

        try {
            action.execute(k)
        } finally {
            hasExecuted = true
        }

        require(keeps.isNotEmpty()) { "No keep filters were configured for this exclude[$path]" }

        val sorted = keeps.sortedBy { it.length }

        sorted.forEach { keep ->
            sorted.forEach other@ { other ->
                if (keep == other) return@other
                require(!keep.startsWith(other)) { "keep[$keep] cannot start with another keep[$other]" }
                require(!other.startsWith(keep)) { "keep[$other] cannot start with another keep[$keep]"}
            }
        }

        filters[path] = sorted.mapTo(LinkedHashSet(keeps.size, 1.0f)) { path + it }
    }

    @JvmSynthetic
    @Throws(IllegalArgumentException::class)
    internal fun checkValid() {
        require(filters.isNotEmpty()) { "No exclusions have been defined for FilterJarConfig[$name]" }
        val sorted = filters.entries.sortedBy { it.key.length }
        sorted.forEach { (exclude, _) ->
            sorted.forEach other@ { (other, _) ->
                if (exclude == other) return@other
                require(!exclude.startsWith(other)) { "exclude[$exclude] cannot start with another exclude[$other]" }
                require(!other.startsWith(exclude)) { "exclude[$other] cannot start with another exclude[$exclude]" }
            }
        }
    }

    public override fun toString(): String = buildString {
        append("FilterJarConfig[group=")
        append(group)
        append(", artifact=")
        append(artifact)

        if (filters.isEmpty()) {
            append(']')
            return@buildString
        }

        append("]: [")
        filters.entries.forEach { (exclude, keeps) ->
            appendLine()
            append("    exclude[")
            append(exclude)
            if (keeps.isEmpty()) {
                append(']')
            } else {
                append("]: [")
                keeps.forEach { keep ->
                    appendLine()
                    append("        keep[")
                    append(keep)
                    append(']')
                }
                appendLine()
                append("    ]")
            }
        }
        appendLine()
        append("]")
    }

    @JvmSynthetic
    @Throws(IllegalArgumentException::class)
    internal fun toFilterJarConfig(): FilterJarConfig = FilterJarConfig(group, artifact, filters.toMap())

    internal companion object {

        @JvmSynthetic
        @Throws(IllegalArgumentException::class)
        internal fun of(
            group: String,
            artifact: String,
        ): RealFilterJarConfigDSL = RealFilterJarConfigDSL(group, artifact)

        @Throws(IllegalArgumentException::class)
        private fun String.checkValid(isExclude: Map<String, Set<String>>?) {
            require(isNotBlank()) { "argument cannot be blank" }
            require(indexOfFirst { it == '\n' || it == '\r' } == -1) { "argument cannot contain multiple lines" }

            if (isExclude == null) return
            require(first() != '/') { "first character cannot be /" }
            require(!isExclude.containsKey(this)){ "exclude[$this] is already defined" }
        }
    }
}
