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

import android.content.Context
import androidx.annotation.StringRes
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.model.SettingsData.TypeWithOffset.BooleanType.PhotovoltaicsActive
import com.uwetrottmann.wpdisplay.model.StatusData.Type.TypeWithOffset.Number
import com.uwetrottmann.wpdisplay.model.StatusData.Type.TypeWithOffset.TimeHours
import java.text.DateFormat
import java.util.*
import kotlin.math.max

/**
 * Holder object for heat pump controller status data.
 */
class StatusData(
    private val rawData: IntArray,
    val shouldRefreshSettings: Boolean,
    val settingsData: SettingsData
) {

    constructor() : this(IntArray(LENGTH_BYTES), shouldRefreshSettings = true, SettingsData())

    /**
     * Return the [java.util.Date] this status data was stored.
     */
    val timestamp: Date

    init {
        if (rawData.size != LENGTH_BYTES) {
            throw IllegalArgumentException(
                "array is not size $LENGTH_BYTES but was ${rawData.size}"
            )
        }
        this.timestamp = Date()
    }

    fun getLabelFor(type: Type, context: Context): String {
        return when (type) {
            is Type.TypeWithOffset.HeatQuantity.HeatQuantitySwimmingPool -> {
                // If PV is active, the swimming pool becomes PV (:
                if (PhotovoltaicsActive.getValue(settingsData)) {
                    context.getString(R.string.label_text_heat_photovoltaics)
                } else {
                    context.getString(type.labelResId)
                }
            }
            else -> context.getString(type.labelResId)
        }
    }

    fun getValueFor(type: Type, context: Context): String {
        return when (type) {
            is Type.TypeWithOffset.Temperature -> getTemperature(type)
            is Type.TypeWithOffset.TimeSeconds -> getHoursMinutesSeconds(type)
            is TimeHours -> getHours(type)
            is Type.TypeWithOffset.HeatQuantity, is Type.HeatQuantityTotal ->
                getHeatQuantity(context, type)
            is Type.HeatQuantitySinceDate -> getHeatQuantitySinceDate()
            is Number -> getValueAt(type.offset).toString()
            is Type.OperatingState -> context.getString(getOperatingStateStringRes())
            is Type.CompressorAverageRuntime -> getCompressorAverageRuntime(
                Number.CompressorImpulses,
                TimeHours.OperatingHoursCompressor
            )
            is Type.Compressor2AverageRuntime -> getCompressorAverageRuntime(
                Number.Compressor2Impulses,
                TimeHours.OperatingHoursCompressor2
            )
            is Type.FirmwareVersion -> getFirmwareVersion()
            is Type.CurrentTime -> getCurrentTime()
        }
    }

    /**
     * Get a Celsius temperature value with single fractional digit (e.g. "1.5").
     */
    private fun getTemperature(temperature: Type.TypeWithOffset.Temperature): String {
        val tempRaw = getValueAt(temperature.offset)
        val tempValue = tempRaw / 10.0
        return String.format(Locale.getDefault(), "%.1f", tempValue)
    }

    /**
     * Get a heat quantity value with single fractional digit and unit (e.g. "100.5 kWh").
     */
    private fun getHeatQuantity(context: Context, quantity: Type): String {
        val quantityRaw = if (quantity == Type.HeatQuantityTotal) {
            getValueAt(Type.TypeWithOffset.HeatQuantity.HeatQuantityHeating.offset) +
                    getValueAt(Type.TypeWithOffset.HeatQuantity.HeatQuantityWater.offset) +
                    getValueAt(Type.TypeWithOffset.HeatQuantity.HeatQuantitySwimmingPool.offset)
        } else if (quantity is Type.TypeWithOffset.HeatQuantity) {
            getValueAt(quantity.offset)
        } else throw IllegalArgumentException("Given type is not a heat quantity type.")

        val quantityValue = quantityRaw / 10.0
        val quantityDisplayValue = String.format(Locale.getDefault(), "%.1f", quantityValue)
        return "$quantityDisplayValue ${context.getString(R.string.unit_kilowatthours)}"
    }

    private fun getHeatQuantitySinceDate(): String =
        SettingsData.TypeWithOffset.DateType.HeatQuantitySinceDate.getValue(settingsData)

    /**
     * Get a time duration string with second precision, formatted like "1h 2min 3sec".
     */
    private fun getHoursMinutesSeconds(time: Type.TypeWithOffset.TimeSeconds): String {
        return buildHoursMinutesSecondsString(getValueAt(time.offset))
    }

    private fun buildHoursMinutesSecondsString(elapsedSeconds: Int): String {
        var secondsRemaining = elapsedSeconds
        var hours: Long = 0
        var minutes: Long = 0

        if (secondsRemaining >= 3600) {
            hours = (secondsRemaining / 3600).toLong()
            secondsRemaining -= (hours * 3600).toInt()
        }

        if (secondsRemaining >= 60) {
            minutes = (secondsRemaining / 60).toLong()
            secondsRemaining -= (minutes * 60).toInt()
        }

        val seconds = secondsRemaining.toLong()

        return hours.toString() + "h " + minutes + "min " + seconds + "sec"
    }

    /**
     * Get a time duration string with hour precision, formatted like "1h".
     */
    private fun getHours(time: TimeHours): String {
        val elapsedSeconds = getValueAt(time.offset)

        var hours: Long = 0
        if (elapsedSeconds >= 3600) {
            hours = (elapsedSeconds / 3600).toLong()
        }

        return hours.toString() + "h"
    }

    private fun getOperatingStateStringRes(): Int {
        val stateIndex = getValueAt(OPERATING_STATE_INDEX)
        val state = OperatingState.fromIndex(stateIndex)
        // If PV is active, the swimming pool becomes PV (:
        return if (state == OperatingState.SWB && PhotovoltaicsActive.getValue(settingsData)) {
            R.string.state_photovoltaics
        } else {
            state.labelRes
        }
    }

    private fun getCompressorAverageRuntime(
        compressorImpulses: Number,
        compressorHours: TimeHours
    ): String {
        val impulses = max(0, getValueAt(compressorImpulses.offset))
        // Avoid division by 0.
        return if (impulses == 0) {
            "?"
        } else {
            val seconds = getValueAt(compressorHours.offset)
            buildHoursMinutesSecondsString(seconds / impulses)
        }
    }

    /**
     * Text like "V1.23".
     */
    private fun getFirmwareVersion(): String {
        var version = ""
        for (i in FIRMWARE_VERSION_INDEX_BEGIN until FIRMWARE_VERSION_INDEX_BEGIN + FIRMWARE_VERSION_LENGTH) {
            version += getValueAt(i).toChar().toString()
        }
        return version
    }

    private fun getCurrentTime(): String {
        val unixTimeInSeconds = getValueAt(CURRENT_TIME_INDEX)
        return DateFormat.getDateTimeInstance().format(Date(unixTimeInSeconds.toLong() * 1000))
    }

    private fun getValueAt(index: Int): Int {
        if (index + 1 > rawData.size) {
            throw IllegalArgumentException(
                "offset must be from 0 to array length ${rawData.size}"
            )
        }

        return rawData[index]
    }

    companion object {

        /**
         * Maximum length of data supported. Sent status data is 200 bytes long for my controller,
         * but values up to 231 are documented (see docs folder).
         */
        const val LENGTH_BYTES = 232

        private const val FIRMWARE_VERSION_INDEX_BEGIN = 81
        private const val FIRMWARE_VERSION_LENGTH = 10

        private const val OPERATING_STATE_INDEX = 80

        private const val CURRENT_TIME_INDEX = 134
    }

    sealed class Type(@StringRes val labelResId: Int) {

        object OperatingState : Type(R.string.label_operating_state)
        object CompressorAverageRuntime : Type(R.string.label_compressor_average_runtime)
        object Compressor2AverageRuntime : Type(R.string.label_compressor2_average_runtime)
        object HeatQuantityTotal : Type(R.string.label_text_heat_total)
        object HeatQuantitySinceDate : Type(R.string.label_text_heat_since)
        object FirmwareVersion : Type(R.string.label_firmware)
        object CurrentTime : Type(R.string.label_current_time)

        sealed class TypeWithOffset(
            @StringRes labelResId: Int, val offset: Int
        ) : Type(labelResId) {

            /**
             * Temperature values, factor 10, Celsius.
             */
            sealed class Temperature(
                offset: Int, @StringRes labelResId: Int
            ) : TypeWithOffset(labelResId, offset) {

                object Outgoing
                    : Temperature(10, R.string.label_temp_outgoing)

                object Return
                    : Temperature(11, R.string.label_temp_return)

                object ReturnShould
                    : Temperature(12, R.string.label_temp_return_should)

                object ReturnExternal
                    : Temperature(13, R.string.label_temp_return_external)

                object HotGas
                    : Temperature(14, R.string.label_temp_hot_gas)

                object Outdoors
                    : Temperature(15, R.string.label_temp_outdoors)

                object OutdoorsAverage
                    : Temperature(16, R.string.label_temp_outdoors_average)

                object Water
                    : Temperature(17, R.string.label_temp_water)

                object WaterShould
                    : Temperature(18, R.string.label_temp_water_should)

                object SourceIn
                    : Temperature(19, R.string.label_temp_source_in)

                object SourceOut
                    : Temperature(20, R.string.label_temp_source_out)

                object MixingCircuit1
                    : Temperature(21, R.string.label_temp_mix_1_outgoing)

                object MixingCircuit1Target
                    : Temperature(22, R.string.label_temp_mix_1_outgoing_target)

                object MixingCircuit2
                    : Temperature(24, R.string.label_temp_mix_2_outgoing)

                object MixingCircuit2Target
                    : Temperature(25, R.string.label_temp_mix_2_outgoing_target)

                object SolarCollector
                    : Temperature(26, R.string.label_temp_solar_collector)

                object SolarTank
                    : Temperature(27, R.string.label_temp_solar_tank)

                object ExternalEnergySource
                    : Temperature(28, R.string.label_temp_ext_energy_src)

            }

            /**
             * Time values, factor 1, second precision.
             */
            sealed class TimeSeconds(
                offset: Int, @StringRes labelResId: Int
            ) : TypeWithOffset(labelResId, offset) {

                object PumpActive
                    : TimeSeconds(67, R.string.label_time_pump_active)

                object SecondaryHeater1Active
                    : TimeSeconds(68, R.string.label_time_secondary_heater1_active)

                object SecondaryHeater2Active
                    : TimeSeconds(69, R.string.label_time_secondary_heater2_active)

                object Rest
                    : TimeSeconds(71, R.string.label_time_rest)

                object CompressorNoOp
                    : TimeSeconds(73, R.string.label_time_compressor_inactive)

                object ReturnLower
                    : TimeSeconds(74, R.string.label_time_return_lower)

                object ReturnHigher
                    : TimeSeconds(75, R.string.label_time_return_higher)

            }

            /**
             * Time values, factor 3600, hour precision.
             */
            sealed class TimeHours(
                offset: Int, @StringRes labelResId: Int
            ) : TypeWithOffset(labelResId, offset) {

                object OperatingHoursCompressor
                    : TimeHours(56, R.string.label_hours_compressor)

                object OperatingHoursCompressor2
                    : TimeHours(58, R.string.label_hours_compressor2)

                object OperatingHoursSecondaryHeater1
                    : TimeHours(60, R.string.label_hours_secondary_heater1)

                object OperatingHoursSecondaryHeater2
                    : TimeHours(61, R.string.label_hours_secondary_heater2)

                object OperatingHoursSecondaryHeater3
                    : TimeHours(62, R.string.label_hours_secondary_heater3)

                object OperatingHoursHeatPump
                    : TimeHours(63, R.string.label_hours_pump)

                object OperatingHoursHeating
                    : TimeHours(64, R.string.label_hours_heating)

                object OperatingHoursWater
                    : TimeHours(65, R.string.label_hours_water)

                object OperatingHoursSolar
                    : TimeHours(161, R.string.label_hours_solar)

            }

            /**
             * Heat quantity in kWh.
             */
            sealed class HeatQuantity(
                offset: Int, @StringRes labelResId: Int
            ) : TypeWithOffset(labelResId, offset) {

                object HeatQuantityHeating
                    : HeatQuantity(151, R.string.label_text_heat_heating)

                object HeatQuantityWater
                    : HeatQuantity(152, R.string.label_text_heat_water)

                object HeatQuantitySwimmingPool
                    : HeatQuantity(153, R.string.label_text_heat_swimming_pool)

                object HeatQuantitySince
                    : HeatQuantity(154, R.string.label_text_heat_since)

            }

            /**
             * Number values.
             */
            sealed class Number(
                offset: Int, @StringRes labelResId: Int
            ) : TypeWithOffset(labelResId, offset) {

                object CompressorImpulses
                    : Number(57, R.string.label_text_compressor_impulses)

                object Compressor2Impulses
                    : Number(59, R.string.label_text_compressor2_impulses)

                object CompressorFrequency
                    : Number(231, R.string.label_text_compressor_frequency)

            }
        }
    }

    private enum class OperatingState(
        val index: Int,
        @param:StringRes @field:StringRes val labelRes: Int
    ) {

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
                return values().firstOrNull { it.index == index } ?: UNKNOWN
            }
        }
    }
}
