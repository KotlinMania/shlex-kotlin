// port-lint: source src/lib.rs
package io.github.kotlinmania.shlex

// Copyright 2015 Nicholas Allegra (comex).
// Licensed under the Apache License, Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0> or
// the MIT license <https://opensource.org/licenses/MIT>, at your option. This file may not be
// copied, modified, or distributed except according to those terms.

/**
 * Convenience function that consumes an iterable of words and turns it into a single string,
 * quoting words when necessary. Consecutive words will be separated by a single space.
 *
 * Uses default settings except that nul bytes are passed through, which may be dangerous (see
 * `quoting_warning#nul-bytes` in the upstream tree), leading to this function being deprecated.
 *
 * Equivalent to `Quoter().allowNul(true).join(words).getOrThrow()`.
 *
 * (That configuration never returns an error, so this function does not panic.)
 *
 * The bytes equivalent is [io.github.kotlinmania.shlex.bytes.join].
 */
@Deprecated(
    "replace with `tryJoin(words)` to avoid nul byte danger",
    ReplaceWith("tryJoin(words)"),
    level = DeprecationLevel.WARNING,
)
fun join(words: Iterable<String>): String =
    Quoter().allowNul(true).join(words).getOrThrow()

/**
 * Convenience function that consumes an iterable of words and turns it into a single string,
 * quoting words when necessary. Consecutive words will be separated by a single space.
 *
 * Uses default settings.  The only error that can be returned is [QuoteError.Nul].
 *
 * Equivalent to `Quoter().join(words)`.
 *
 * The bytes equivalent is [io.github.kotlinmania.shlex.bytes.tryJoin].
 */
fun tryJoin(words: Iterable<String>): Result<String> = Quoter().join(words)

/**
 * Given a single word, return a string suitable to encode it as a shell argument.
 *
 * Uses default settings except that nul bytes are passed through, which may be dangerous (see
 * `quoting_warning#nul-bytes` in the upstream tree), leading to this function being deprecated.
 *
 * Equivalent to `Quoter().allowNul(true).quote(inStr).getOrThrow()`.
 *
 * (That configuration never returns an error, so this function does not panic.)
 *
 * The bytes equivalent is [io.github.kotlinmania.shlex.bytes.quote].
 */
@Deprecated(
    "replace with `tryQuote(str)` to avoid nul byte danger",
    ReplaceWith("tryQuote(inStr)"),
    level = DeprecationLevel.WARNING,
)
fun quote(inStr: String): String =
    Quoter().allowNul(true).quote(inStr).getOrThrow()

/**
 * Given a single word, return a string suitable to encode it as a shell argument.
 *
 * Uses default settings.  The only error that can be returned is [QuoteError.Nul].
 *
 * Equivalent to `Quoter().quote(inStr)`.
 *
 * The bytes equivalent is [io.github.kotlinmania.shlex.bytes.tryQuote].
 */
fun tryQuote(inStr: String): Result<String> = Quoter().quote(inStr)
