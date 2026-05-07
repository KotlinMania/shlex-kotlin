// port-lint: source lib.rs
package io.github.kotlinmania.shlex

import io.github.kotlinmania.shlex.bytes.Shlex as BytesShlex
import io.github.kotlinmania.shlex.bytes.Quoter as BytesQuoter

// Copyright 2015 Nicholas Allegra (comex).
// Licensed under the Apache License, Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0> or
// the MIT license <https://opensource.org/licenses/MIT>, at your option. This file may not be
// copied, modified, or distributed except according to those terms.

/**
 * Rust `Iterator::Item` for [Shlex].
 *
 * This is a Kotlin transliteration of Rust associated types in `impl Iterator for Shlex`.
 */
public typealias Item = String

/**
 * Rust `Deref::Target` for [Shlex].
 *
 * This is a Kotlin transliteration of Rust associated types in `impl Deref for Shlex`.
 */
public typealias Target = BytesShlex

/**
 * Parse strings like, and escape strings for, POSIX shells.
 *
 * Same idea as (but implementation not directly based on) the Python shlex module.
 *
 * ## <span style="color:red">Warning</span>
 *
 * The [tryQuote]/[tryJoin] family of APIs does not quote control characters (because they cannot
 * be quoted portably).
 *
 * This is fully safe in noninteractive contexts, like shell scripts and `sh -c` arguments (or
 * even scripts `source`d from interactive shells).
 *
 * But if you are quoting for human consumption, you should keep in mind that ugly inputs produce
 * ugly outputs (which may not be copy-pastable).
 *
 * And if by chance you are piping the output of [tryQuote]/[tryJoin] directly to the stdin of an
 * interactive shell, you should stop, because control characters can lead to arbitrary command
 * injection.
 *
 * ## Compatibility
 *
 * This crate's quoting functionality tries to be compatible with **any POSIX-compatible shell**;
 * it's tested against `bash`, `zsh`, `dash`, Busybox `ash`, and `mksh`, plus `fish` (which is not
 * POSIX-compatible but close enough).
 *
 * It also aims to be compatible with Python `shlex` and C `wordexp`.
 */

/**
 * An iterator that takes an input string and splits it into the words using the same syntax as
 * the POSIX shell.
 *
 * See [io.github.kotlinmania.shlex.bytes.Shlex].
 */
public class Shlex(inStr: String) : Iterator<String> {
    private val inner: BytesShlex = BytesShlex(inStr.encodeToByteArray())

    public companion object {
        /** Create a new [Shlex] with the given input string. */
        public fun new(inStr: String): Shlex = Shlex(inStr)
    }

    /** The number of newlines read so far, plus one. (Forwarded from the bytes [Shlex].) */
    public val lineNo: Int get() = inner.lineNo

    /** Whether the underlying iterator hit an error. (Forwarded from the bytes [Shlex].) */
    public var hadError: Boolean
        get() = inner.hadError
        set(value) {
            inner.hadError = value
        }

    override fun hasNext(): Boolean = inner.hasNext()

    override fun next(): String {
        // Safety: given valid UTF-8, bytes.Shlex will always return valid UTF-8.
        return inner.next().decodeToString()
    }

    /**
     * Kotlin equivalent of Rust `Deref` for this wrapper.
     *
     * This exists to preserve the Rust surface-area mapping for port-lint and AST-distance tooling.
     */
    public fun deref(): BytesShlex = inner

    /**
     * Kotlin equivalent of Rust `DerefMut` for this wrapper.
     *
     * Kotlin does not have mutable references; this simply returns the underlying [BytesShlex].
     */
    public fun derefMut(): BytesShlex = inner
}

/**
 * Convenience function that consumes the whole string at once. Returns `null` if the input was
 * erroneous.
 */
public fun split(inStr: String): List<String>? {
    val shl = Shlex(inStr)
    val res: MutableList<String> = mutableListOf()
    while (shl.hasNext()) {
        res.add(shl.next())
    }
    return if (shl.hadError) null else res
}

/**
 * Errors from [Quoter.quote], [Quoter.join], etc. (and their [io.github.kotlinmania.shlex.bytes]
 * counterparts).
 *
 * By default, the only error that can be returned is [Nul]. If you call `allowNul(true)`, then no
 * errors can be returned at all. Any error variants added in the future will not be enabled by
 * default; they will be enabled through corresponding non-default [Quoter] options.
 *
 * ...In theory. In the unlikely event that additional classes of inputs are discovered that, like
 * nul bytes, are fundamentally hazardous to quote even for non-interactive shells, the risk will be
 * mitigated by adding corresponding [QuoteError] subclasses that *are* enabled by default.
 */
public sealed class QuoteError(message: String) : Exception(message) {
    /**
     * The input contained a nul byte. In most cases, shells fundamentally cannot handle strings
     * containing nul bytes, no matter how they are quoted. But if you're sure you can handle nul
     * bytes, you can call `allowNul(true)` on the [Quoter] to permit them to pass through.
     */
    public object Nul : QuoteError("cannot shell-quote string containing nul byte")

    /** Kotlin equivalent of Rust `Display::fmt` for this error. */
    public fun fmt(): String = message ?: toString()

    override fun toString(): String = message ?: super.toString()
}

/**
 * A more configurable interface to quote strings. If you only want the default settings you can
 * use the convenience functions [tryQuote] and [tryJoin].
 *
 * The bytes equivalent is [io.github.kotlinmania.shlex.bytes.Quoter].
 */
public data class Quoter(private val inner: BytesQuoter = BytesQuoter()) {
    /** Create a new [Quoter] with default settings. */
    public constructor() : this(BytesQuoter())

    /**
     * Set whether to allow nul bytes. By default they are not allowed and will result in an error
     * of [QuoteError.Nul].
     */
    public fun allowNul(allow: Boolean): Quoter = Quoter(inner.allowNul(allow))

    /**
     * Convenience function that consumes an iterable of words and turns it into a single string,
     * quoting words when necessary. Consecutive words will be separated by a single space.
     */
    public fun join(words: Iterable<String>): Result<String> {
        // Safety: given valid UTF-8, bytes.join() will always return valid UTF-8.
        return inner.join(words.map { it.encodeToByteArray() }).map { it.decodeToString() }
    }

    /** Given a single word, return a string suitable to encode it as a shell argument. */
    public fun quote(inStr: String): Result<String> {
        // Safety: given valid UTF-8, bytes.quote() will always return valid UTF-8.
        return inner.quote(inStr.encodeToByteArray()).map { it.decodeToString() }
    }

    /** Convert this [Quoter] to its bytes-level equivalent. */
    public fun toBytesQuoter(): BytesQuoter = inner

    public companion object {
        /** Create a new [Quoter] with default settings. */
        public fun new(): Quoter = Quoter()

        /** Wrap a [io.github.kotlinmania.shlex.bytes.Quoter] as a string [Quoter]. */
        public fun fromBytesQuoter(inner: BytesQuoter): Quoter = Quoter(inner)

        /** Rust `From<bytes::Quoter> for Quoter` transliteration. */
        public fun from(inner: BytesQuoter): Quoter = fromBytesQuoter(inner)
    }
}

/**
 * Convenience function that consumes an iterable of words and turns it into a single string,
 * quoting words when necessary. Consecutive words will be separated by a single space.
 *
 * Uses default settings except that nul bytes are passed through, which may be dangerous, leading
 * to this function being deprecated.
 *
 * Equivalent to `Quoter().allowNul(true).join(words).getOrThrow()`.
 *
 * (That configuration never returns a failure, so this function does not throw.)
 *
 * The bytes equivalent is [io.github.kotlinmania.shlex.bytes.join].
 */
@Deprecated(
    message = "replace with `tryJoin(words).getOrThrow()` to avoid nul byte danger",
    replaceWith = ReplaceWith("tryJoin(words).getOrThrow()"),
    level = DeprecationLevel.WARNING,
)
public fun join(words: Iterable<String>): String =
    Quoter().allowNul(true).join(words).getOrThrow()

/**
 * Convenience function that consumes an iterable of words and turns it into a single string,
 * quoting words when necessary. Consecutive words will be separated by a single space.
 *
 * Uses default settings. The only error that can be returned is [QuoteError.Nul].
 *
 * Equivalent to `Quoter().join(words)`.
 *
 * The bytes equivalent is [io.github.kotlinmania.shlex.bytes.tryJoin].
 */
public fun tryJoin(words: Iterable<String>): Result<String> =
    Quoter().join(words)

/**
 * Given a single word, return a string suitable to encode it as a shell argument.
 *
 * Uses default settings except that nul bytes are passed through, which may be dangerous, leading
 * to this function being deprecated.
 *
 * Equivalent to `Quoter().allowNul(true).quote(inStr).getOrThrow()`.
 *
 * (That configuration never returns a failure, so this function does not throw.)
 *
 * The bytes equivalent is [io.github.kotlinmania.shlex.bytes.quote].
 */
@Deprecated(
    message = "replace with `tryQuote(str).getOrThrow()` to avoid nul byte danger",
    replaceWith = ReplaceWith("tryQuote(inStr).getOrThrow()"),
    level = DeprecationLevel.WARNING,
)
public fun quote(inStr: String): String =
    Quoter().allowNul(true).quote(inStr).getOrThrow()

/**
 * Given a single word, return a string suitable to encode it as a shell argument.
 *
 * Uses default settings. The only error that can be returned is [QuoteError.Nul].
 *
 * Equivalent to `Quoter().quote(inStr)`.
 *
 * (That configuration never returns a failure, so this function does not throw.)
 *
 * The bytes equivalent is [io.github.kotlinmania.shlex.bytes.tryQuote].
 */
public fun tryQuote(inStr: String): Result<String> =
    Quoter().quote(inStr)
