// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2023 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.model

import java.text.DateFormat
import java.util.Date

/**
 * Holds settings data retrieved from the controller.
 */
class SettingsData(val rawData: IntArray) {

    constructor() : this(IntArray(LENGTH_BYTES))

    private fun getValueAt(index: Int): Int {
        if (index + 1 > rawData.size) {
            throw IllegalArgumentException(
                "offset must be from 0 to array length ${rawData.size}"
            )
        }

        return rawData[index]
    }

    sealed class TypeWithOffset(val offset: Int) {

        sealed class BooleanType(offset: Int) : TypeWithOffset(offset) {

            object PhotovoltaicsActive : BooleanType(976)

            fun getValue(settingsData: SettingsData): Boolean {
                return settingsData.getValueAt(offset) != 0
            }
        }

        sealed class DateType(offset: Int) : TypeWithOffset(offset) {

            object HeatQuantitySinceDate : DateType(880)

            fun getValue(settingsData: SettingsData): String {
                val unixTimeInSeconds = settingsData.getValueAt(offset)
                return DateFormat.getDateTimeInstance()
                    .format(Date(unixTimeInSeconds.toLong() * 1000))
            }
        }
    }

    companion object {
        /**
         * Maximum length of data supported. Sent data is 1123 bytes long for my controller,
         * but only values up to 1061 are documented (see docs folder).
         */
        const val LENGTH_BYTES = 1061
    }
}