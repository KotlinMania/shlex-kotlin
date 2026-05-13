// port-lint: source src/lib.rs
package io.github.kotlinmania.shlex

// Copyright 2015 Nicholas Allegra (comex).
// Licensed under the Apache License, Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0> or
// the MIT license <https://opensource.org/licenses/MIT>, at your option. This file may not be
// copied, modified, or distributed except according to those terms.

//! Parse strings like, and escape strings for, POSIX shells.
//!
//! Same idea as (but implementation not directly based on) the Python shlex module.
//!
//! ## Warning
//!
//! The [tryQuote] / [tryJoin] family of APIs does not quote control characters (because they
//! cannot be quoted portably).
//!
//! This is fully safe in noninteractive contexts, like shell scripts and `sh -c` arguments (or
//! even scripts `source`d from interactive shells).
//!
//! But if you are quoting for human consumption, you should keep in mind that ugly inputs produce
//! ugly outputs (which may not be copy-pastable).
//!
//! And if by chance you are piping the output of [tryQuote] / [tryJoin] directly to the stdin
//! of an interactive shell, you should stop, because control characters can lead to arbitrary
//! command injection.
//!
//! For more information, and for information about more minor issues, please see
//! `tmp/shlex/src/quoting_warning.md` in the upstream tree.
//!
//! ## Compatibility
//!
//! This package's quoting functionality tries to be compatible with **any POSIX-compatible shell**;
//! it's tested against `bash`, `zsh`, `dash`, Busybox `ash`, and `mksh`, plus `fish` (which is not
//! POSIX-compatible but close enough).
//!
//! It also aims to be compatible with Python `shlex` and C `wordexp`.

import io.github.kotlinmania.shlex.bytes.Shlex as BytesShlex

/**
 * An iterator that takes an input string and splits it into the words using the same syntax as
 * the POSIX shell.
 *
 * See [io.github.kotlinmania.shlex.bytes.Shlex].
 */
class Shlex(inStr: String) : Iterator<String> {
    // Upstream wraps `bytes::Shlex` and uses Deref/DerefMut to expose `lineNo` / `hadError`.  In
    // Kotlin we surface those as forwarding properties instead.
    private val inner: BytesShlex = BytesShlex(inStr.encodeToByteArray())

    val lineNo: Int get() = inner.lineNo
    val hadError: Boolean get() = inner.hadError

    override fun hasNext(): Boolean = inner.hasNext()

    override fun next(): String {
        val byteWord = inner.next()
        // Safety: given valid UTF-8, bytes::Shlex will always return valid UTF-8.
        return byteWord.decodeToString()
    }
}
