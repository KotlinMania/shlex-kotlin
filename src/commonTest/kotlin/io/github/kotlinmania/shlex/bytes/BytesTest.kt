// port-lint: source bytes.rs
package io.github.kotlinmania.shlex.bytes

import io.github.kotlinmania.shlex.QuoteError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

// Upstream defines this constant as a single 0xA1 byte (intentionally invalid UTF-8).
private val INVALID_UTF8: ByteArray = byteArrayOf(0xa1.toByte())

// Upstream defines this constant as the same byte wrapped in single quotes.
private val INVALID_UTF8_SINGLEQUOTED: ByteArray = byteArrayOf(
    '\''.code.toByte(),
    0xa1.toByte(),
    '\''.code.toByte(),
)

// Helper: build a ByteArray from a String (the equivalent of an upstream byte-string literal).
private fun b(s: String): ByteArray = s.encodeToByteArray()

// Helpers: upstream byte-slice equality is elementwise; Kotlin ByteArray equality is reference-based,
// so wrap the comparison via list conversion.
private fun assertBytesEquals(expected: ByteArray, actual: ByteArray) {
    assertEquals(expected.toList(), actual.toList())
}

private fun assertBytesEquals(expected: List<ByteArray>, actual: List<ByteArray>?) {
    if (actual == null) error("expected list of byte arrays, got null")
    assertEquals(expected.size, actual.size)
    for ((i, p) in expected.withIndex()) {
        assertEquals(p.toList(), actual[i].toList())
    }
}

// `static SPLIT_TEST_ITEMS` from upstream — pairs of (input bytes, expected output).
// `null` corresponds to upstream's `None` (an erroneous input that `split` rejects).
private val SPLIT_TEST_ITEMS: Array<Pair<ByteArray, List<ByteArray>?>> = arrayOf(
    b("foo\$baz") to listOf(b("foo\$baz")),
    b("foo baz") to listOf(b("foo"), b("baz")),
    b("foo\"bar\"baz") to listOf(b("foobarbaz")),
    b("foo \"bar\"baz") to listOf(b("foo"), b("barbaz")),
    b("   foo \nbar") to listOf(b("foo"), b("bar")),
    b("foo\\\nbar") to listOf(b("foobar")),
    b("\"foo\\\nbar\"") to listOf(b("foobar")),
    b("'baz\\\$b'") to listOf(b("baz\\\$b")),
    b("'baz\\''") to null,
    b("\\") to null,
    b("\"\\") to null,
    b("'\\") to null,
    b("\"") to null,
    b("'") to null,
    b("foo #bar\nbaz") to listOf(b("foo"), b("baz")),
    b("foo #bar") to listOf(b("foo")),
    b("foo#bar") to listOf(b("foo#bar")),
    b("foo\"#bar") to null,
    b("'\\n'") to listOf(b("\\n")),
    b("'\\\\n'") to listOf(b("\\\\n")),
    INVALID_UTF8 to listOf(INVALID_UTF8),
)

class BytesTest {
    // Confirms the test fixture is rejected by a strict UTF-8 decoder.
    @Test
    fun testInvalidUtf8() {
        // Check that our test string is actually invalid UTF-8.
        assertFails {
            INVALID_UTF8.decodeToString(throwOnInvalidSequence = true)
        }
    }

    @Test
    fun testSplit() {
        for ((input, output) in SPLIT_TEST_ITEMS) {
            val actual = split(input)
            if (output == null) {
                assertNull(actual)
            } else {
                assertBytesEquals(output, actual)
            }
        }
    }

    @Test
    fun testLineno() {
        val sh = Shlex(b("\nfoo\nbar"))
        while (sh.hasNext()) {
            val word = sh.next()
            if (word.toList() == b("bar").toList()) {
                assertEquals(3, sh.lineNo)
            }
        }
    }

    // Suppression mirrors the upstream allow-deprecated attribute on this test.
    @Suppress("DEPRECATION")
    @Test
    fun testQuote() {
        // Validate behavior with invalid UTF-8:
        assertBytesEquals(INVALID_UTF8_SINGLEQUOTED, quote(INVALID_UTF8))
        // Replicate a few tests from lib.rs.  No need to replicate all of them.
        assertBytesEquals(b("''"), quote(b("")))
        assertBytesEquals(b("foobar"), quote(b("foobar")))
        assertBytesEquals(b("'foo bar'"), quote(b("foo bar")))
        assertBytesEquals(b("\"'\\\"\""), quote(b("'\"")))
        assertBytesEquals(b("''"), quote(b("")))
    }

    // Suppression mirrors the upstream allow-deprecated attribute on this test.
    @Suppress("DEPRECATION")
    @Test
    fun testJoin() {
        // Validate behavior with invalid UTF-8:
        assertBytesEquals(INVALID_UTF8_SINGLEQUOTED, join(listOf(INVALID_UTF8)))
        // Replicate a few tests from lib.rs.  No need to replicate all of them.
        assertBytesEquals(b(""), join(emptyList()))
        assertBytesEquals(b("''"), join(listOf(b(""))))
    }
}
