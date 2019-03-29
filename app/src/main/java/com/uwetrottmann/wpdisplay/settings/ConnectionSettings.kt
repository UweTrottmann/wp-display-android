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

package com.uwetrottmann.wpdisplay.settings

import android.content.Context
import android.preference.PreferenceManager

/**
 * Settings related to the controller connection.
 */
object ConnectionSettings {

    private val KEY_HOST = "host"
    private val KEY_PORT = "port"

    /**
     * Return the user set host or null.
     */
    fun getHost(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_HOST, null)
    }

    /**
     * Return the user set port or a default port.
     */
    fun getPort(context: Context): Int {
        var value = PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_PORT, -1)
        if (value == -1) {
            // default port
            value = 8888
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(KEY_PORT, value)
                .apply()
        }
        return value
    }

    /**
     * Save host and port.
     *
     * @param port Needs to be a valid port.
     */
    fun saveConnectionSettings(context: Context, host: String, port: Int) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putString(KEY_HOST, host)
        if (port > 0 || port <= 65535) {
            editor.putInt(KEY_PORT, port)
        }
        editor.apply()
    }
}
