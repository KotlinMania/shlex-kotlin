// port-lint: source bytes.rs
package io.github.kotlinmania.shlex.bytes

import io.github.kotlinmania.shlex.QuoteError

// Copyright 2015 Nicholas Allegra (comex).
// Licensed under the Apache License, Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0> or
// the MIT license <https://opensource.org/licenses/MIT>, at your option. This file may not be
// copied, modified, or distributed except according to those terms.

/**
 * `Shlex` and friends for byte strings.
 *
 * This is used internally by the [outer module][io.github.kotlinmania.shlex], and may be more
 * convenient if you are working with byte slices ([ByteArray]) or types that are wrappers around
 * bytes.
 *
 * Example (Unix-style invalid-UTF-8 input passed through `quote`):
 * ```kotlin
 * import io.github.kotlinmania.shlex.bytes.tryQuote
 * val osBytes = byteArrayOf(0x61, 0x80.toByte(), 0x62, 0x20, 0x63)
 * // tryQuote returns Result; non-nul bytes always succeed.
 * assertEquals(byteArrayOf(0x27, 0x61, 0x80.toByte(), 0x62, 0x20, 0x63, 0x27).toList(),
 *              tryQuote(osBytes).getOrThrow().toList())
 * ```
 */

private const val ASCII_MASK: Int = 0xFF

private fun byteAt(bytes: ByteArray, i: Int): Int = bytes[i].toInt() and ASCII_MASK

/**
 * An iterator that takes an input byte string and splits it into the words using the same syntax as
 * the POSIX shell.
 */
public class Shlex(private val inBytes: ByteArray) : Iterator<ByteArray> {
    private var index: Int = 0

    public companion object {
        /** Create a new [Shlex] with the given input bytes. */
        public fun new(inBytes: ByteArray): Shlex = Shlex(inBytes)
    }

    /** The number of newlines read so far, plus one. */
    public var lineNo: Int = 1

    /**
     * An input string is erroneous if it ends while inside a quotation or right after an
     * unescaped backslash. Since [Iterator] does not have a mechanism to return an error, if that
     * happens, [Shlex] just throws out the last token, ends the iteration, and sets [hadError] to
     * `true`; best to check it after you are done iterating.
     */
    public var hadError: Boolean = false

    private var lookahead: ByteArray? = null
    private var done: Boolean = false

    private fun parseWord(initial: Int): ByteArray? {
        var ch = initial
        val result: MutableList<Byte> = mutableListOf()
        while (true) {
            when (ch) {
                '"'.code -> if (!parseDouble(result)) {
                    hadError = true
                    return null
                }
                '\''.code -> if (!parseSingle(result)) {
                    hadError = true
                    return null
                }
                '\\'.code -> {
                    val ch2 = nextChar()
                    if (ch2 == -1) {
                        hadError = true
                        return null
                    }
                    if (ch2 != '\n'.code) result.add(ch2.toByte())
                }
                ' '.code, '\t'.code, '\n'.code -> {
                    return result.toByteArray()
                }
                else -> result.add(ch.toByte())
            }
            val ch2 = nextChar()
            if (ch2 == -1) return result.toByteArray()
            ch = ch2
        }
    }

    private fun parseDouble(result: MutableList<Byte>): Boolean {
        while (true) {
            val ch2 = nextChar()
            if (ch2 == -1) return false
            when (ch2) {
                '\\'.code -> {
                    val ch3 = nextChar()
                    if (ch3 == -1) return false
                    when (ch3) {
                        // \$ => $
                        '$'.code, '`'.code, '"'.code, '\\'.code -> result.add(ch3.toByte())
                        // \<newline> => nothing
                        '\n'.code -> {}
                        // \x => =x
                        else -> {
                            result.add('\\'.code.toByte())
                            result.add(ch3.toByte())
                        }
                    }
                }
                '"'.code -> return true
                else -> result.add(ch2.toByte())
            }
        }
    }

    private fun parseSingle(result: MutableList<Byte>): Boolean {
        while (true) {
            val ch2 = nextChar()
            if (ch2 == -1) return false
            when (ch2) {
                '\''.code -> return true
                else -> result.add(ch2.toByte())
            }
        }
    }

    private fun nextChar(): Int {
        if (index >= inBytes.size) return -1
        val res = byteAt(inBytes, index)
        index += 1
        if (res == '\n'.code) lineNo += 1
        return res
    }

    private fun computeNext(): ByteArray? {
        var ch = nextChar()
        if (ch == -1) return null
        // skip initial whitespace
        while (true) {
            when (ch) {
                ' '.code, '\t'.code, '\n'.code -> {}
                '#'.code -> {
                    while (true) {
                        val ch2 = nextChar()
                        if (ch2 == -1) break
                        if (ch2 == '\n'.code) break
                    }
                }
                else -> return parseWord(ch)
            }
            ch = nextChar()
            if (ch == -1) return null
        }
    }

    override fun hasNext(): Boolean {
        if (done) return false
        if (lookahead != null) return true
        lookahead = computeNext()
        if (lookahead == null) {
            done = true
            return false
        }
        return true
    }

    override fun next(): ByteArray {
        if (!hasNext()) throw NoSuchElementException()
        val item = lookahead!!
        lookahead = null
        return item
    }
}

/**
 * Convenience function that consumes the whole byte string at once. Returns `null` if the input
 * was erroneous.
 */
public fun split(inBytes: ByteArray): List<ByteArray>? {
    val shl = Shlex(inBytes)
    val res: MutableList<ByteArray> = mutableListOf()
    while (shl.hasNext()) {
        res.add(shl.next())
    }
    return if (shl.hadError) null else res
}

/**
 * A more configurable interface to quote strings. If you only want the default settings you can
 * use the convenience functions [tryQuote] and [tryJoin].
 *
 * The string equivalent is [io.github.kotlinmania.shlex.Quoter].
 */
public data class Quoter(
    private val allowNul: Boolean = false,
    // (Future extension point: additional options.)
) {
    public companion object {
        /** Create a new [Quoter] with default settings. */
        public fun new(): Quoter = Quoter()
    }

    /**
     * Set whether to allow nul bytes. By default they are not allowed and will result in an error
     * of [QuoteError.Nul].
     */
    public fun allowNul(allow: Boolean): Quoter = copy(allowNul = allow)

    /**
     * Convenience function that consumes an iterable of words and turns it into a single byte
     * string, quoting words when necessary. Consecutive words will be separated by a single space.
     */
    public fun join(words: Iterable<ByteArray>): Result<ByteArray> {
        val parts: MutableList<ByteArray> = mutableListOf()
        for (word in words) {
            val r = quote(word)
            if (r.isFailure) return Result.failure(r.exceptionOrNull()!!)
            parts.add(r.getOrThrow())
        }
        var totalLen = 0
        for (p in parts) totalLen += p.size
        if (parts.isNotEmpty()) totalLen += parts.size - 1
        val out = ByteArray(totalLen)
        var pos = 0
        for ((i, p) in parts.withIndex()) {
            if (i != 0) {
                out[pos] = ' '.code.toByte()
                pos += 1
            }
            p.copyInto(out, pos)
            pos += p.size
        }
        return Result.success(out)
    }

    /**
     * Given a single word, return a byte string suitable to encode it as a shell argument.
     *
     * If given valid UTF-8, this will never produce invalid UTF-8. This is because it only ever
     * inserts valid ASCII characters before or after existing ASCII characters (or returns two
     * single quotes if the input was an empty string). It will never modify a multibyte UTF-8
     * character.
     */
    public fun quote(inBytes: ByteArray): Result<ByteArray> {
        if (inBytes.isEmpty()) {
            // Empty string. Special case that is not meaningful as only part of a word.
            return Result.success(byteArrayOf('\''.code.toByte(), '\''.code.toByte()))
        }
        if (!allowNul) {
            for (b in inBytes) if (b == 0.toByte()) return Result.failure(QuoteError.Nul)
        }
        val out: MutableList<Byte> = mutableListOf()
        var pos = 0
        while (pos < inBytes.size) {
            // Pick a quoting strategy for some prefix of the input. Normally this will cover the
            // entire input, but in some case we might need to divide the input into multiple
            // chunks that are quoted differently.
            val (curLen, strategy) = quotingStrategy(inBytes, pos)
            if (curLen == inBytes.size - pos && strategy == QuotingStrategy.Unquoted && out.isEmpty()) {
                // Entire string can be represented unquoted. Reuse the buffer.
                return Result.success(inBytes.copyOf())
            }
            check(curLen > 0) { "no infinite loop" }
            appendQuotedChunk(out, inBytes, pos, curLen, strategy)
            pos += curLen
        }
        return Result.success(out.toByteArray())
    }
}

private enum class QuotingStrategy {
    /**
     * No quotes and no backslash escapes. (If backslash escapes would be necessary, we use a
     * different strategy instead.)
     */
    Unquoted,

    /** Single quoted. */
    SingleQuoted,

    /** Double quotes, potentially with backslash escapes. */
    DoubleQuoted,
    // (Future strategies could include the shell dollar-quoting and printf-substitution styles.)
}

/** Is this ASCII byte okay to emit unquoted? */
private fun unquotedOk(c: Int): Boolean {
    // Allowed characters: the punctuation `+ - . / : @ ] _`,
    //                     ASCII digits 0..9, uppercase A..Z, lowercase a..z.
    if (c == '+'.code || c == '-'.code || c == '.'.code || c == '/'.code ||
        c == ':'.code || c == '@'.code || c == ']'.code || c == '_'.code ||
        (c >= '0'.code && c <= '9'.code) ||
        (c >= 'A'.code && c <= 'Z'.code) ||
        (c >= 'a'.code && c <= 'z'.code)
    ) {
        return true
    }
    // Non-allowed characters:
    // From POSIX (Open Group base specifications, V3 chapter 2, "Shell Command Language"):
    // "The application shall quote the following characters if they are to represent themselves":
    //   pipe, ampersand, semicolon, less-than, greater-than, open-paren, close-paren,
    //   dollar, backtick, backslash, double quote, single quote, space, tab, newline.
    // "and the following may need to be quoted under certain circumstances":
    //   asterisk, question mark, open-bracket, hash, tilde, equals, percent.
    // Brace expansion: open-brace and close-brace (not yet in POSIX list; austingroupbugs#1193).
    // Comma is also quoted, just to be safe with brace expansion patterns.
    // Carriage return is allowed in a word by all real shells we tested, but is treated as a
    // word separator by Python shlex and might be translated to newline in interactive mode.
    // The bang and caret characters are treated specially in interactive mode; see quotingWarning.
    // Nul bytes and control characters in the ranges 0x00..0x1F and 0x7F.
    return false
}

/** Optimized version of [unquotedOk]. */
private val UNQUOTED_OK_TABLE: BooleanArray = BooleanArray(0x80).also { tbl ->
    for (i in 0 until 0x80) tbl[i] = unquotedOk(i)
}

private fun unquotedOkFast(c: Int): Boolean {
    return UNQUOTED_OK_TABLE[c]
}

/** Is this ASCII byte okay to emit in single quotes? */
private fun singleQuotedOk(c: Int): Boolean {
    return when (c) {
        // No single quotes in single quotes.
        '\''.code -> false
        // To work around a Bash bug, ^ is only allowed right after an opening single quote;
        // see quotingWarning.
        '^'.code -> false
        // Backslashes in single quotes are literal according to POSIX, but Fish treats them as
        // an escape character. Ban them. Fish does not aim to be POSIX-compatible, but we *can*
        // achieve Fish compatibility using double quotes, so we might as well.
        '\\'.code -> false
        else -> true
    }
}

/** Is this ASCII byte okay to emit in double quotes? */
private fun doubleQuotedOk(c: Int): Boolean {
    return when (c) {
        // Work around Python `shlex` bug where parsing "\`" and "\$" does not strip the
        // backslash, even though POSIX requires it.
        '`'.code, '$'.code -> false
        // '!' and '^' are treated specially in interactive mode; see quotingWarning.
        '!'.code, '^'.code -> false
        else -> true
    }
}

/**
 * Given an input, return a quoting strategy that can cover some prefix of the string, along with
 * the size of that prefix.
 *
 * Precondition: input size is nonzero. (Empty strings are handled by the caller.)
 * Postcondition: returned size is nonzero.
 */
private fun quotingStrategy(inBytes: ByteArray, start: Int): Pair<Int, QuotingStrategy> {
    val unquotedOkBit = 1
    val singleQuotedOkBit = 2
    val doubleQuotedOkBit = 4

    var prevOk = singleQuotedOkBit or doubleQuotedOkBit or unquotedOkBit
    var i = 0

    if (byteAt(inBytes, start) == '^'.code) {
        // To work around a Bash bug, ^ is only allowed right after an opening single quote;
        // see quotingWarning.
        prevOk = singleQuotedOkBit
        i = 1
    }

    while (start + i < inBytes.size) {
        val c = byteAt(inBytes, start + i)
        var curOk = prevOk

        if (c >= 0x80) {
            // Normally, non-ASCII characters should not require quoting, but see quotingWarning.md
            // about \xa0. For now, just treat all non-ASCII characters as requiring quotes. This
            // also ensures things are safe in the off-chance that the runtime is in a legacy 8-bit locale
            // that has additional characters satisfying `isblank`.
            curOk = curOk and unquotedOkBit.inv()
        } else {
            if (!unquotedOkFast(c)) curOk = curOk and unquotedOkBit.inv()
            if (!singleQuotedOk(c)) curOk = curOk and singleQuotedOkBit.inv()
            if (!doubleQuotedOk(c)) curOk = curOk and doubleQuotedOkBit.inv()
        }

        if (curOk == 0) {
            // There are no quoting strategies that would work for both the previous characters
            // and this one. So we have to end the chunk before this character. The caller will
            // call `quotingStrategy` again to handle the rest of the string.
            break
        }

        prevOk = curOk
        i += 1
    }

    // Pick the best allowed strategy.
    val strategy = when {
        (prevOk and unquotedOkBit) != 0 -> QuotingStrategy.Unquoted
        (prevOk and singleQuotedOkBit) != 0 -> QuotingStrategy.SingleQuoted
        (prevOk and doubleQuotedOkBit) != 0 -> QuotingStrategy.DoubleQuoted
        else -> error("unreachable")
    }
    check(i > 0)
    return Pair(i, strategy)
}

private fun appendQuotedChunk(
    out: MutableList<Byte>,
    inBytes: ByteArray,
    start: Int,
    len: Int,
    strategy: QuotingStrategy,
) {
    when (strategy) {
        QuotingStrategy.Unquoted -> {
            for (i in 0 until len) out.add(inBytes[start + i])
        }
        QuotingStrategy.SingleQuoted -> {
            out.add('\''.code.toByte())
            for (i in 0 until len) out.add(inBytes[start + i])
            out.add('\''.code.toByte())
        }
        QuotingStrategy.DoubleQuoted -> {
            out.add('"'.code.toByte())
            for (i in 0 until len) {
                val c = byteAt(inBytes, start + i)
                if (c == '$'.code || c == '`'.code || c == '"'.code || c == '\\'.code) {
                    // Add a preceding backslash.
                    // Note: We should not actually get here for $ and ` because they do not pass
                    // `doubleQuotedOk`.
                    out.add('\\'.code.toByte())
                }
                // Add the character itself.
                out.add(inBytes[start + i])
            }
            out.add('"'.code.toByte())
        }
    }
}

/**
 * Convenience function that consumes an iterable of words and turns it into a single byte string,
 * quoting words when necessary. Consecutive words will be separated by a single space.
 *
 * Uses default settings except that nul bytes are passed through, which may be dangerous, leading
 * to this function being deprecated.
 *
 * Equivalent to `Quoter().allowNul(true).join(words).getOrThrow()`.
 *
 * (That configuration never returns a failure, so this function does not throw.)
 *
 * The string equivalent is [io.github.kotlinmania.shlex.join].
 */
@Deprecated(
    message = "replace with `tryJoin(words).getOrThrow()` to avoid nul byte danger",
    replaceWith = ReplaceWith("tryJoin(words).getOrThrow()"),
    level = DeprecationLevel.WARNING,
)
public fun join(words: Iterable<ByteArray>): ByteArray =
    Quoter().allowNul(true).join(words).getOrThrow()

/**
 * Convenience function that consumes an iterable of words and turns it into a single byte string,
 * quoting words when necessary. Consecutive words will be separated by a single space.
 *
 * Uses default settings. The only error that can be returned is [QuoteError.Nul].
 *
 * Equivalent to `Quoter().join(words)`.
 *
 * The string equivalent is [io.github.kotlinmania.shlex.tryJoin].
 */
public fun tryJoin(words: Iterable<ByteArray>): Result<ByteArray> =
    Quoter().join(words)

/**
 * Given a single word, return a string suitable to encode it as a shell argument.
 *
 * Uses default settings except that nul bytes are passed through, which may be dangerous, leading
 * to this function being deprecated.
 *
 * Equivalent to `Quoter().allowNul(true).quote(inBytes).getOrThrow()`.
 *
 * (That configuration never returns a failure, so this function does not throw.)
 *
 * The string equivalent is [io.github.kotlinmania.shlex.quote].
 */
@Deprecated(
    message = "replace with `tryQuote(str).getOrThrow()` to avoid nul byte danger",
    replaceWith = ReplaceWith("tryQuote(inBytes).getOrThrow()"),
    level = DeprecationLevel.WARNING,
)
public fun quote(inBytes: ByteArray): ByteArray =
    Quoter().allowNul(true).quote(inBytes).getOrThrow()

/**
 * Given a single word, return a string suitable to encode it as a shell argument.
 *
 * Uses default settings. The only error that can be returned is [QuoteError.Nul].
 *
 * Equivalent to `Quoter().quote(inBytes)`.
 *
 * (That configuration never returns a failure, so this function does not throw.)
 *
 * The string equivalent is [io.github.kotlinmania.shlex.tryQuote].
 */
public fun tryQuote(inBytes: ByteArray): Result<ByteArray> =
    Quoter().quote(inBytes)
