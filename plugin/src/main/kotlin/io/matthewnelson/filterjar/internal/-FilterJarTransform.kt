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
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

@CacheableTransform
internal abstract class FilterJarTransform internal constructor(): TransformAction<FilterJarParameter> {

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    internal abstract val inputArtifact: Provider<FileSystemLocation>

    public override fun transform(outputs: TransformOutputs) {
        val oldJar = inputArtifact.get().asFile
        val oldName = oldJar.name

        val config = oldJar.findConfig(parameters.filterConfigs.get())

        if (config == null) {
            outputs.file(oldJar)
            return
        }

        val newName = oldName.substringBeforeLast('.') + "-filtered.jar"
        val newJar = outputs.file(newName)

        parameters.enableLogging.log { "\n    SOURCE[$oldName]\n    FILTER[$newJar]" }

        try {
            config.executeTransform(oldJar, newJar, parameters.enableLogging.get())
        } catch (t: Throwable) {
            throw RuntimeException("Failed to filter artifact[$oldName] by config[${config.name}]", t)
        }
    }

    internal companion object {

        @JvmSynthetic
        @Throws(RuntimeException::class)
        internal fun File.findConfig(configs: Set<FilterJarConfig>): FilterJarConfig? {
            val oldJar = absoluteFile
            val oldName = oldJar.name

            val filtered: List<FilterJarConfig> = configs.mapNotNull { config ->
                if (!oldJar.path.contains(config.group)) return@mapNotNull null
                if (oldName.startsWith(config.artifact)) return@mapNotNull config
                null
            }

            if (filtered.isEmpty()) return null

            if (filtered.size > 1) {
                val names = filtered.map { it.name }
                throw RuntimeException(
                    "Ambiguity in filter group/artifact parameters." +
                    " Multiple configs were found for artifact[$oldName] >> configs$names"
                )
            }

            return filtered.first()
        }

        @JvmSynthetic
        internal fun FilterJarConfig.executeTransform(oldJar: File, newJar: File, enableLogging: Boolean) {
            if (newJar.exists() && !newJar.delete()) {
                throw RuntimeException("Failed to delete duplicate jar file[$newJar]")
            }

            val sourceJar = JarFile(oldJar)
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)

            JarOutputStream(newJar.outputStream()).use { oStream ->
                sourceJar.entries().iterator().forEach { entry ->
                    var write = true
                    for ((exclude, keeps) in filters.entries) {
                        if (!entry.name.startsWith(exclude)) continue
                        write = keeps.firstOrNull { keep -> entry.name.startsWith(keep) } != null
                        if (write) enableLogging.log(prefix = false) { "      ---KEEP[${entry.name}]" }
                        break
                    }

                    if (!write) {
                        enableLogging.log(prefix = false) { "      EXCLUDE[${entry.name}]" }
                        return@forEach
                    }

                    try {
                        oStream.putNextEntry(JarEntry(entry.name))
                        sourceJar.getInputStream(entry).use { iStream ->
                            while (true) {
                                val read = iStream.read(buf)
                                if (read == -1) break
                                oStream.write(buf, 0, read)
                            }
                        }
                        oStream.closeEntry()
                    } catch (t: Throwable) {
                        throw RuntimeException("Failed to write $entry", t)
                    }
                }
            }
        }
    }
}
