package com.uwetrottmann.wpdisplay.model

import java.text.DateFormat
import java.util.*

/*
 * Copyright 2023 Uwe Trottmann
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