// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.util

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * Interfaces for [com.uwetrottmann.wpdisplay.util.ConnectionTools] runnables.
 */
interface ConnectionListener {
    val socket: Socket?

    val inputStream: DataInputStream?

    val outputStream: DataOutputStream?

    val isPaused: Boolean

    fun setSocket(socket: Socket?, `in`: InputStream?, out: OutputStream?)
}
