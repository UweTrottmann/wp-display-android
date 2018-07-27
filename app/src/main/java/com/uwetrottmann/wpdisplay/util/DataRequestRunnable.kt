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

import com.uwetrottmann.wpdisplay.model.StatusData
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.IOException

/**
 * Requests data from the heat pump controller, waits for a response and returns it through an
 * event.
 */
class DataRequestRunnable(private val listener: ConnectionListener) : Runnable {

    class DataEvent(var data: StatusData)

    override fun run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

        val socket = listener.socket
        val input = listener.inputStream
        val output = listener.outputStream

        if (socket == null || !socket.isConnected || input == null || output == null) {
            Timber.e("run: failed, no connection")
            EventBus.getDefault().post(
                    ConnectionTools.ConnectionEvent(false, false, null, 0))
            return
        }

        if (Thread.interrupted()) {
            Timber.d("run: interrupted")
            return
        }

        Timber.d("run: requesting status data")

        try {
            // skip any remaining data
            while (input.available() > 0) {
                input.readByte()
            }

            // send request
            output.writeInt(ControllerConstants.COMMAND_REQUEST_STATUS)
            output.writeInt(0)
            output.flush()

            // wait for and process data
            // heat pump controller sends 32bit BE integers
            // first integer should be sent command code
            val responseCode = input.readInt()
            if (responseCode != ControllerConstants.COMMAND_REQUEST_STATUS) {
                // fail
                Timber.e("run: response code expected %s but was %s",
                        ControllerConstants.COMMAND_REQUEST_STATUS , responseCode)
                return
            }

            // ignored value
            input.readInt()
            // length (from server, so untrusted!)
            // cap maximum number of bytes read
            val length = Math.min(input.readInt(), StatusData.LENGTH_BYTES)

            // create array with max size
            val data = IntArray(StatusData.LENGTH_BYTES)

            // try reading sent data
            for (i in 0 until length) {
                data[i] = input.readInt()
            }

            // don't update data if we have been paused
            if (Thread.interrupted()) {
                Timber.d("run: not posting data, interrupted")
                return
            }

            EventBus.getDefault().postSticky(DataEvent(StatusData(data)))
        } catch (e: IOException) {
            Timber.e(e, "run: failed to request data")
            EventBus.getDefault().post(
                    ConnectionTools.ConnectionEvent(false, false, null, 0))
        }

    }
}
