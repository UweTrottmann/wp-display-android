// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.settings

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Settings related to the controller connection.
 */
object ConnectionSettings {

    private const val KEY_HOST = "host"
    private const val KEY_PORT = "port"

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
            // 8889 seems to be used on newer firmwares and returns more data, so default to it.
            value = 8889
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
