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
 * Extension for configuring [FilterJarConfig] to filter out things from dependency Jar artifacts.
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
 * If no [KotlinJvmTarget] compilations are found, [FilterJarPlugin] will do nothing.
 *
 * ### Java Plugin
 *
 * When present (in the absence of a multiplatform plugin), the following configurations
 * for all [SourceSet] are configured:
 *  - [SourceSet.getAnnotationProcessorConfigurationName]
 *  - [SourceSet.getCompileClasspathConfigurationName]
 *  - [SourceSet.getRuntimeClasspathConfigurationName]
 *
 * If no [SourceSet] are found, [FilterJarPlugin] will do nothing. Subsequently, if an
 * Android Plugin is present, [FilterJarPlugin] will do nothing.
 *
 * @see [FilterJarApi]
 * @see [FilterJarPlugin]
 * @see [activate]
 * @see [deactivate]
 * */
@FilterJarDsl
public abstract class FilterJarExtension internal constructor(
    public final override val enableLogging: Property<Boolean>,
    private val configs: MapProperty<String, RealFilterJarConfigDSL>,
): FilterJarApi {

    internal companion object {
        internal const val NAME: String = "filterJar" // extension name
    }

    @FilterJarDsl
    public final override fun filter(group: String, artifact: String, action: Action<FilterJarConfig.DSL>) {
        val config = RealFilterJarConfigDSL.of(group, artifact)
        action.execute(config)
        configs.put(config.name, config.checkIsValid())
    }

    @FilterJarDsl
    public final override fun filterGroup(group: String, action: Action<FilterJarApi.Group>) {
        var wasConfigured = false
        var hasExecuted = false
        val g = object : FilterJarApi.Group(group) {
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

    @FilterJarDsl
    public final override fun activate(resolvable: Configuration) {
        resolvable.assignAttributeFiltered(active = true)
    }

    @FilterJarDsl
    public final override fun deactivate(resolvable: Configuration) {
        resolvable.assignAttributeFiltered(active = false)
    }
}
