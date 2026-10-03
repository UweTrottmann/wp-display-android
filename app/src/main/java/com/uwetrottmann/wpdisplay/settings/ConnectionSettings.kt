// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.settings

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

/**
 * Settings related to the controller connection.
 */
object ConnectionSettings {

    private const val KEY_HOST = "host"
    private const val KEY_PORT = "port"

    /**
     * 8889 seems to be used on newer firmwares and returns more data, so default to it.
     */
    private const val DEFAULT_PORT = 8889

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
            value = DEFAULT_PORT
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit {
                    putInt(KEY_PORT, value)
                }
        }
        return value
    }

    /**
     * Saves host and port.
     *
     * If the host is blank, the host is removed.
     *
     * If the port is invalid, the port is removed (so [getPort] will return the default port).
     */
    fun saveConnectionSettings(context: Context, host: String, port: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            if (host.isNotBlank()) {
                putString(KEY_HOST, host)
            } else {
                remove(KEY_HOST)
            }
            if (port in 1..65535) {
                putInt(KEY_PORT, port)
            } else {
                remove(KEY_PORT)
            }
        }
    }
}
