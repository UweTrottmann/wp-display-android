/*
 * Copyright 2018 Uwe Trottmann
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
import android.os.Build
import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.util.*

object ThemeSettings {

    private const val KEY_THEME_MODE = "theme"
    private const val KEY_NIGHT_START_HOUR = "night.start.hour"
    private const val KEY_NIGHT_START_MINUTE = "night.start.minute"
    private const val KEY_NIGHT_END_HOUR = "night.end.hour"
    private const val KEY_NIGHT_END_MINUTE = "night.end.minute"

    const val THEME_ALWAYS_DAY = 0
    const val THEME_ALWAYS_NIGHT = 1
    const val THEME_DAY_NIGHT = 2
    const val THEME_DAY_NIGHT_SYSTEM = 3

    private const val NIGHT_START_HOUR_DEFAULT = 21
    private const val NIGHT_START_MINUTE_DEFAULT = 0
    private const val NIGHT_END_HOUR_DEFAULT = 7
    private const val NIGHT_END_MINUTE_DEFAULT = 0

    fun getThemeMode(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_THEME_MODE, THEME_DAY_NIGHT_SYSTEM)
    }

    fun getNightMode(context: Context): Int {
        return when (getThemeMode(context)) {
            THEME_ALWAYS_DAY -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_ALWAYS_NIGHT -> AppCompatDelegate.MODE_NIGHT_YES
            THEME_DAY_NIGHT -> {
                val nightStartHour = getNightStartHour(context)
                val nightStartMinute = getNightStartMinute(context)
                val nightEndHour = getNightEndHour(context)
                val nightEndMinute = getNightEndMinute(context)

                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)

                if (currentHour > nightStartHour || currentHour < nightEndHour) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else if (currentHour == nightStartHour && currentMinute >= nightStartMinute) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else if (currentHour == nightEndHour && currentMinute < nightEndMinute) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            }
            else -> {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                } else {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }
        }
    }

    fun getNightStartHour(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(KEY_NIGHT_START_HOUR, NIGHT_START_HOUR_DEFAULT)
    }

    fun getNightStartMinute(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(KEY_NIGHT_START_MINUTE, NIGHT_START_MINUTE_DEFAULT)
    }

    fun getNightEndHour(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(KEY_NIGHT_END_HOUR, NIGHT_END_HOUR_DEFAULT)
    }

    fun getNightEndMinute(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(KEY_NIGHT_END_MINUTE, NIGHT_END_MINUTE_DEFAULT)
    }

    fun getNightStartTime(context: Context): String {
        return formatAsTime(context, getNightStartHour(context), getNightStartMinute(context))
    }

    fun getNightEndTime(context: Context): String {
        return formatAsTime(context, getNightEndHour(context), getNightEndMinute(context))
    }

    private fun formatAsTime(context: Context, hour: Int, minute: Int): String {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time.let {
            DateFormat.getTimeFormat(context).format(it)
        }
    }

    fun saveThemeMode(context: Context, themeMode: Int) {
        if (themeMode !in THEME_ALWAYS_DAY..THEME_DAY_NIGHT_SYSTEM) {
            throw IllegalArgumentException("themeMode not known")
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putInt(KEY_THEME_MODE, themeMode)
        }
    }

    fun saveNightStart(context: Context, hour: Int, minute: Int) {
        saveHourAndMinute(context, KEY_NIGHT_START_HOUR, hour, KEY_NIGHT_START_MINUTE, minute)
    }

    fun saveNightEnd(context: Context, hour: Int, minute: Int) {
        saveHourAndMinute(context, KEY_NIGHT_END_HOUR, hour, KEY_NIGHT_END_MINUTE, minute)
    }

    private fun saveHourAndMinute(
        context: Context,
        keyHour: String,
        hour: Int,
        keyMinute: String,
        minute: Int
    ) {
        if (hour !in 0..23) throw IllegalArgumentException("hour not within 0..23")
        if (minute !in 0..59) throw IllegalArgumentException("minute not within 0..59")

        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putInt(keyHour, hour)
            putInt(keyMinute, minute)
        }
    }

}