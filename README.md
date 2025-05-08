# gradle-filterjar-plugin
[![badge-license]][url-license]

A Gradle Plugin for Java & KotlinMultiplatform/Jvm projects to reduce dependency jar sizes by filtering out unneeded 
things, such as native compilations, that will not be used at runtime of a given release for a specific OS & Architecture.

Work is based off of [Craig Raw's][url-craig] plugin that [he wrote][url-sparrow-plugin] for [SparrowWallet][url-sparrow] 
to minimize the [kmp-tor-resource][url-kmp-tor-resource] dependency size by filtering out native compilations of `tor` 
resources that would not be used at runtime for the given application distribution.

### Get Started

<!-- TAG_VERSION -->

```kotlin
plugins {
    // ...
    id("io.matthewnelson.filterjar") version("0.1.0-alpha01")
}

// ...

filterJar {

    enableLogging.set(true)

    // Define filters, e.g...
    filter(group = "io.matthewnelson.kmp-tor", artifact = "resource-lib-tor") {
        exclude("io/matthewnelson/kmp/tor/resource/lib/tor/native") {
            keep("/linux-libc/aarch64")
            keep("/linux-libc/x86_64")
        }
    }

    // Define filters that share a group name
    filterGroup(group = "io.matthewnelson.kmp-tor") {
        filter(artifact = "resource-exec-tor") {
            exclude("io/matthewnelson/kmp/tor/resource/exec/tor/native") {
                keep("/linux-libc/aarch64")
                keep("/linux-libc/x86_64")
            }
        }
        filter(artifact = "resource-noexec-tor") {
            exclude("io/matthewnelson/kmp/tor/resource/noexec/tor/native/linux-libc/armv7/")
            exclude("io/matthewnelson/kmp/tor/resource/noexec/tor/native/linux-libc/ppc64/")
            exclude("io/matthewnelson/kmp/tor/resource/noexec/tor/native/linux-libc/x86/")
        }
    }
}
```

[badge-license]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat

[url-license]: https://www.apache.org/licenses/LICENSE-2.0
[url-kmp-tor-resource]: https://github.com/05nelsonm/kmp-tor-resource
[url-craig]: https://github.com/craigraw
[url-sparrow]: https://github.com/sparrowwallet/sparrow
[url-sparrow-plugin]: https://github.com/sparrowwallet/sparrow/commit/474f3a4e91ea28ed2a52131bc1909b919b73a8cb
