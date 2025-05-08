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

import io.matthewnelson.filterjar.FilterJarPlugin.Companion.assignAttributeFiltered
import io.matthewnelson.filterjar.internal.RealFilterJarConfigDSL
import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 * Extension for configuring [FilterJarConfig] to filter out things from dependency
 * Jar files.
 *
 * e.g.
 *
 *     filterJar {
 *         filter(group = "io.matthewnelson.kmp-tor", artifact = "resource-tor-exec") {
 *             exclude("io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android") {
 *                 keep("/aarch64")
 *                 keep("/x86/") // << Notice the trailing slash so x86_64 is excluded
 *             }
 *         }
 *
 *         // logs:
 *         // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/]
 *         // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/aarch64/]
 *         // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/aarch64/tor.gz]
 *         // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/armv7/]
 *         // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/armv7/tor.gz]
 *         // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86/]
 *         // ---KEEP[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86/tor.gz]
 *         // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86_64/]
 *         // EXCLUDE[io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android/x86_64/tor.gz]
 *     }
 *
 * The following [Configuration] will be enabled automatically when [FilterJarPlugin]
 * is applied to a project, depending on the presence of the following plugins.
 *
 * ### KotlinMultiplatform Plugin
 *
 * When present, the following configurations for all [KotlinJvmTarget] compilations
 * are configured:
 *  - [KotlinCompilation.compileDependencyConfigurationName]
 *  - [KotlinCompilation.runtimeDependencyConfigurationName]
 *
 * ### Java Plugin
 *
 * When present (in the absence of a multiplatform plugin), the following configurations
 * for all [SourceSet] are configured:
 *  - [SourceSet.getAnnotationProcessorConfigurationName]
 *  - [SourceSet.getCompileClasspathConfigurationName]
 *  - [SourceSet.getRuntimeClasspathConfigurationName]
 *
 * To activate a custom [Configuration], see [activate]
 * To deactivate a [Configuration], see [deactivate]
 *
 * @see [FilterJarPlugin]
 * */
@FilterJarDsl
public abstract class FilterJarExtension internal constructor(
    @JvmField
    public val enableLogging: Property<Boolean>,
    private val configs: MapProperty<String, RealFilterJarConfigDSL>,
) {

    internal companion object {
        internal const val NAME: String = "filterJar" // extension name
    }

    /**
     * Create a new filter
     *
     * **NOTE:** when filtering dependency Jar artifacts, the artifact's absolute path
     * is checked to contain [group]. Subsequently, the Jar artifact name is also checked
     * to start with [artifact]. This means an [artifact] argument of "resource-exec-tor" will
     * work on both "resource-exec-tor-jvm:version.jar" and "resource-exec-tor-gpl-jvm:version.jar"
     * artifacts.
     *
     * e.g.
     *
     *     filter(group = "io.matthewnelson.kmp-tor", artifact = "resource-exec-tor") {
     *         exclude("io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android")
     *     }
     *
     * @param [group] the dependency group name
     * @param [artifact] the dependency artifact name
     *
     * @see [filterGroup]
     *
     * @throws [IllegalArgumentException] when:
     *  - [group] is empty
     *  - [group] contains whitespace
     *  - [artifact] is empty
     *  - [artifact] contains whitespace
     *  - No [FilterJarConfig.DSL.exclude] was configured
     * */
    @FilterJarDsl
    public fun filter(group: String, artifact: String, action: Action<FilterJarConfig.DSL>) {
        val config = RealFilterJarConfigDSL.of(group, artifact)
        action.execute(config)
        config.checkValid()
        configs.put(config.name, config)
    }

    /**
     * Create multiple filters for artifacts with the same group
     *
     * e.g.
     *
     *     filterGroup(group = "io.matthewnelson.kmp-tor") {
     *         filter(artifact = "resource-exec-tor") {
     *             exclude("io/matthewnelson/kmp/tor/resource/exec/tor/native/linux-android")
     *         }
     *         filter(artifact = "resource-lib-tor") {
     *             exclude("io/matthewnelson/kmp/tor/resource/lib/tor/native") {
     *                 keep("/linux-libc/x86_64")
     *             }
     *         }
     *     }
     *
     * @param [group] the dependency group name
     *
     * @see [Group]
     *
     * @throws [IllegalArgumentException] when:
     *  - [group] is empty
     *  - [group] contains whitespace
     *  - No [FilterJarConfig] were configured for provided [Group]
     * */
    @FilterJarDsl
    public fun filterGroup(group: String, action: Action<Group>) {
        var wasConfigured = false
        var hasExecuted = false
        val g = object : Group(group) {
            override fun filter(artifact: String, action: Action<FilterJarConfig.DSL>) {
                if (hasExecuted) return
                this@FilterJarExtension.filter(group, artifact, action)
                wasConfigured = true
            }
        }

        try {
            action.execute(g)
        } finally {
            hasExecuted = true
        }

        require(wasConfigured) { "No filters were configured for group[$group]" }
    }

    /**
     * Activates functionality for provided [Configuration]
     *
     * e.g.
     *
     *     activate(configurations["customClasspath"])
     *
     * @param [resolvable] the configuration to activate
     * */
    @FilterJarDsl
    public fun activate(resolvable: Configuration) {
        resolvable.assignAttributeFiltered(active = true)
    }

    /**
     * Deactivates functionality for provided [Configuration]
     *
     * e.g.
     *
     *     deactivate(configurations["customClasspath"])
     *
     * @param [resolvable] the configuration to activate
     * */
    @FilterJarDsl
    public fun deactivate(resolvable: Configuration) {
        resolvable.assignAttributeFiltered(active = false)
    }

    /**
     * Configure multiple [FilterJarConfig] for artifacts with the same [group] name
     *
     * @see [filterGroup]
     * */
    @FilterJarDsl
    public abstract class Group
    @Throws(IllegalArgumentException::class)
    internal constructor(@JvmField public val group: String) {

        /**
         * Create a filter using provided [group] argument
         *
         * **NOTE:** when filtering dependency Jar artifacts, the artifact's absolute path
         * is checked to contain [group]. Subsequently, the Jar artifact name is also checked
         * to start with [artifact]. This means an [artifact] argument of "resource-exec-tor" will
         * work on both "resource-exec-tor-jvm:version.jar" and "resource-exec-tor-gpl-jvm:version.jar"
         * artifacts.
         *
         * @throws [IllegalArgumentException] when:
         *  - [artifact] is empty
         *  - [artifact] contains whitespace
         *  - [FilterJarConfig.DSL.exclude] was not configured
         * */
        @FilterJarDsl
        public abstract fun filter(artifact: String, action: Action<FilterJarConfig.DSL>)

        init { FilterJarConfig.checkGroup(group) }
    }
}
