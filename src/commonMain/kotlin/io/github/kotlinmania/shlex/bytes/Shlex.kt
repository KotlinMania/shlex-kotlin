// port-lint: source src/bytes.rs
package io.github.kotlinmania.shlex.bytes

// Copyright 2015 Nicholas Allegra (comex).
// Licensed under the Apache License, Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0> or
// the MIT license <https://opensource.org/licenses/MIT>, at your option. This file may not be
// copied, modified, or distributed except according to those terms.

// [Shlex] and friends for byte strings.
//
// This is used internally by the [outer package][io.github.kotlinmania.shlex], and may be more
// convenient if you are working with byte arrays (`ByteArray`) or types that are
// wrappers around bytes.
//
// On Unix, a byte-oriented file system path can be quoted directly with [quote]; for example,
// a path that contains `0x80` (which is invalid in UTF-8) like `a\x80b c` quotes to `'a\x80b c'`.
//
// (On Windows, paths use 16 bit wide characters so this byte-oriented API will not work.)

/**
 * An iterator that takes an input byte string and splits it into the words using the same syntax as
 * the POSIX shell.
 */
class Shlex(private val inBytes: ByteArray) : Iterator<ByteArray> {
    private var pos: Int = 0

    /** The number of newlines read so far, plus one. */
    var lineNo: Int = 1

    /**
     * An input string is erroneous if it ends while inside a quotation or right after an
     * unescaped backslash.  Since Iterator does not have a mechanism to return an error, if that
     * happens, Shlex just throws out the last token, ends the iteration, and sets [hadError] to
     * true; best to check it after you're done iterating.
     */
    var hadError: Boolean = false

    private var peeked: ByteArray? = null
    private var exhausted: Boolean = false

    private fun parseWord(initial: Byte): ByteArray? {
        var ch = initial
        val result: MutableList<Byte> = mutableListOf()
        while (true) {
            when ((ch.toInt() and 0xFF).toChar()) {
                '"' -> if (!parseDouble(result)) {
                    hadError = true
                    return null
                }
                '\'' -> if (!parseSingle(result)) {
                    hadError = true
                    return null
                }
                '\\' -> {
                    val ch2 = nextChar()
                    if (ch2 != null) {
                        if (ch2 != '\n'.code.toByte()) result.add(ch2)
                    } else {
                        hadError = true
                        return null
                    }
                }
                ' ', '\t', '\n' -> return result.toByteArray()
                else -> result.add(ch)
            }
            val ch2 = nextChar() ?: return result.toByteArray()
            ch = ch2
        }
    }

    private fun parseDouble(result: MutableList<Byte>): Boolean {
        while (true) {
            val ch2 = nextChar() ?: return false
            when ((ch2.toInt() and 0xFF).toChar()) {
                '\\' -> {
                    val ch3 = nextChar() ?: return false
                    when ((ch3.toInt() and 0xFF).toChar()) {
                        // \$ => $
                        '$', '`', '"', '\\' -> result.add(ch3)
                        // \<newline> => nothing
                        '\n' -> { }
                        // \x => =x
                        else -> {
                            result.add('\\'.code.toByte())
                            result.add(ch3)
                        }
                    }
                }
                '"' -> return true
                else -> result.add(ch2)
            }
        }
    }

    private fun parseSingle(result: MutableList<Byte>): Boolean {
        while (true) {
            val ch2 = nextChar() ?: return false
            when ((ch2.toInt() and 0xFF).toChar()) {
                '\'' -> return true
                else -> result.add(ch2)
            }
        }
    }

    private fun nextChar(): Byte? {
        if (pos >= inBytes.size) return null
        val res = inBytes[pos]
        pos++
        if (res == '\n'.code.toByte()) lineNo += 1
        return res
    }

    private fun computeNext(): ByteArray? {
        var ch = nextChar() ?: return null
        // skip initial whitespace
        while (true) {
            val isWhitespace = when ((ch.toInt() and 0xFF).toChar()) {
                ' ', '\t', '\n' -> true
                '#' -> {
                    while (true) {
                        val ch2 = nextChar() ?: break
                        if ((ch2.toInt() and 0xFF).toChar() == '\n') break
                    }
                    true
                }
                else -> false
            }
            if (!isWhitespace) break
            ch = nextChar() ?: return null
        }
        return parseWord(ch)
    }

    override fun hasNext(): Boolean {
        if (peeked != null) return true
        if (exhausted) return false
        val n = computeNext()
        if (n == null) {
            exhausted = true
            return false
        }
        peeked = n
        return true
    }

    override fun next(): ByteArray {
        if (!hasNext()) throw NoSuchElementException()
        val r = peeked!!
        peeked = null
        return r
    }
}

/**
 * Convenience function that consumes the whole byte string at once.  Returns null if the input was
 * erroneous.
 */
fun split(inBytes: ByteArray): List<ByteArray>? {
    val shl = Shlex(inBytes)
    val res = shl.asSequence().toList()
    return if (shl.hadError) null else res
}
