// port-lint: source bytes.rs
package io.github.kotlinmania.shlex.bytes

import io.github.kotlinmania.shlex.QuoteError
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// `\xa1` is invalid in UTF-8.
private val INVALID_UTF8: ByteArray = byteArrayOf(0xa1.toByte())
private val INVALID_UTF8_SINGLEQUOTED: ByteArray =
    byteArrayOf('\''.code.toByte(), 0xa1.toByte(), '\''.code.toByte())

private fun bs(s: String): ByteArray = s.encodeToByteArray()

private val SPLIT_TEST_ITEMS: List<Pair<ByteArray, List<ByteArray>?>> =
    listOf(
        bs("foo\$baz") to listOf(bs("foo\$baz")),
        bs("foo baz") to listOf(bs("foo"), bs("baz")),
        bs("foo\"bar\"baz") to listOf(bs("foobarbaz")),
        bs("foo \"bar\"baz") to listOf(bs("foo"), bs("barbaz")),
        bs("   foo \nbar") to listOf(bs("foo"), bs("bar")),
        bs("foo\\\nbar") to listOf(bs("foobar")),
        bs("\"foo\\\nbar\"") to listOf(bs("foobar")),
        bs("'baz\\\$b'") to listOf(bs("baz\\\$b")),
        bs("'baz\\\''") to null,
        bs("\\") to null,
        bs("\"\\") to null,
        bs("'\\") to null,
        bs("\"") to null,
        bs("'") to null,
        bs("foo #bar\nbaz") to listOf(bs("foo"), bs("baz")),
        bs("foo #bar") to listOf(bs("foo")),
        bs("foo#bar") to listOf(bs("foo#bar")),
        bs("foo\"#bar") to null,
        bs("'\\n'") to listOf(bs("\\n")),
        bs("'\\\\n'") to listOf(bs("\\\\n")),
        INVALID_UTF8 to listOf(INVALID_UTF8),
    )

class BytesTest {
    @Test
    fun testInvalidUtf8() {
        // Check that our test bytes are actually invalid UTF-8.  Kotlin's decodeToString defaults
        // to throwOnInvalidSequence=false, but we ask it to throw to assert invalidity.
        var threw = false
        try {
            INVALID_UTF8.decodeToString(throwOnInvalidSequence = true)
        } catch (e: CharacterCodingException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun testSplit() {
        for ((input, output) in SPLIT_TEST_ITEMS) {
            val result = split(input)
            if (output == null) {
                assertNull(result, "input=<${input.toList()}>")
            } else {
                assertNotNull(result, "input=<${input.toList()}>")
                assertEquals(output.size, result.size)
                for (i in output.indices) {
                    assertContentEquals(output[i], result[i])
                }
            }
        }
    }

    @Test
    fun testLineno() {
        val sh = Shlex("\nfoo\nbar".encodeToByteArray())
        while (sh.hasNext()) {
            val word = sh.next()
            if (word.contentEquals("bar".encodeToByteArray())) {
                assertEquals(3, sh.lineNo)
            }
        }
    }

    @Test
    fun testQuote() {
        // Validate behavior with invalid UTF-8:
        assertContentEquals(
            INVALID_UTF8_SINGLEQUOTED,
            Quoter().allowNul(true).quote(INVALID_UTF8).getOrThrow(),
        )
        // Replicate a few tests from lib.rs.  No need to replicate all of them.
        // Upstream uses the deprecated `quote` here; we use the Quoter API to avoid the
        // -Werror=deprecation trip.
        val q = Quoter().allowNul(true)
        assertContentEquals(bs("''"), q.quote(bs("")).getOrThrow())
        assertContentEquals(bs("foobar"), q.quote(bs("foobar")).getOrThrow())
        assertContentEquals(bs("'foo bar'"), q.quote(bs("foo bar")).getOrThrow())
        assertContentEquals(bs("\"'\\\"\""), q.quote(bs("'\"")).getOrThrow())
        assertContentEquals(bs("''"), q.quote(bs("")).getOrThrow())
    }

    @Test
    fun testJoin() {
        // Mirrors the upstream join test, rewritten to use the Quoter API directly.
        val q = Quoter().allowNul(true)
        // Validate behavior with invalid UTF-8:
        assertContentEquals(INVALID_UTF8_SINGLEQUOTED, q.join(listOf(INVALID_UTF8)).getOrThrow())
        assertContentEquals(bs(""), q.join(emptyList()).getOrThrow())
        assertContentEquals(bs("''"), q.join(listOf(bs(""))).getOrThrow())
    }

    @Test
    fun testFallible() {
        assertEquals(QuoteError.Nul, tryJoin(listOf(byteArrayOf(0))).exceptionOrNull())
        assertEquals(QuoteError.Nul, tryQuote(byteArrayOf(0)).exceptionOrNull())
    }

    @Test
    fun testAllowNul() {
        val q = Quoter().allowNul(true)
        val quotedNul = byteArrayOf('\''.code.toByte(), 0, '\''.code.toByte())
        assertContentEquals(quotedNul, q.quote(byteArrayOf(0)).getOrThrow())
        assertContentEquals(quotedNul, q.join(listOf(byteArrayOf(0))).getOrThrow())
    }
}
