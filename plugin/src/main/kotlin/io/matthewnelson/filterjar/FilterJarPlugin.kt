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

import io.matthewnelson.filterjar.internal.FilterJarTransform
import io.matthewnelson.filterjar.internal.RealFilterJarConfigDSL
import io.matthewnelson.filterjar.internal.log
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 * Plugin which aids KotlinMultiplatform JVM and Java projects with filtering
 * out unneeded things from dependency Jar artifacts via [TransformAction].
 *
 * @see [FilterJarExtension]
 * */
public open class FilterJarPlugin internal constructor(): Plugin<Project> {

    public final override fun apply(target: Project) {
        val arguments = ExtensionArgs(target)

        target.extensions.create(
            FilterJarExtension.NAME,
            FilterJarExtension::class.java,
            arguments.logging,
            arguments.configs,
        )

        target.afterEvaluate { project ->
            listOf(
                "org.jetbrains.kotlin.multiplatform" to ::configureMultiplatform,
                "java" to ::configureJava,
            ).forEach { (pluginId, configure) ->
                if (project.plugins.hasPlugin(pluginId)) {
                    configure(project, arguments)
                    return@afterEvaluate
                }
            }
        }
    }

    private fun configureMultiplatform(project: Project, arguments: ExtensionArgs) {
        val jvmTargets = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            .targets
            .filterIsInstance<KotlinJvmTarget>()

        if (jvmTargets.isEmpty()) {
            arguments.logging.log { "Disabling >> No KotlinMultiplatform Jvm targets found" }
            return
        }

        jvmTargets.flatMap { jvmTarget ->
            jvmTarget.compilations.flatMap { compilation ->
                listOf(
                    compilation.compileDependencyConfigurationName,
                    compilation.runtimeDependencyConfigurationName,
                )
            }
        }.activateConfigurations(project, arguments)
    }

    private fun configureJava(project: Project, arguments: ExtensionArgs) {
        val agp = listOf(
            "com.android.base",
            "com.android.library",
            "com.android.application",
        ).filter { pluginId -> project.plugins.hasPlugin(pluginId) }

        if (agp.isNotEmpty()) {
            // Java Android projects are not supported.
            arguments.logging.log { "Disabling >> The following Android plugins are present $agp" }
            return
        }

        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        if (sourceSets.isEmpty()) {
            arguments.logging.log { "Disabling >> No Java SourceSets found" }
            return
        }

        sourceSets.flatMap { sourceSet ->
            listOf(
                sourceSet.annotationProcessorConfigurationName,
                sourceSet.compileClasspathConfigurationName,
                sourceSet.runtimeClasspathConfigurationName,
            )
        }.activateConfigurations(project, arguments)
    }

    private fun List<String>.activateConfigurations(project: Project, arguments: ExtensionArgs) {
        val configs = arguments.configs.get().mapTo(LinkedHashSet()) { it.value.toFilterJarConfig() }
        if (configs.isEmpty()) {
            arguments.logging.log { "Disabling >> No FilterJarConfig have been created" }
            return
        }

        project.dependencies.artifactTypes.maybeCreate("jar").attributes.attribute(FILTERED, false)

        forEach { name ->
            val configuration = project.configurations.getByName(name)
            if (configuration.attributes.contains(FILTERED)) {
                arguments.logging.log { "Attribute[${FILTERED.name}] present for $configuration" }
                return@forEach
            }
            configuration.assignAttributeFiltered(active = true)
        }

        project.dependencies.registerTransform(FilterJarTransform::class.java) { transform ->
            transform.from.attribute(FILTERED, false)
            transform.to.attribute(FILTERED, true)
            transform.parameters { parameters ->
                parameters.logging.set(arguments.logging.get())
                parameters.filterConfigs.addAll(configs)
            }
        }
    }

    private class ExtensionArgs(project: Project) {
        val configs: MapProperty<String, RealFilterJarConfigDSL> = project
            .objects
            .mapProperty(String::class.java, RealFilterJarConfigDSL::class.java)

        val logging: Property<Boolean> = project
            .objects
            .property(Boolean::class.javaObjectType)
            .apply {
                val value = project
                    .properties["io.matthewnelson.filterjar.logging"]
                    ?.toString()
                    ?.toBooleanStrict()
                    ?: false

                set(value)
            }
    }

    internal companion object {
        private val FILTERED = Attribute.of("io.matthewnelson.filterjar.filtered", Boolean::class.javaObjectType)

        @JvmSynthetic
        internal fun Configuration.assignAttributeFiltered(active: Boolean) {
            attributes.attribute(FILTERED, active)
        }
    }
}
