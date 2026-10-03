// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2023 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.model

import java.text.DateFormat
import java.util.Date

/**
 * Holds settings data retrieved from the controller.
 */
class SettingsData(val rawData: IntArray) {

    constructor() : this(IntArray(MAX_VALUES))

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
         * Maximum number of values (32-bit integers) supported. My controller sends 1123 values,
         * but only values up to 1061 are documented (see docs folder).
         */
        const val MAX_VALUES = 1061
    }
}