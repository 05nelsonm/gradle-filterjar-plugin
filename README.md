# gradle-filterjar-plugin
[![badge-license]][url-license]

A Gradle Plugin to help Java developers reduce dependency jar sizes by filtering out unneeded things, such as 
native compilations that will not be used at runtime for a given release of a specific OS & Architecture.

Work is based off of [Craig Raw's][url-craig] plugin that [he wrote][url-sparrow-plugin] for [SparrowWallet][url-sparrow] 
to minimize the [kmp-tor-resource][url-kmp-tor-resource] dependency size by filtering out native compilations of `tor` 
resources that would not be used at runtime for the given application distribution.

[badge-license]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat

[url-license]: https://www.apache.org/licenses/LICENSE-2.0
[url-kmp-tor-resource]: https://github.com/05nelsonm/kmp-tor-resource
[url-craig]: https://github.com/craigraw
[url-sparrow]: https://github.com/sparrowwallet/sparrow
[url-sparrow-plugin]: https://github.com/sparrowwallet/sparrow/tree/master/buildSrc/src/main/java/com/sparrowwallet/filterjar
