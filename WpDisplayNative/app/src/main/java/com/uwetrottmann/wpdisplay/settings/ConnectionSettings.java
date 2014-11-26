package com.uwetrottmann.wpdisplay.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Settings related to the controller connection.
 */
public class ConnectionSettings {

    public static final String KEY_HOST = "host";
    public static final String KEY_PORT = "port";

    /**
     * Return the user set host or null.
     */
    public static String getHost(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_HOST, null);
    }

    /**
     * Return the user set port or a default port.
     */
    public static int getPort(Context context) {
        int value = PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_PORT, -1);
        if (value == -1) {
            // default port
            value = 8888;
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putInt(KEY_PORT, value)
                    .apply();
        }
        return value;
    }

    /**
     * Save host and port.
     *
     * @param port Needs to be a valid port.
     */
    public static void saveConnectionSettings(Context context, String host, int port) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
                .edit();
        editor.putString(KEY_HOST, host);
        if (port > 0 || port <= 65535) {
            editor.putInt(KEY_PORT, port);
        }
        editor.apply();
    }
}
