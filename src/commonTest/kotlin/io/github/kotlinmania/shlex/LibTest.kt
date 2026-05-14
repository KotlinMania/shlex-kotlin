// port-lint: source lib.rs
package io.github.kotlinmania.shlex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// `static SPLIT_TEST_ITEMS` from upstream — pairs of (input, expected output).
// `null` corresponds to upstream's `None` (an erroneous input that `split` rejects).
private val SPLIT_TEST_ITEMS: Array<Pair<String, List<String>?>> = arrayOf(
    "foo\$baz" to listOf("foo\$baz"),
    "foo baz" to listOf("foo", "baz"),
    "foo\"bar\"baz" to listOf("foobarbaz"),
    "foo \"bar\"baz" to listOf("foo", "barbaz"),
    "   foo \nbar" to listOf("foo", "bar"),
    "foo\\\nbar" to listOf("foobar"),
    "\"foo\\\nbar\"" to listOf("foobar"),
    "'baz\\\$b'" to listOf("baz\\\$b"),
    "'baz\\''" to null,
    "\\" to null,
    "\"\\" to null,
    "'\\" to null,
    "\"" to null,
    "'" to null,
    "foo #bar\nbaz" to listOf("foo", "baz"),
    "foo #bar" to listOf("foo"),
    "foo#bar" to listOf("foo#bar"),
    "foo\"#bar" to null,
    "'\\n'" to listOf("\\n"),
    "'\\\\n'" to listOf("\\\\n"),
)

class LibTest {
    @Test
    fun testSplit() {
        for ((input, output) in SPLIT_TEST_ITEMS) {
            assertEquals(output, split(input))
        }
    }

    @Test
    fun testLineno() {
        val sh = Shlex("\nfoo\nbar")
        while (sh.hasNext()) {
            val word = sh.next()
            if (word == "bar") {
                assertEquals(3, sh.lineNo)
            }
        }
    }

    @Test
    fun testQuote() {
        // This is a list of (unquoted, quoted) pairs.
        // It uses a single long string literal with an ad-hoc format, just because it is hard
        // to read if we have to put the test strings through Kotlin escaping on top of the
        // escaping being tested. Ad-hoc: "NL" is replaced with a literal newline; no other
        // escape sequences.
        val tests = """
            <>                => <''>
            <foobar>          => <foobar>
            <foo bar>         => <'foo bar'>
            <"foo bar'">      => <"\"foo bar'\"">
            <'foo bar'>       => <"'foo bar'">
            <">               => <'"'>
            <"'>              => <"\"'">
            <hello!world>     => <'hello!world'>
            <'hello!world>    => <"'hello"'!world'>
            <'hello!>         => <"'hello"'!'>
            <hello ^ world>   => <'hello ''^ world'>
            <hello^>          => <hello'^'>
            <!world'>         => <'!world'"'">
            <{a, b}>          => <'{a, b}'>
            <NL>              => <'NL'>
            <^>               => <'^'>
            <foo^bar>         => <foo'^bar'>
            <NLx^>            => <'NLx''^'>
            <NL^x>            => <'NL''^x'>
            <NL ^x>           => <'NL ''^x'>
            <{a,b}>           => <'{a,b}'>
            <a,b>             => <'a,b'>
            <a..b             => <a..b>
            <'$>              => <"'"'$'>
            <"^>              => <'"''^'>
        """.trimIndent()
        var ok = true
        for (test in tests.trim().split('\n')) {
            val parts = test
                .replace("NL", "\n")
                .split("=>")
                .map { it.trim().trimStart('<').trimEnd('>') }
            check(parts.size == 2)
            val unquoted = parts[0]
            val quotedExpected = parts[1]
            val quotedActual = tryQuote(unquoted).getOrThrow()
            if (quotedExpected != quotedActual) {
                println("FAIL: for input <$unquoted>, expected <$quotedExpected>, got <$quotedActual>")
                ok = false
            }
        }
        assertTrue(ok)
    }

    // Suppression mirrors the upstream allow-deprecated attribute on this test.
    @Suppress("DEPRECATION")
    @Test
    fun testJoin() {
        assertEquals("", join(listOf()))
        assertEquals("''", join(listOf("")))
        assertEquals("a b", join(listOf("a", "b")))
        assertEquals("'foo bar' baz", join(listOf("foo bar", "baz")))
    }

    @Test
    fun testFallible() {
        assertEquals(QuoteError.Nul, tryJoin(listOf("\u0000")).exceptionOrNull())
        assertEquals(QuoteError.Nul, tryQuote("\u0000").exceptionOrNull())
    }
}
