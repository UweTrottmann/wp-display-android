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
import java.net.InetSocketAddress
import java.net.Socket

internal class ConnectRunnable(
        private val listener: ConnectionListener,
        private val host: String?,
        private val port: Int
) : Runnable {

    override fun run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

        val existingSocket = listener.socket
        if (existingSocket != null && existingSocket.isConnected) {
            Timber.d("run: already connected")
            return
        }

        if (Thread.interrupted()) {
            return
        }

        Timber.d("run: connecting")
        ConnectionTools.connectionEvent.postEvent(
                ConnectionTools.ConnectionEvent(true, false, host, port))

        var socket : Socket? = null
        try {
            // connect, create in and out streams
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), 15 * 1000) // 5 sec
            socket.soTimeout = 20 * 1000 // 15 sec
            listener.setSocket(socket, socket.getInputStream(), socket.getOutputStream())

            // post success
            ConnectionTools.connectionEvent.postEvent(
                    ConnectionTools.ConnectionEvent(false, true, host, port))
        } catch (e: IOException) {
            Timber.e(e, "run: connection to $host:$port failed")
            try {
                socket?.close()
            } catch (ignored: IOException) {
            }

            // post failure
            ConnectionTools.connectionEvent.postEvent(
                    ConnectionTools.ConnectionEvent(false, false, host, port))
        }

    }
}
