// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.util

import android.content.Context
import com.uwetrottmann.wpdisplay.settings.ConnectionSettings
import timber.log.Timber
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object ConnectionTools : ConnectionListener {

    private val executor = Executors.newScheduledThreadPool(1)
    private val disconnectRunnable: DisconnectRunnable = DisconnectRunnable(this)
    private val requestRunnable: DataRequestRunnable = DataRequestRunnable(this)

    override var connection: Connection? = null

    private var requestSchedule: ScheduledFuture<*>? = null

    /**
     * Whether request calls are currently ignored.
     */
    override var isPaused: Boolean = false

    /** LiveData to observe for changes in connection state. */
    val connectionEvent = EventLiveData<ConnectionEvent>()

    sealed interface ConnectionEvent
    object MissingSettingsEvent : ConnectionEvent
    data class ConnectingEvent(val host: String, val port: Int) : ConnectionEvent
    data class ConnectedEvent(val host: String, val port: Int) : ConnectionEvent
    data class ConnectionErrorEvent(val host: String, val port: Int, val errorCause: String?) :
        ConnectionEvent

    /**
     * Try to establish a connection, async.
     */
    @Synchronized
    fun connect(context: Context) {
        Timber.d("connect: scheduling")
        executor.execute(
            ConnectRunnable(
                this, ConnectionSettings.getHost(context),
                ConnectionSettings.getPort(context)
            )
        )
    }

    /**
     * Disconnect (if connected) and stop status data requests.
     */
    @Synchronized
    fun disconnect() {
        Timber.d("disconnect: scheduling")
        cancelStatusDataRequests()
        executor.execute(disconnectRunnable)
    }

    /**
     * If enabled, will request new status data immediately, then every 2 seconds.
     *
     *
     *  **Note:** If already enabled/disabled or paused, will do nothing.
     */
    @Synchronized
    fun requestStatusData(enable: Boolean) {
        if (isPaused) {
            // do nothing, paused
            return
        }

        if (enable) {
            scheduleStatusDataRequests()
        } else {
            cancelStatusDataRequests()
        }
    }

    /**
     * Stop requesting status data.
     */
    @Synchronized
    fun pause() {
        isPaused = true
        cancelStatusDataRequests()
    }

    /**
     * Resume requesting status data.
     */
    @Synchronized
    fun resume() {
        isPaused = false
        scheduleStatusDataRequests()
    }

    private fun scheduleStatusDataRequests() {
        if (requestSchedule != null) {
            // already running
            return
        }
        Timber.d("scheduleStatusDataRequests: scheduling")
        requestSchedule = executor.scheduleWithFixedDelay(
            requestRunnable, 0, 2,
            TimeUnit.SECONDS
        )
    }

    private fun cancelStatusDataRequests() {
        requestSchedule?.cancel(true)
        requestSchedule = null
    }

    @Synchronized
    override fun setConnection(
        host: String,
        port: Int,
        socket: Socket
    ) {
        connection = Connection(
            host = host,
            port = port,
            socket = socket,
            inputStream = DataInputStream(socket.getInputStream()),
            outputStream = DataOutputStream(socket.getOutputStream())
        )
    }

    @Synchronized
    override fun clearConnection() {
        connection = null
    }

}
