// port-lint: source src/bytes.rs
package io.github.kotlinmania.shlex.bytes

// Copyright 2015 Nicholas Allegra (comex).
// Licensed under the Apache License, Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0> or
// the MIT license <https://opensource.org/licenses/MIT>, at your option. This file may not be
// copied, modified, or distributed except according to those terms.

internal enum class QuotingStrategy {
    /**
     * No quotes and no backslash escapes.  (If backslash escapes would be necessary, we use a
     * different strategy instead.)
     */
    Unquoted,

    /** Single quoted. */
    SingleQuoted,

    /** Double quotes, potentially with backslash escapes. */
    DoubleQuoted,
    // TODO: add $'xxx' and "$(printf 'xxx')" styles
}

/** Is this ASCII byte okay to emit unquoted? */
internal fun unquotedOk(c: Byte): Boolean {
    val ch = (c.toInt() and 0xFF).toChar()
    return when (ch) {
        // Allowed characters:
        '+', '-', '.', '/', ':', '@', ']', '_',
        in '0'..'9', in 'A'..'Z', in 'a'..'z',
        -> true

        // Non-allowed characters:
        // From POSIX https://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html
        // "The application shall quote the following characters if they are to represent themselves:"
        '|', '&', ';', '<', '>', '(', ')', '$', '`', '\\', '"', '\'', ' ', '\t', '\n',
        // "and the following may need to be quoted under certain circumstances[..]:"
        '*', '?', '[', '#', '~', '=', '%',
        // Brace expansion.  These ought to be in the POSIX list but aren't yet;
        // see: https://www.austingroupbugs.net/view.php?id=1193
        '{', '}',
        // Also quote comma, just to be safe in the extremely odd case that the user of this crate
        // is intentionally placing a quoted string inside a brace expansion, e.g.:
        //     "echo foo{a,b,${shlex::quote(some_str)}}"
        ',',
        // '\r' is allowed in a word by all real shells I tested, but is treated as a word
        // separator by Python `shlex` and might be translated to '\n' in interactive mode.
        '\r',
        // '!' and '^' are treated specially in interactive mode; see quoting_warning.
        '!', '^',
        // Nul bytes and control characters.
        in '\u0000'..'\u001f', '\u007f',
        -> false

        else -> false // unreachable; unquotedOk is only called for 0..128. Non-ASCII bytes are
        // handled separately in quotingStrategy.
    }
    // Note: The logic cited above for quoting comma might suggest that `..` should also be quoted,
    // as a special case of brace expansion.  But it's not necessary.  There are three cases:
    //
    // 1. The user wants comma-based brace expansion, but the untrusted string being quoted
    //    contains `..`, so they get something like `{foo,bar,3..5}`.
    //  => That's safe; both Bash and Zsh expand this to `foo bar 3..5` rather than
    //     `foo bar 3 4 5`.  The presence of commas disables sequence expression expansion.
    //
    // 2. The user wants comma-based brace expansion where the contents of the braces are a
    //    variable number of quoted strings and nothing else.  There happens to be exactly
    //    one string and it contains `..`, so they get something like `{3..5}`.
    //  => Then this will expand as a sequence expression, which is unintended.  But I don't mind,
    //     because any such code is already buggy.  Suppose the untrusted string *didn't* contain
    //     `,` or `..`, resulting in shell input like `{foo}`.  Then the shell would interpret it
    //     as the literal string `{foo}` rather than brace-expanding it into `foo`.
    //
    // 3. The user wants a sequence expression and wants to supply an untrusted string as one of
    //    the endpoints or the increment.
    //  => Well, that's just silly, since the endpoints can only be numbers or single letters.
}

/** Optimized version of [unquotedOk]. */
private val unquotedOkMask: BooleanArray = BooleanArray(128) { unquotedOk(it.toByte()) }

internal fun unquotedOkFast(c: Byte): Boolean {
    // Mask of all bytes in 0..<0x80 that pass.
    val i = c.toInt() and 0xFF
    return i < 128 && unquotedOkMask[i]
}

/** Is this ASCII byte okay to emit in single quotes? */
internal fun singleQuotedOk(c: Byte): Boolean = when (c) {
    // No single quotes in single quotes.
    '\''.code.toByte() -> false
    // To work around a Bash bug, ^ is only allowed right after an opening single quote; see
    // quoting_warning.
    '^'.code.toByte() -> false
    // Backslashes in single quotes are literal according to POSIX, but Fish treats them as an
    // escape character.  Ban them.  Fish doesn't aim to be POSIX-compatible, but we *can*
    // achieve Fish compatibility using double quotes, so we might as well.
    '\\'.code.toByte() -> false
    else -> true
}

/** Is this ASCII byte okay to emit in double quotes? */
internal fun doubleQuotedOk(c: Byte): Boolean = when (c) {
    // Work around Python `shlex` bug where parsing "\`" and "\$" doesn't strip the
    // backslash, even though POSIX requires it.
    '`'.code.toByte(), '$'.code.toByte() -> false
    // '!' and '^' are treated specially in interactive mode; see quoting_warning.
    '!'.code.toByte(), '^'.code.toByte() -> false
    else -> true
}

/**
 * Given an input, return a quoting strategy that can cover some prefix of the string, along with
 * the size of that prefix.
 *
 * Precondition: input size is nonzero.  (Empty strings are handled by the caller.)
 * Postcondition: returned size is nonzero.
 */
internal fun quotingStrategy(inBytes: ByteArray): Pair<Int, QuotingStrategy> {
    val unquotedOkBit: Int = 1
    val singleQuotedOkBit: Int = 2
    val doubleQuotedOkBit: Int = 4

    var prevOk: Int = singleQuotedOkBit or doubleQuotedOkBit or unquotedOkBit
    var i = 0

    if (inBytes[0] == '^'.code.toByte()) {
        // To work around a Bash bug, ^ is only allowed right after an opening single quote; see
        // quoting_warning.
        prevOk = singleQuotedOkBit
        i = 1
    }

    while (i < inBytes.size) {
        val c = inBytes[i]
        var curOk = prevOk

        if ((c.toInt() and 0xFF) >= 0x80) {
            // Normally, non-ASCII characters shouldn't require quoting, but see quoting_warning.md
            // about \xa0.  For now, just treat all non-ASCII characters as requiring quotes.  This
            // also ensures things are safe in the off-chance that you're in a legacy 8-bit locale
            // that has additional characters satisfying `isblank`.
            curOk = curOk and unquotedOkBit.inv()
        } else {
            if (!unquotedOkFast(c)) {
                curOk = curOk and unquotedOkBit.inv()
            }
            if (!singleQuotedOk(c)) {
                curOk = curOk and singleQuotedOkBit.inv()
            }
            if (!doubleQuotedOk(c)) {
                curOk = curOk and doubleQuotedOkBit.inv()
            }
        }

        if (curOk == 0) {
            // There are no quoting strategies that would work for both the previous characters and
            // this one.  So we have to end the chunk before this character.  The caller will call
            // [quotingStrategy] again to handle the rest of the string.
            break
        }

        prevOk = curOk
        i += 1
    }

    // Pick the best allowed strategy.
    val strategy = when {
        prevOk and unquotedOkBit != 0 -> QuotingStrategy.Unquoted
        prevOk and singleQuotedOkBit != 0 -> QuotingStrategy.SingleQuoted
        prevOk and doubleQuotedOkBit != 0 -> QuotingStrategy.DoubleQuoted
        else -> error("unreachable")
    }
    check(i > 0) // debug_assert!(i > 0)
    return i to strategy
}

internal fun appendQuotedChunk(out: MutableList<Byte>, curChunk: ByteArray, strategy: QuotingStrategy) {
    when (strategy) {
        QuotingStrategy.Unquoted -> {
            for (b in curChunk) out.add(b)
        }
        QuotingStrategy.SingleQuoted -> {
            out.add('\''.code.toByte())
            for (b in curChunk) out.add(b)
            out.add('\''.code.toByte())
        }
        QuotingStrategy.DoubleQuoted -> {
            out.add('"'.code.toByte())
            for (c in curChunk) {
                if (c == '$'.code.toByte() ||
                    c == '`'.code.toByte() ||
                    c == '"'.code.toByte() ||
                    c == '\\'.code.toByte()) {
                    // Add a preceding backslash.
                    // Note: We shouldn't actually get here for $ and ` because they don't pass
                    // [doubleQuotedOk].
                    out.add('\\'.code.toByte())
                }
                // Add the character itself.
                out.add(c)
            }
            out.add('"'.code.toByte())
        }
    }
}
