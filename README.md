# Shlex in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fshlex--kotlin-blue.svg)](https://github.com/KotlinMania/shlex-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/shlex-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/shlex-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/shlex-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/shlex-kotlin/actions)

Kotlin Multiplatform line-by-line clean-room port of the Rust crate [`shlex`](https://crates.io/crates/shlex).

> **Status: scaffold — porting has not started.** This repo currently contains build infrastructure only. The upstream Rust source for the `shlex` crate will be cloned into `tmp/` (gitignored) when porting begins.

## About

Shell-style word splitting (POSIX `shlex`)

This port targets functional parity with the upstream Rust crate while presenting an idiomatic Kotlin Multiplatform API. Every Kotlin file is a faithful translation of an upstream Rust file and carries a `// port-lint: source <path>` header so the AST-distance tool can track provenance.

## Supported targets

- macOS arm64 / x64
- Linux x64
- Windows mingw-x64
- iOS arm64 / x64 / simulator-arm64
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

## Installation

Once published:

```kotlin
dependencies {
    implementation("io.github.kotlinmania:shlex-kotlin:0.1.0-SNAPSHOT")
}
```

## Build

```bash
./gradlew build
./gradlew test
```

## Porting guidelines

See [CLAUDE.md](CLAUDE.md) and [AGENTS.md](AGENTS.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

## License

Apache 2.0 — see [LICENSE](LICENSE).

Original work copyrighted by the upstream `shlex` authors. Kotlin port copyright (c) 2026 Sydney Renee and The Solace Project.
