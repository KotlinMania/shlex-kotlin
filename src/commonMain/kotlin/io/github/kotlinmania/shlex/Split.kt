// port-lint: source src/lib.rs
package io.github.kotlinmania.shlex

// Copyright 2015 Nicholas Allegra (comex).
// Licensed under the Apache License, Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0> or
// the MIT license <https://opensource.org/licenses/MIT>, at your option. This file may not be
// copied, modified, or distributed except according to those terms.

/**
 * Convenience function that consumes the whole string at once.  Returns null if the input was
 * erroneous.
 */
fun split(inStr: String): List<String>? {
    val shl = Shlex(inStr)
    val res = shl.asSequence().toList()
    return if (shl.hadError) null else res
}
