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
import kotlin.test.*

internal class RealFilterJarConfigDSLUnitTest {

    @Test
    fun givenGroup_whenEmpty_thenThrowsException() {
        assertIllegalArgument { testFilterDSL("", "artifact") }
    }

    @Test
    fun givenGroup_whenWhitespace_thenThrowsException() {
        assertIllegalArgument { testFilterDSL(" ", "artifact") }
        assertIllegalArgument { testFilterDSL("\n", "artifact") }
    }

    @Test
    fun givenArtifact_whenEmpty_thenThrowsException() {
        assertIllegalArgument { testFilterDSL("group", "") }
    }

    @Test
    fun givenArtifact_whenWhitespace_thenThrowsException() {
        assertIllegalArgument { testFilterDSL("group", " ") }
        assertIllegalArgument { testFilterDSL("artifact", "\n") }
    }

    @Test
    fun givenExclude_whenPathBlank_thenThrowsException() {
        assertIllegalArgument { testFilterDSL().exclude("") }
        assertIllegalArgument { testFilterDSL().exclude("  ") }
    }

    @Test
    fun givenExclude_whenPathMultiLine_thenThrowsException() {
        assertIllegalArgument { testFilterDSL().exclude("""
            some
            path
            argument
        """.trimIndent()) }
    }

    @Test
    fun givenExclude_whenPathStartsWithSlash_thenThrowsException() {
        assertIllegalArgument { testFilterDSL().exclude("/something") }
    }

    @Test
    fun givenExclude_whenPathAlreadyConfigured_thenThrowsException() {
        val dsl = testFilterDSL()
        dsl.exclude("some/path")
        assertIllegalArgument { dsl.exclude("some/path") }
    }

    @Test
    fun givenExclude_whenPathStartsWithOtherExcludePath_thenThrowsException() {
        val dsl = testFilterDSL()
        dsl.exclude("some/path")
        dsl.exclude("some/path/already/covered")
        assertIllegalArgument { dsl.checkIsValid() }
    }

    @Test
    fun givenExcludeWithKeep_whenKeepExists_thenThrowsException() {
        var threwOnKeep = false
        testFilterDSL().exclude("some/path") { keep ->
            keep.keep("/this")
            assertIllegalArgument {
                try {
                    keep.keep("/this")
                } catch (t: Throwable) {
                    threwOnKeep = true
                    throw t
                }
            }
        }
        assertTrue(threwOnKeep)
    }

    @Test
    fun givenExcludeWithKeep_whenKeepStartsWithOtherKeep_thenThrowsException() {
        var threwOnKeep = false
        assertIllegalArgument {
            testFilterDSL().exclude("some/path") { keep ->
                keep.keep("/short")
                try {
                    keep.keep("/short/already/covered")
                } catch (t: Throwable) {
                    // Should NOT be the case here. Should throw after Action
                    // is executed (so on Keep lambda closure).
                    threwOnKeep = true
                    throw t
                }
            }
        }
        assertFalse(threwOnKeep)
    }

    @Test
    fun givenDSL_whenToFilterJarConfig_thenArgumentsAreTransferred() {
        val expected = FilterJarConfig(
            group = "group1",
            artifact = "artifact1",
            filters = mapOf("some/path" to setOf("some/path/keep")),
        )

        val dsl = testFilterDSL(expected.group, expected.artifact)
        dsl.exclude(expected.filters.keys.first()) { keep ->
            keep.keep("/keep")
        }
        val actual = dsl.checkIsValid().toFilterJarConfig()

        assertEquals(expected.group, actual.group)
        assertEquals(expected.artifact, actual.artifact)
        assertEquals(expected.filters, actual.filters)
    }

    private inline fun assertIllegalArgument(block: () -> Unit) {
        assertFailsWith<IllegalArgumentException> { block() }
    }
}
