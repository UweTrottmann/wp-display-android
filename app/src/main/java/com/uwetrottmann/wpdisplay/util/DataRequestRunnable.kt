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

import SettingsData
import androidx.lifecycle.MutableLiveData
import com.uwetrottmann.wpdisplay.BuildConfig
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

        val socket = listener.socket
        val input = listener.inputStream
        val output = listener.outputStream

        if (socket == null || !socket.isConnected || input == null || output == null) {
            Timber.e("run: failed, no connection")
            ConnectionTools.connectionEvent.postEvent(
                ConnectionTools.ConnectionEvent(
                    isConnecting = false,
                    isConnected = false,
                    host = null,
                    port = 0
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
                ConnectionTools.ConnectionEvent(
                    isConnecting = false,
                    isConnected = false,
                    host = null,
                    port = 0
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
        // cap maximum number of bytes read
        val lengthByServer = input.readInt()
        Timber.d("settings length=$lengthByServer")
        val length = lengthByServer.coerceAtMost(data.rawData.size)

        // try reading sent data
        for (i in 0 until length) {
            data.rawData[i] = input.readInt()
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
        // cap maximum number of bytes read
        val lengthByServer = input.readInt()
        Timber.d("status data length=$lengthByServer")
        val length = lengthByServer.coerceAtMost(StatusData.LENGTH_BYTES)

        // create array with max size
        val data = IntArray(StatusData.LENGTH_BYTES)

        // try reading sent data
        for (i in 0 until length) {
            data[i] = input.readInt()
        }

        // Set some debug data.
        if (BuildConfig.DEBUG) {
            data[Type.TypeWithOffset.HeatQuantity.HeatQuantityHeating.offset] = 101
            data[Type.TypeWithOffset.HeatQuantity.HeatQuantityWater.offset] = 202
            data[Type.TypeWithOffset.HeatQuantity.HeatQuantitySwimmingPool.offset] = 303
            data[Type.TypeWithOffset.HeatQuantity.HeatQuantitySince.offset] = 404
        }

        return StatusData(data, status > 0, settingsData)
    }

    companion object {
        val statusData = MutableLiveData<StatusData>().apply {
            postValue(StatusData())
        }
    }
}
