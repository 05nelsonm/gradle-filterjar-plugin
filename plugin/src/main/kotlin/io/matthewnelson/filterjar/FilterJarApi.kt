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
import org.gradle.api.artifacts.Configuration

/**
 * [FilterJarExtension] Api for extensibility via delegation.
 *
 * e.g.
 *
 *     @FilterJarDsl
 *     public abstract class MyExtension internal constructor(
 *         private val delegate: FilterJarExtension,
 *     ): FilterJarApi by delegate {
 *
 *         @JvmField
 *         public val logging: Property<Boolean> = delegate.logging
 *
 *         // MyExtension extended functionality...
 *     }
 *
 * @see [FilterJarExtension]
 * */
@FilterJarDsl
public interface FilterJarApi {

    /**
     * Create a new filter
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
     * @see [FilterJarConfig.DSL.group]
     * @see [FilterJarConfig.DSL.artifact]
     *
     * @throws [IllegalArgumentException] when:
     *  - [group] is empty
     *  - [group] contains whitespace
     *  - [artifact] is empty
     *  - [artifact] contains whitespace
     *  - No [FilterJarConfig.DSL.exclude] was configured
     * */
    @FilterJarDsl
    public fun filter(group: String, artifact: String, action: Action<FilterJarConfig.DSL>)

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
     * @see [filter]
     * @see [Group]
     * @see [FilterJarConfig.DSL.group]
     * @see [FilterJarConfig.DSL.artifact]
     *
     * @throws [IllegalArgumentException] when:
     *  - [group] is empty
     *  - [group] contains whitespace
     *  - No [FilterJarConfig] were configured for provided [Group]
     * */
    @FilterJarDsl
    public fun filterGroup(group: String, action: Action<Group>)

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
    public fun activate(resolvable: Configuration)

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
    public fun deactivate(resolvable: Configuration)

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
