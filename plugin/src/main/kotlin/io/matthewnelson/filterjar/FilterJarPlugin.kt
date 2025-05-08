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
public open class FilterJarPlugin: Plugin<Project> {

    public override fun apply(target: Project) {
        val arguments = ExtensionArguments(target)

        target.extensions.create(
            FilterJarExtension.NAME,
            FilterJarExtension::class.java,
            arguments.enableLogging,
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

    private fun configureMultiplatform(project: Project, arguments: ExtensionArguments) {
        val jvmTargets = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            .targets
            .filterIsInstance<KotlinJvmTarget>()

        if (jvmTargets.isEmpty()) {
            arguments.enableLogging.log { "No KotlinMultiplatform Jvm targets found. Disabling..." }
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

    private fun configureJava(project: Project, arguments: ExtensionArguments) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        if (sourceSets.isEmpty()) {
            arguments.enableLogging.log { "No Java SourceSets found. Disabling..." }
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

    private fun List<String>.activateConfigurations(project: Project, arguments: ExtensionArguments) {
        val configs = arguments.configs.get()
        if (configs.isEmpty()) {
            arguments.enableLogging.log { "No FilterJarConfig have been created. Disabling..." }
            return
        }

        project.dependencies.artifactTypes.maybeCreate("jar").attributes.attribute(FILTERED, false)

        forEach { name ->
            val configuration = project.configurations.getByName(name)
            if (configuration.attributes.contains(FILTERED)) {
                arguments.enableLogging.log { "Attribute[${FILTERED.name}] present for $configuration" }
                return@forEach
            }
            configuration.assignAttributeFiltered(active = true)
        }

        project.dependencies.registerTransform(FilterJarTransform::class.java) { transform ->
            transform.from.attribute(FILTERED, false)
            transform.to.attribute(FILTERED, true)
            transform.parameters { parameters ->
                parameters.enableLogging.set(arguments.enableLogging.get())
                configs.values.forEach { config -> parameters.filterConfigs.add(config.toFilterJarConfig()) }
            }
        }
    }

    private class ExtensionArguments(project: Project) {
        val configs: MapProperty<String, RealFilterJarConfigDSL> = project
            .objects
            .mapProperty(String::class.java, RealFilterJarConfigDSL::class.java)

        val enableLogging: Property<Boolean> = project
            .objects
            .property(Boolean::class.javaObjectType)
            .apply { set(false) }
    }

    internal companion object {
        private val FILTERED = Attribute.of("io.matthewnelson.filterjar.filtered", Boolean::class.javaObjectType)

        @JvmSynthetic
        internal fun Configuration.assignAttributeFiltered(active: Boolean) {
            attributes.attribute(FILTERED, active)
        }
    }
}
