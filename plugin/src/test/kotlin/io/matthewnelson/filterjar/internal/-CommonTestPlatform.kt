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

import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipFile

internal fun testFilterDSL(
    group: String = "group",
    artifact: String = "artifact",
): RealFilterJarConfigDSL = RealFilterJarConfigDSL.of(group, artifact)

@Suppress("unused")
internal fun zipToJar(zipFile: File, jarFile: File) {
    val zip = ZipFile(zipFile)
    val buf = ByteArray(DEFAULT_BUFFER_SIZE)

    JarOutputStream(jarFile.outputStream()).use { oStream ->
        zip.entries().iterator().forEach { entry ->
            oStream.putNextEntry(JarEntry(entry))
            zip.getInputStream(entry).use { iStream ->
                while (true) {
                    val read = iStream.read(buf)
                    if (read == -1) break
                    oStream.write(buf, 0, read)
                }
            }
            oStream.closeEntry()
        }
    }
}
