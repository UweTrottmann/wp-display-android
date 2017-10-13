/*
 * Copyright 2015 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
