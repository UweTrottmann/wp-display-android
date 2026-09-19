// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

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
            ConnectionTools.ConnectionEvent(
                isConnecting = true,
                isConnected = false,
                host = host,
                port = port
            )
        )

        var socket: Socket? = null
        try {
            // connect, create in and out streams
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), 15 * 1000) // 15 sec
            socket.soTimeout = 20 * 1000 // 20 sec
            listener.setSocket(socket, socket.getInputStream(), socket.getOutputStream())

            // post success
            ConnectionTools.connectionEvent.postEvent(
                ConnectionTools.ConnectionEvent(
                    isConnecting = false,
                    isConnected = true,
                    host = host,
                    port = port
                )
            )
        } catch (e: IOException) {
            Timber.e(e, "run: connection to $host:$port failed")
            try {
                socket?.close()
            } catch (ignored: IOException) {
            }

            // post failure
            ConnectionTools.connectionEvent.postEvent(
                ConnectionTools.ConnectionEvent(
                    isConnecting = false,
                    isConnected = false,
                    host = host,
                    port = port
                )
            )
        }

    }
}
