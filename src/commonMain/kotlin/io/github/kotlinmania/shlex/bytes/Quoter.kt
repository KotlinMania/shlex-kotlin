// port-lint: source src/bytes.rs
package io.github.kotlinmania.shlex.bytes

// Copyright 2015 Nicholas Allegra (comex).
// Licensed under the Apache License, Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0> or
// the MIT license <https://opensource.org/licenses/MIT>, at your option. This file may not be
// copied, modified, or distributed except according to those terms.

import io.github.kotlinmania.shlex.QuoteError

/**
 * A more configurable interface to quote strings.  If you only want the default settings you can
 * use the convenience functions [tryQuote] and [tryJoin].
 *
 * The string equivalent is [io.github.kotlinmania.shlex.Quoter].
 */
class Quoter(
    private val allowNul: Boolean = false,
    // TODO: more options
) {
    /** Set whether to allow nul bytes.  By default they are not allowed and will result in an
     *  error of [QuoteError.Nul]. */
    fun allowNul(allow: Boolean): Quoter = Quoter(allowNul = allow)

    /**
     * Convenience function that consumes an iterable of words and turns it into a single byte
     * string, quoting words when necessary. Consecutive words will be separated by a single
     * space.
     */
    fun join(words: Iterable<ByteArray>): Result<ByteArray> {
        val quoted: MutableList<ByteArray> = mutableListOf()
        for (word in words) {
            val q = quote(word)
            val v = q.getOrElse { return Result.failure(it) }
            quoted.add(v)
        }
        val totalLen = quoted.sumOf { it.size } + (if (quoted.isEmpty()) 0 else quoted.size - 1)
        val out = ByteArray(totalLen)
        var pos = 0
        for ((idx, w) in quoted.withIndex()) {
            if (idx > 0) {
                out[pos] = ' '.code.toByte()
                pos += 1
            }
            w.copyInto(out, pos)
            pos += w.size
        }
        return Result.success(out)
    }

    /**
     * Given a single word, return a byte string suitable to encode it as a shell argument.
     *
     * If given valid UTF-8, this will never produce invalid UTF-8. This is because it only
     * ever inserts valid ASCII characters before or after existing ASCII characters (or
     * returns two single quotes if the input was an empty string). It will never modify a
     * multibyte UTF-8 character.
     */
    fun quote(inBytes: ByteArray): Result<ByteArray> {
        if (inBytes.isEmpty()) {
            // Empty string.  Special case that isn't meaningful as only part of a word.
            return Result.success(byteArrayOf('\''.code.toByte(), '\''.code.toByte()))
        }
        if (!allowNul && inBytes.any { it == 0.toByte() }) {
            return Result.failure(QuoteError.Nul)
        }
        val out: MutableList<Byte> = mutableListOf()
        var remaining = inBytes
        var offset = 0
        while (offset < remaining.size) {
            // Pick a quoting strategy for some prefix of the input.  Normally this will cover the
            // entire input, but in some case we might need to divide the input into multiple chunks
            // that are quoted differently.
            val slice = remaining.copyOfRange(offset, remaining.size)
            val (curLen, strategy) = quotingStrategy(slice)
            if (curLen == slice.size && strategy == QuotingStrategy.Unquoted && out.isEmpty()) {
                // Entire string can be represented unquoted.  Reuse the allocation.
                return Result.success(if (offset == 0) inBytes else slice)
            }
            val curChunk = slice.copyOfRange(0, curLen)
            check(slice.size - curLen < slice.size) // no infinite loop
            offset += curLen
            appendQuotedChunk(out, curChunk, strategy)
        }
        return Result.success(out.toByteArray())
    }
}

/**
 * Convenience function that consumes an iterable of words and turns it into a single byte string,
 * quoting words when necessary. Consecutive words will be separated by a single space.
 *
 * Uses default settings except that nul bytes are passed through, which may be dangerous (see
 * `quoting_warning#nul-bytes`), leading to this function being deprecated.
 *
 * Equivalent to `Quoter().allowNul(true).join(words).getOrThrow()`.
 *
 * (That configuration never returns an error, so this function does not panic.)
 *
 * The string equivalent is [io.github.kotlinmania.shlex.join].
 */
@Deprecated(
    "replace with `tryJoin(words)` to avoid nul byte danger",
    ReplaceWith("tryJoin(words)"),
    level = DeprecationLevel.WARNING,
)
fun join(words: Iterable<ByteArray>): ByteArray =
    Quoter().allowNul(true).join(words).getOrThrow()

/**
 * Convenience function that consumes an iterable of words and turns it into a single byte string,
 * quoting words when necessary. Consecutive words will be separated by a single space.
 *
 * Uses default settings.  The only error that can be returned is [QuoteError.Nul].
 *
 * Equivalent to `Quoter().join(words)`.
 *
 * The string equivalent is [io.github.kotlinmania.shlex.tryJoin].
 */
fun tryJoin(words: Iterable<ByteArray>): Result<ByteArray> = Quoter().join(words)

/**
 * Given a single word, return a string suitable to encode it as a shell argument.
 *
 * Uses default settings except that nul bytes are passed through, which may be dangerous (see
 * `quoting_warning#nul-bytes`), leading to this function being deprecated.
 *
 * Equivalent to `Quoter().allowNul(true).quote(inBytes).getOrThrow()`.
 *
 * (That configuration never returns an error, so this function does not panic.)
 *
 * The string equivalent is [io.github.kotlinmania.shlex.quote].
 */
@Deprecated(
    "replace with `tryQuote(bytes)` to avoid nul byte danger",
    ReplaceWith("tryQuote(inBytes)"),
    level = DeprecationLevel.WARNING,
)
fun quote(inBytes: ByteArray): ByteArray =
    Quoter().allowNul(true).quote(inBytes).getOrThrow()

/**
 * Given a single word, return a string suitable to encode it as a shell argument.
 *
 * Uses default settings.  The only error that can be returned is [QuoteError.Nul].
 *
 * Equivalent to `Quoter().quote(inBytes)`.
 *
 * The string equivalent is [io.github.kotlinmania.shlex.tryQuote].
 */
fun tryQuote(inBytes: ByteArray): Result<ByteArray> = Quoter().quote(inBytes)
