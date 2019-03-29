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

import android.content.Context
import com.uwetrottmann.wpdisplay.settings.ConnectionSettings
import timber.log.Timber
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object ConnectionTools : ConnectionListener {

    private val executor = Executors.newScheduledThreadPool(1)
    private val disconnectRunnable: DisconnectRunnable = DisconnectRunnable(this)
    private val requestRunnable: DataRequestRunnable = DataRequestRunnable(this)

    override var socket: Socket? = null
    override var inputStream: DataInputStream? = null
    override var outputStream: DataOutputStream? = null

    private var requestSchedule: ScheduledFuture<*>? = null
    /**
     * Whether request calls are currently ignored.
     */
    override var isPaused: Boolean = false

    /** LiveData to observe for changes in connection state. */
    val connectionEvent = EventLiveData<ConnectionEvent>()

    class ConnectionEvent(
        var isConnecting: Boolean,
        var isConnected: Boolean,
        var host: String?,
        var port: Int
    )

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
    override fun setSocket(socket: Socket?, `in`: InputStream?, out: OutputStream?) {
        this.socket = socket
        this.inputStream = DataInputStream(`in`)
        this.outputStream = DataOutputStream(out)
    }

}
