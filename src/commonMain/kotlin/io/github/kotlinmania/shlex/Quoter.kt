// port-lint: source src/lib.rs
package io.github.kotlinmania.shlex

// Copyright 2015 Nicholas Allegra (comex).
// Licensed under the Apache License, Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0> or
// the MIT license <https://opensource.org/licenses/MIT>, at your option. This file may not be
// copied, modified, or distributed except according to those terms.

import io.github.kotlinmania.shlex.bytes.Quoter as BytesQuoter

/**
 * A more configurable interface to quote strings.  If you only want the default settings you can
 * use the convenience functions [tryQuote] and [tryJoin].
 *
 * The bytes equivalent is [io.github.kotlinmania.shlex.bytes.Quoter].
 */
// Lifted from upstream attributes: #[derive(Default, Debug, Clone)].
class Quoter private constructor(private val inner: BytesQuoter) {
    /** Create a new [Quoter] with default settings. */
    constructor() : this(BytesQuoter())

    /** Set whether to allow nul bytes.  By default they are not allowed and will result in an
     *  error of [QuoteError.Nul]. */
    fun allowNul(allow: Boolean): Quoter = Quoter(inner.allowNul(allow))

    /**
     * Convenience function that consumes an iterable of words and turns it into a single string,
     * quoting words when necessary. Consecutive words will be separated by a single space.
     */
    fun join(words: Iterable<String>): Result<String> {
        return inner.join(words.map { it.encodeToByteArray() })
            // Safety: given valid UTF-8, bytes::join() will always return valid UTF-8.
            .map { it.decodeToString() }
    }

    /** Given a single word, return a string suitable to encode it as a shell argument. */
    fun quote(inStr: String): Result<String> {
        return inner.quote(inStr.encodeToByteArray())
            // Safety: given valid UTF-8, bytes::quote() will always return valid UTF-8.
            .map { it.decodeToString() }
    }

    /** Companion conversion: get the underlying bytes-side [BytesQuoter]. */
    fun toBytesQuoter(): BytesQuoter = inner

    companion object {
        /** Lifted from upstream: `impl From<bytes::Quoter> for Quoter`. */
        fun fromBytesQuoter(inner: BytesQuoter): Quoter = Quoter(inner)
    }
}
