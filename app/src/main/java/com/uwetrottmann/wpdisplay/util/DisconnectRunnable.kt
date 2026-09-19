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

        try {
            listener.inputStream?.close()
            listener.outputStream?.close()
            listener.socket?.close()
        } catch (e: IOException) {
            Timber.e(e, "run: disconnecting failed")
        }

        listener.setSocket(null, null, null)
    }
}
