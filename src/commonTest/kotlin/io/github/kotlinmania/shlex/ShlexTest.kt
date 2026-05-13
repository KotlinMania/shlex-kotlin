// port-lint: source src/lib.rs
package io.github.kotlinmania.shlex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val SPLIT_TEST_ITEMS: List<Pair<String, List<String>?>> = listOf(
    "foo\$baz" to listOf("foo\$baz"),
    "foo baz" to listOf("foo", "baz"),
    "foo\"bar\"baz" to listOf("foobarbaz"),
    "foo \"bar\"baz" to listOf("foo", "barbaz"),
    "   foo \nbar" to listOf("foo", "bar"),
    "foo\\\nbar" to listOf("foobar"),
    "\"foo\\\nbar\"" to listOf("foobar"),
    "'baz\\\$b'" to listOf("baz\\\$b"),
    "'baz\\\''" to null,
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

class ShlexTest {
    @Test
    fun testSplit() {
        for ((input, output) in SPLIT_TEST_ITEMS) {
            assertEquals(output, split(input), "input=<$input>")
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
        // But it's using a single long (raw) string literal with an ad-hoc format, just because
        // it's hard to read if we have to put the test strings through escaping on top of the
        // escaping being tested.
        // Ad-hoc: "NL" is replaced with a literal newline; no other escape sequences.
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
        <'${'$'}>              => <"'"'${'$'}'>
        <"^>              => <'"''^'>
        """.trimIndent()
        var ok = true
        for (test in tests.trim().split('\n')) {
            val parts: List<String> = test
                .replace("NL", "\n")
                .split("=>")
                .map { part -> part.trim().trimStart('<').trimEnd('>') }
            assertTrue(parts.size == 2)
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

    @Test
    fun testJoin() {
        // Mirrors upstream test_join, rewritten to use tryJoin so that the deprecated
        // convenience function `join` is not invoked in tests (which would trip
        // -Werror=deprecation).
        assertEquals("", tryJoin(emptyList()).getOrThrow())
        assertEquals("''", tryJoin(listOf("")).getOrThrow())
        assertEquals("a b", tryJoin(listOf("a", "b")).getOrThrow())
        assertEquals("'foo bar' baz", tryJoin(listOf("foo bar", "baz")).getOrThrow())
    }

    @Test
    fun testFallible() {
        assertEquals(QuoteError.Nul, tryJoin(listOf("\u0000")).exceptionOrNull())
        assertEquals(QuoteError.Nul, tryQuote("\u0000").exceptionOrNull())
    }
}
