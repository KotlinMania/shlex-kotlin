// port-lint: source src/lib.rs
package io.github.kotlinmania.shlex

// Copyright 2015 Nicholas Allegra (comex).
// Licensed under the Apache License, Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0> or
// the MIT license <https://opensource.org/licenses/MIT>, at your option. This file may not be
// copied, modified, or distributed except according to those terms.

/**
 * Errors from [Quoter.quote], [Quoter.join], etc. (and their
 * [io.github.kotlinmania.shlex.bytes] counterparts).
 *
 * By default, the only error that can be returned is [QuoteError.Nul].  If you call
 * `allowNul(true)`, then no errors can be returned at all.  Any error variants added in the
 * future will not be enabled by default; they will be enabled through corresponding non-default
 * [Quoter] options.
 *
 * ...In theory.  In the unlikely event that additional classes of inputs are discovered that,
 * like nul bytes, are fundamentally unsafe to quote even for non-interactive shells, the risk
 * will be mitigated by adding corresponding [QuoteError] variants that *are* enabled by
 * default.
 */
sealed class QuoteError(override val message: String) : Throwable(message) {
    /**
     * The input contained a nul byte.  In most cases, shells fundamentally cannot handle strings
     * containing nul bytes, no matter how they are quoted.  But if you're sure you can handle nul
     * bytes, you can call `allowNul(true)` on the [Quoter] to let them pass through.
     */
    data object Nul : QuoteError("cannot shell-quote string containing nul byte")
}
