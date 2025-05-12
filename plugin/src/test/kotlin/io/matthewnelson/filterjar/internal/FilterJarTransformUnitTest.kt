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
import io.matthewnelson.filterjar.internal.FilterJarTransform.Companion.executeTransform
import io.matthewnelson.filterjar.internal.FilterJarTransform.Companion.findConfig
import org.junit.BeforeClass
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@RunWith(Enclosed::class)
internal class FilterJarTransformUnitTest {

    private companion object {
        private val CWD = File(".").absoluteFile.normalize()
    }

    class FindConfig {

        private val configs = listOf(
            testFilterDSL("io.matthewnelson.something", "something1").apply {
                exclude("io/matthewnelson/something1")
            },
            testFilterDSL("io.matthewnelson.something", "something2").apply {
                exclude("io/matthewnelson/something2")
            }
        ).mapTo(LinkedHashSet()) { dsl -> dsl.checkIsValid().toFilterJarConfig() }

        @Test
        fun givenArtifactPath_whenContainsGroupAndArtifact_thenReturnsConfig() {
            configs.forEach { config ->
                val artifact = CWD
                    .resolve("abc123")
                    .resolve(config.group)
                    .resolve("def456")
                    .resolve(config.artifact + "-0.1.0.jar")

                assertEquals(config, artifact.findConfig(configs))
            }
        }

        @Test
        fun givenArtifactPath_whenDoesNotContainGroup_thenReturnsNull() {
            val artifact = CWD
                .resolve("abc123")
                .resolve(configs.first().group.dropLast(1))
                .resolve("def456")
                .resolve(configs.first().artifact + "-0.1.0.jar")

            assertNull(artifact.findConfig(configs))
        }

        @Test
        fun givenArtifactPath_whenDoesNotContainArtifact_thenReturnsNull() {
            val artifact = CWD
                .resolve("abc123")
                .resolve(configs.first().group)
                .resolve("def456")
                .resolve(configs.first().artifact.dropLast(1) + "-0.1.0.jar")

            assertNull(artifact.findConfig(configs))
        }

        @Test
        fun givenArtifactPath_whenMultipleConfigsFound_thenThrowsException() {
            val configs = configs + testFilterDSL(configs.first().group, configs.first().artifact.dropLast(1)).apply {
                exclude(configs.first().filters.keys.first().dropLast(1))
            }.checkIsValid().toFilterJarConfig()

            assertEquals(3, configs.size)

            val artifact = CWD
                .resolve("abc123")
                .resolve(configs.first().group)
                .resolve("def456")
                .resolve(configs.first().artifact + "-0.1.0.jar")

            assertFailsWith<RuntimeException> { artifact.findConfig(configs) }
        }
    }

    class ExecuteTransform {

        companion object {
            private const val LOG = true

            private val RESOURCES = CWD
                .resolve("src")
                .resolve("test")
                .resolve("resources")

            private val TESTING_JAR = RESOURCES
                .resolve("testing.jar")

            private val TEST_DIR = CWD
                .resolve("build")
                .resolve("tmp")
                .resolve("test")
                .resolve("execute_transform")

            @JvmStatic
            @BeforeClass
            fun beforeClass() {
                if (!TEST_DIR.exists() && !TEST_DIR.mkdirs()) {
                    throw IOException("Failed mkdirs[$TEST_DIR]")
                }
            }
        }

        @Test
        fun givenJarFile_whenTransformed_thenEntryInformationIsPreserved() {
            val expected = TESTING_JAR.sha256()
            val newJar = TEST_DIR.resolve("preserve_info.jar")

            newConfig { exclude("will_never_hit") }.executeTransform(TESTING_JAR, newJar, LOG)
            assertEquals(expected, newJar.sha256())
        }

        private fun File.sha256(): String {
            require(name.endsWith(".jar")) { "File must be a .jar file" }

            val digest = MessageDigest.getInstance("SHA-256")
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)

            JarFile(this).use { jarFile ->
                jarFile.entries().iterator().forEach { entry ->
                    digest.apply {
                        update(entry.name.encodeToByteArray())
                        entry.creationTime?.let { update(it.toMillis().toString().encodeToByteArray()) }
                        entry.lastModifiedTime?.let { update(it.toMillis().toString().encodeToByteArray()) }
                        entry.lastAccessTime?.let { update(it.toMillis().toString().encodeToByteArray()) }
                        update(entry.time.toString().encodeToByteArray())

                        jarFile.getInputStream(entry).use { iStream ->
                            while (true) {
                                val read = iStream.read(buf)
                                if (read == -1) break
                                update(buf, 0, read)
                            }
                        }
                    }
                }
            }

            @OptIn(ExperimentalStdlibApi::class)
            return digest.digest().toHexString()
        }

        private fun newConfig(block: RealFilterJarConfigDSL.() -> Unit): FilterJarConfig {
            val dsl = testFilterDSL(group = "does.not.matter", artifact = "does.not.matter")
            block(dsl)
            return dsl.checkIsValid().toFilterJarConfig()
        }
    }
}
