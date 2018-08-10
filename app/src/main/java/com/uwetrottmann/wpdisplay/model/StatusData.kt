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

package com.uwetrottmann.wpdisplay.model

import androidx.annotation.StringRes
import com.uwetrottmann.wpdisplay.R
import java.util.*

/**
 * Holder object for heat pump controller status data.
 */
class StatusData(private val rawData: IntArray) {

    constructor() : this(IntArray(LENGTH_BYTES))

    /**
     * Return the [java.util.Date] this status data was stored.
     */
    val timestamp: Date

    val firmwareVersion: String
        get() {
            var version = ""
            for (i in FIRMWARE_VERSION_INDEX_BEGIN until FIRMWARE_VERSION_INDEX_BEGIN + FIRMWARE_VERSION_LENGTH) {
                version += getValueAt(i).toChar().toString()
            }
            return version
        }

    val operatingState: Int
        @StringRes
        get() {
            val state = getValueAt(OPERATING_STATE_INDEX)
            return OperatingState.fromIndex(state).labelRes
        }

    init {
        if (rawData.size != LENGTH_BYTES) {
            throw IllegalArgumentException(
                    "array is not size $LENGTH_BYTES but was ${rawData.size}")
        }
        this.timestamp = Date()
    }

    /**
     * Get a Celsius temperature value.
     */
    fun getTemperature(temperature: Temperature): Double {
        val tempRaw = getValueAt(temperature.offset)
        return tempRaw / 10.0
    }

    /**
     * Get a time duration string, formatted like "1h 2min 3sec".
     */
    fun getTime(time: Time): String {
        var elapsedSeconds = getValueAt(time.offset)

        var hours: Long = 0
        var minutes: Long = 0
        if (elapsedSeconds >= 3600) {
            hours = (elapsedSeconds / 3600).toLong()
            elapsedSeconds -= (hours * 3600).toInt()
        }
        if (elapsedSeconds >= 60) {
            minutes = (elapsedSeconds / 60).toLong()
            elapsedSeconds -= (minutes * 60).toInt()
        }
        val seconds = elapsedSeconds.toLong()

        return hours.toString() + "h " + minutes + "min " + seconds + "sec"
    }

    private fun getValueAt(index: Int): Int {
        if (index + 1 > rawData.size) {
            throw IllegalArgumentException(
                    "offset must be from 0 to array length ${rawData.size}")
        }

        return rawData[index]
    }

    companion object {

        /**
         * Maximum length of data supported. Sent status data is actually 183 bytes long, but we don't
         * care about the rest, yet.
         */
        val LENGTH_BYTES = 100

        private val FIRMWARE_VERSION_INDEX_BEGIN = 81
        private val FIRMWARE_VERSION_LENGTH = 10

        private val OPERATING_STATE_INDEX = 80
    }

    /**
     * Temperature values, factor 10, Celsius.
     */
    enum class Temperature(val offset: Int) {

        OUTGOING(10),
        RETURN(11),
        RETURN_SHOULD(12),
        HOT_GAS(14),
        OUTDOORS(15),
        OUTDOORS_AVERAGE(16),
        WATER(17),
        WATER_SHOULD(18),
        SOURCE_IN(19),
        SOURCE_OUT(20),
        SOLAR_COLLECTOR(26),
        SOLAR_TANK(27)
    }

    /**
     * Time values, factor 1, Seconds.
     */
    enum class Time(val offset: Int) {

        TIME_PUMP_ACTIVE(67),
        TIME_REST(71),
        TIME_COMPRESSOR_NOOP(73),
        TIME_RETURN_LOWER(74),
        TIME_RETURN_HIGHER(75)
    }

    private enum class OperatingState(val index: Int, @param:StringRes @field:StringRes val labelRes: Int) {

        UNKNOWN(-1, R.string.state_unknown),
        HEATING(0, R.string.state_heating),
        WATER(1, R.string.state_water),
        SWB(2, R.string.state_swimming_pool),
        EVU(3, R.string.state_power_supply_company),
        DEFROST(4, R.string.state_defrost),
        NO_OP(5, R.string.state_noop),
        EXT_ENERG(6, R.string.state_ext_energ),
        COOLING(7, R.string.state_cooling);


        companion object {

            fun fromIndex(index: Int): OperatingState {
                return values().firstOrNull { it.index == index }
                        ?: UNKNOWN
            }
        }
    }
}
