// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.util

import timber.log.Timber
import java.io.IOException

class DisconnectRunnable(private val listener: ConnectionListener) : Runnable {

    override fun run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

        Timber.d("run: disconnecting")

        val connection = listener.connection
        if (connection == null) {
            Timber.d("run: not connected")
            return
        }

        try {
            // Closing the socket also closes its input and output streams
            connection.socket.close()
        } catch (e: IOException) {
            Timber.e(e, "run: disconnecting failed")
        }

        listener.clearConnection()
    }
}
