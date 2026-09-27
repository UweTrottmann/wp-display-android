// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.util

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

/**
 * Interfaces for [com.uwetrottmann.wpdisplay.util.ConnectionTools] runnables.
 */
interface ConnectionListener {
    val connection: Connection?

    val isPaused: Boolean

    fun setConnection(host: String, port: Int, socket: Socket)
    fun clearConnection()
}

data class Connection(
    val host: String,
    val port: Int,
    val socket: Socket,
    val inputStream: DataInputStream,
    val outputStream: DataOutputStream
)
