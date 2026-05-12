# shlex-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fshlex--kotlin-blue.svg)](https://github.com/KotlinMania/shlex-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/shlex-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/shlex-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/shlex-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/shlex-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`comex/rust-shlex`](https://github.com/comex/rust-shlex).

**Original Project:** This port is based on [`comex/rust-shlex`](https://github.com/comex/rust-shlex). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `comex/rust-shlex`

> The text below is reproduced and lightly edited from [`https://github.com/comex/rust-shlex`](https://github.com/comex/rust-shlex). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

[![ci badge]][ci link] [![crates.io badge]][crates.io link] [![docs.rs badge]][docs.rs link]

[crates.io badge]: https://img.shields.io/crates/v/shlex.svg?style=flat-square
[crates.io link]: https://crates.io/crates/shlex
[docs.rs badge]: https://img.shields.io/badge/docs-online-dddddd.svg?style=flat-square
[docs.rs link]: https://docs.rs/shlex
[ci badge]: https://img.shields.io/github/actions/workflow/status/comex/rust-shlex/test.yml?branch=master&style=flat-square
[ci link]: https://github.com/comex/rust-shlex/actions

Same idea as (but implementation not directly based on) the Python shlex
module. However, this implementation does not support any of the Python
module's customization because it makes parsing slower and is fairly useless.
You only get the default settings of shlex.split, which mimic the POSIX shell:
<https://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html>

This implementation also deviates from the Python version in not treating \r
specially, which I believe is more compliant.

This crate can be used on either normal Rust strings, or on byte strings with
the `bytes` module. The algorithms used are oblivious to UTF-8 high bytes, so
internally they all work on bytes directly as a micro-optimization.

Disabling the `std` feature (which is enabled by default) will allow the crate
to work in `no_std` environments, where the `alloc` crate, and a global
allocator, are available.

## LICENSE

The source code in this repository is Licensed under either of
- Apache License, Version 2.0, ([LICENSE-APACHE](https://github.com/comex/rust-shlex/blob/HEAD/LICENSE-APACHE) or
  https://www.apache.org/licenses/LICENSE-2.0)
- MIT license ([LICENSE-MIT](https://github.com/comex/rust-shlex/blob/HEAD/LICENSE-MIT) or
  https://opensource.org/licenses/MIT)

at your option.

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in the work by you, as defined in the Apache-2.0 license, shall
be dual licensed as above, without any additional terms or conditions.

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:shlex-kotlin:0.1.0-SNAPSHOT")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`comex/rust-shlex`](https://github.com/comex/rust-shlex). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the rust-shlex authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`comex/rust-shlex`](https://github.com/comex/rust-shlex) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
