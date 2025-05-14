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
// build.gradle.kts

plugins {
    // ...
    id("io.matthewnelson.filterjar") version("0.1.0")
}

// ...

filterJar {
    logging.set(true)

    // Define filters, e.g...
    filter(group = "io.matthewnelson.kmp-tor", artifact = "resource-lib-tor") {
        exclude("io/matthewnelson/kmp/tor/resource/lib/tor/native") {
            keep("/linux-libc/x86_64")
        }
    }

    // Define filters that share a group name
    filterGroup(group = "io.matthewnelson.kmp-tor") {
        filter(artifact = "resource-noexec-tor") {
            // Exclude all entries starting with this path
            exclude("io/matthewnelson/kmp/tor/resource/exec/tor/native") {
                // But keep entries within the exclude path that start with these
                keep("/linux-libc/x86_64")
            }
        }
    }
}
```

<!-- TAG_VERSION -->

```groovy
// build.gradle

plugins {
    id 'io.matthewnelson.filterjar' version '0.1.0'
}

filterJar {
    logging.set(true)

    // Define filters, e.g...
    filter("io.matthewnelson.kmp-tor", "resource-lib-tor") { config ->
        config.exclude("io/matthewnelson/kmp/tor/resource/lib/tor/native") { keep ->
            keep.keep("/linux-libc/x86_64")
        }
    }

    // Define filters that share a group name
    filterGroup("io.matthewnelson.kmp-tor") { group ->
        group.filter("resource-noexec-tor") { config ->
            // Exclude all entries starting with this path
            config.exclude("io/matthewnelson/kmp/tor/resource/exec/tor/native") { keep ->
                // But keep entries within the exclude path that start with these
                keep.keep("/linux-libc/x86_64")
            }
        }
    }
}
```

```

// --- logs ---
// FILTER_JAR: 
//     SOURCE[resource-lib-tor-jvm-408.16.1.jar]
//     FILTER[~/.gradle/caches/8.12.1/transforms/13767819281f195d2780f3a327bd2333-f6ef606a-cc20-43e2-a7a3-922f1319491f/transformed/resource-lib-tor-jvm-408.16.1-filtered.jar]
//       ---KEEP[io/matthewnelson/kmp/tor/resource/lib/tor/native/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-android/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-android/aarch64/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-android/aarch64/libtor.so.gz]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-android/armv7/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-android/armv7/libtor.so.gz]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-android/x86/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-android/x86/libtor.so.gz]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-android/x86_64/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-android/x86_64/libtor.so.gz]
//       ---KEEP[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-libc/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-libc/aarch64/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-libc/aarch64/libtor.so.gz]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-libc/armv7/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-libc/armv7/libtor.so.gz]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-libc/ppc64/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-libc/ppc64/libtor.so.gz]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-libc/x86/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-libc/x86/libtor.so.gz]
//       ---KEEP[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-libc/x86_64/]
//       ---KEEP[io/matthewnelson/kmp/tor/resource/lib/tor/native/linux-libc/x86_64/libtor.so.gz]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/macos/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/macos/aarch64/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/macos/aarch64/libtor.dylib.gz]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/macos/x86_64/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/macos/x86_64/libtor.dylib.gz]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/mingw/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/mingw/x86/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/mingw/x86/tor.dll.gz]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/mingw/x86_64/]
//       EXCLUDE[io/matthewnelson/kmp/tor/resource/lib/tor/native/mingw/x86_64/tor.dll.gz]
//
// ...
```

[badge-license]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat

[url-license]: https://www.apache.org/licenses/LICENSE-2.0
[url-kmp-tor-resource]: https://github.com/05nelsonm/kmp-tor-resource
[url-craig]: https://github.com/craigraw
[url-sparrow]: https://github.com/sparrowwallet/sparrow
[url-sparrow-plugin]: https://github.com/sparrowwallet/sparrow/commit/474f3a4e91ea28ed2a52131bc1909b919b73a8cb
