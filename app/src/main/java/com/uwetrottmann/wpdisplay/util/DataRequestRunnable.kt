// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.util

import androidx.lifecycle.MutableLiveData
import com.uwetrottmann.wpdisplay.BuildConfig
import com.uwetrottmann.wpdisplay.model.SettingsData
import com.uwetrottmann.wpdisplay.model.StatusData
import com.uwetrottmann.wpdisplay.model.StatusData.Type
import timber.log.Timber
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Requests data from the heat pump controller, waits for a response and returns it through an
 * event.
 */
class DataRequestRunnable(private val listener: ConnectionListener) : Runnable {

    override fun run() {
        // Moves the current Thread into the background
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

        val connection = listener.connection

        if (connection == null) {
            Timber.d("run: disconnected")
            return
        }

        val socket = connection.socket
        val input = connection.inputStream
        val output = connection.outputStream

        if (!socket.isConnected) {
            Timber.d("run: no connection")
            ConnectionTools.connectionEvent.postEvent(
                ConnectionTools.ConnectionErrorEvent(
                    connection.host,
                    connection.port,
                    errorCause = null
                )
            )
            return
        }

        if (Thread.interrupted()) {
            Timber.d("run: interrupted")
            return
        }

        try {
            val previousData = statusData.value
            val settingsData = if (previousData == null || previousData.shouldRefreshSettings) {
                Timber.d("run: requesting settings data")
                requestSettings(input, output) ?: return
            } else {
                Timber.d("run: using previous settings data")
                previousData.settingsData
            }

            Timber.d("run: requesting status data")
            val statusData = requestStatusData(input, output, settingsData) ?: return

            // don't update data if we have been paused
            if (Thread.interrupted()) {
                Timber.d("run: not posting data, interrupted")
                return
            }

            Companion.statusData.postValue(statusData)
        } catch (e: IOException) {
            Timber.e(e, "run: failed to request data")
            ConnectionTools.connectionEvent.postEvent(
                ConnectionTools.ConnectionErrorEvent(
                    connection.host,
                    connection.port,
                    errorCause = e.javaClass.simpleName,
                )
            )
        }

    }

    private fun requestSettings(input: DataInputStream, output: DataOutputStream): SettingsData? {
        // skip any remaining data
        while (input.available() > 0) {
            input.readByte()
        }

        // send request
        val command = ControllerConstants.COMMAND_REQUEST_SETTINGS
        output.writeInt(command)
        output.writeInt(0)
        output.flush()

        // wait for and process data
        // heat pump controller sends 32bit BE integers
        // first integer should be sent command code
        val responseCode = input.readInt()
        if (responseCode != command) {
            // fail
            Timber.e("run: response code expected %s but was %s", command, responseCode)
            return null
        }

        // create array with max size
        val data = SettingsData()

        // length (from server, so untrusted!)
        // cap maximum number of ints read
        val lengthByServer = input.readInt()
        Timber.d("settings length=$lengthByServer")
        val length = lengthByServer.coerceAtMost(data.rawData.size)

        // try reading sent data
        for (i in 0 until length) {
            data.rawData[i] = input.readInt()
        }
        skipExcessInts(input, lengthByServer, length)

        // Set some debug data.
        if (BuildConfig.DEBUG) {
            data.rawData[SettingsData.TypeWithOffset.BooleanType.PhotovoltaicsActive.offset] = 1
        }

        return data
    }

    private fun requestStatusData(
        input: DataInputStream,
        output: DataOutputStream,
        settingsData: SettingsData
    ) : StatusData? {
        // skip any remaining data
        while (input.available() > 0) {
            input.readByte()
        }

        // send request
        val command = ControllerConstants.COMMAND_REQUEST_STATUS
        output.writeInt(command)
        output.writeInt(0)
        output.flush()

        // wait for and process data
        // heat pump controller sends 32bit BE integers
        // first integer should be sent command code
        val responseCode = input.readInt()
        if (responseCode != command) {
            // fail
            Timber.e("run: response code expected %s but was %s", command, responseCode)
            return null
        }

        // Status: If bigger 0, indicates that settings have changed.
        val status = input.readInt()
        Timber.d("status=$status")

        // length (from server, so untrusted!)
        // cap maximum number of ints read
        val lengthByServer = input.readInt()
        Timber.d("status data length=$lengthByServer")
        val length = lengthByServer.coerceAtMost(StatusData.MAX_VALUES)

        // create array with max size
        val data = IntArray(StatusData.MAX_VALUES)

        // try reading sent data
        for (i in 0 until length) {
            data[i] = input.readInt()
        }
        skipExcessInts(input, lengthByServer, length)

        // Set some debug data.
        if (BuildConfig.DEBUG) {
            data[Type.TypeWithOffset.HeatQuantity.HeatQuantityHeating.offset] = 101
            data[Type.TypeWithOffset.HeatQuantity.HeatQuantityWater.offset] = 202
            data[Type.TypeWithOffset.HeatQuantity.HeatQuantitySwimmingPool.offset] = 303
            data[Type.TypeWithOffset.HeatQuantity.HeatQuantitySince.offset] = 404
        }

        return StatusData(data, status > 0, settingsData)
    }

    /**
     * If the server sent more ints than were read, reads and discards the rest. Otherwise, if not
     * all of them have arrived yet, skipping using [DataInputStream.available] before the next
     * request misses them and the next response would be read starting at the wrong position.
     */
    private fun skipExcessInts(input: DataInputStream, lengthByServer: Int, lengthRead: Int) {
        val excess = lengthByServer - lengthRead
        if (excess > 0) {
            Timber.d("skipping $excess ints")
            repeat(excess) { input.readInt() }
        }
    }

    companion object {
        val statusData = MutableLiveData<StatusData>().apply {
            postValue(StatusData())
        }
    }
}
