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
package io.matthewnelson.filterjar

import org.gradle.api.Action
import org.gradle.api.Named
import java.io.Serializable

/**
 * See [DSL]
 * */
public class FilterJarConfig
@Throws(IllegalArgumentException::class)
internal constructor(
    internal val group: String,
    internal val artifact: String,
    internal val filters: Map<String, Set<String>>,
): Named, Serializable {

    /**
     * A DSL for creating [FilterJarConfig], accessible via [FilterJarExtension] after applying the plugin
     *
     * **NOTE:** When filtering dependency Jar artifacts, the artifact's absolute path is checked to see
     * if it contains [group]. Additionally, the Jar artifact's name is also checked to see if it starts
     * with [artifact]. For example, an [artifact] argument of "resource-exec-tor" will work for multiple
     * artifacts, such as "resource-exec-tor-jvm-{version}.jar" and "resource-exec-tor-gpl-jvm-{version}.jar".
     * */
    @FilterJarDsl
    public abstract class DSL
    @Throws(IllegalArgumentException::class)
    internal constructor(

        /**
         * The dependency's group parameter (e.g. "io.matthewnelson.kmp-tor")
         *
         * This is utilized when checking if a given Jar artifact has a [FilterJarConfig]
         * by seeing if the artifact's absolute path contains this value.
         * */
        @JvmField
        public val group: String,

        /**
         * The dependency's artifact parameter (e.g. "resource-exec-tor")
         *
         * This is utilized when checking if a given Jar artifact has a [FilterJarConfig]
         * by seeing if the artifact name starts with this value.
         * */
        @JvmField
        public val artifact: String,
    ): Named {

        /**
         * Add a rule to exclude all paths within the dependency Jar artifact that **start**
         * with the provided [path] argument.
         *
         * e.g.
         *
         *     exclude("io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android")
         *
         *     // logs:
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/aarch64/]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/aarch64/tor.gz]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/armv7/]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/armv7/tor.gz]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86/]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86/tor.gz]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86_64/]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86_64/tor.gz]
         *
         * @throws [IllegalArgumentException] when:
         *  - [path] is blank
         *  - [path] is multi-line
         *  - [path] starts with character /
         *  - [path] has already been defined
         * */
        @FilterJarDsl
        public abstract fun exclude(path: String)

        /**
         * Add a rule to exclude all paths within the dependency Jar artifact that **start**
         * with the provided [path] argument, but do **not** start with any of the configured
         * [Keep.keep] arguments.
         *
         * e.g.
         *
         *     exclude("io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android") {
         *         keep("/aarch64")
         *         keep("/x86/") // << Notice the trailing slash so x86_64 is excluded
         *     }
         *
         *     // logs:
         *     // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/]
         *     // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/aarch64/]
         *     // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/aarch64/tor.gz]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/armv7/]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/armv7/tor.gz]
         *     // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86/]
         *     // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86/tor.gz]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86_64/]
         *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86_64/tor.gz]
         *
         * @see [Keep]
         *
         * @throws [IllegalArgumentException] when:
         *  - [path] is blank
         *  - [path] is multi-line
         *  - [path] starts with character /
         *  - [path] has already been defined
         *  - [Keep.keep] was not configured
         *  - [Keep.keep] configurations are conflicting (e.g. keep['a', 'a/b'] >> 'a/b' is already kept by 'a')
         * */
        @FilterJarDsl
        public abstract fun exclude(path: String, action: Action<Keep>)

        /**
         * Helper for applying [keep] rules within an [exclude] definition
         * */
        @FilterJarDsl
        public abstract class Keep(@JvmField public val exclude: String) {

            /**
             * Add a [keep] rule whereby paths within [exclude] that match it will **not** be
             * excluded from the transformed Jar. Provided [suffix] argument is concatenated
             * with [exclude] path argument.
             *
             * **NOTE:** Be mindful of Jar filesystem separators, you may need to prefix your
             * [suffix] with a `/` character.
             *
             * e.g.
             *
             *     exclude("io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android") {
             *         keep("/aarch64")
             *         keep("/x86/") // << Notice the trailing '/' so x86_64 is still excluded
             *     }
             *
             *     // logs:
             *     // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/]
             *     // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/aarch64/]
             *     // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/aarch64/tor.gz]
             *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/armv7/]
             *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/armv7/tor.gz]
             *     // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86/]
             *     // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86/tor.gz]
             *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86_64/]
             *     // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86_64/tor.gz]
             *
             * @param [suffix] to append to [exclude] path to **not** exclude those entries
             *
             * @throws [IllegalArgumentException] when:
             *  - [suffix] is empty
             *  - [suffix] is multi-line
             *  - [suffix] has already been defined
             *  - [suffix] starts with [exclude]
             * */
            @FilterJarDsl
            public abstract fun keep(suffix: String)
        }

        public final override fun getName(): String = "$group:$artifact"

        public final override fun equals(other: Any?): Boolean {
            if (other !is FilterJarConfig) return false
            if (other::class != this::class) return false
            if (other.group != group) return false
            return other.artifact == artifact
        }

        public final override fun hashCode(): Int {
            var result = 17
            result = result * 31 + group.hashCode()
            result = result * 31 + artifact.hashCode()
            result = result * 31 + this::class.hashCode()
            return result
        }

        init {
            group.checkArgument { "group" }
            artifact.checkArgument { "artifact" }
        }
    }

    public override fun getName(): String = "$group:$artifact"

    public override fun equals(other: Any?): Boolean {
        if (other !is FilterJarConfig) return false
        if (other.group != group) return false
        if (other.artifact != artifact) return false
        return other.filters == filters
    }

    public override fun hashCode(): Int {
        var result = 17
        result = result * 31 + group.hashCode()
        result = result * 31 + artifact.hashCode()
        result = result * 31 + filters.hashCode()
        return result
    }

    public override fun toString(): String = "FilterJarConfig[name=$name]@${hashCode()}"

    init {
        group.checkArgument { "group" }
        artifact.checkArgument { "artifact" }
        require(filters.isNotEmpty()) { "filters cannot be empty" }
    }

    internal companion object {
        private inline fun String.checkArgument(lazyField: () -> String) {
            require(isNotEmpty()) { "${lazyField()} cannot be empty" }
            require(indexOfFirst { it.isWhitespace() } == -1) { "${lazyField()} cannot contain whitespace" }
        }

        @JvmSynthetic
        internal fun checkGroup(group: String) { group.checkArgument { "group" } }
    }
}
