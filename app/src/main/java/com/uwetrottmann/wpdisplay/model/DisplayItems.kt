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

package com.uwetrottmann.wpdisplay.model

import android.content.Context
import androidx.preference.PreferenceManager
import com.uwetrottmann.wpdisplay.model.StatusData.Type.TypeWithOffset.Temperature
import com.uwetrottmann.wpdisplay.model.StatusData.Type.TypeWithOffset.TimeHours
import com.uwetrottmann.wpdisplay.model.StatusData.Type.TypeWithOffset.TimeSeconds

object DisplayItems {

    const val KEY_DISABLED_DISPLAY_ITEMS = "DISABLED_DISPLAY_ITEMS"

    // MAX id is 34.
    val all: List<DisplayItem> = listOf(
        TemperatureItem(1, Temperature.Outgoing),
        TemperatureItem(2, Temperature.Return),
        TemperatureItem(3, Temperature.Outdoors),
        TemperatureItem(4, Temperature.ReturnShould),
        TemperatureItem(20, Temperature.ReturnExternal),
        TemperatureItem(5, Temperature.OutdoorsAverage),
        TemperatureItem(6, Temperature.HotGas),
        TemperatureItem(7, Temperature.Water),
        TemperatureItem(8, Temperature.WaterShould),
        TemperatureItem(9, Temperature.SourceIn),
        TemperatureItem(10, Temperature.SourceOut),
        TemperatureItem(18, Temperature.SolarCollector),
        TemperatureItem(19, Temperature.SolarTank),
        TemperatureItem(21, Temperature.ExternalEnergySource),
        TextItem(11, StatusData.Type.OperatingState),
        DurationItem(12, TimeSeconds.PumpActive),
        DurationItem(32, TimeSeconds.SecondaryHeater1Active),
        DurationItem(33, TimeSeconds.SecondaryHeater2Active),
        DurationItem(13, TimeSeconds.CompressorNoOp),
        DurationItem(14, TimeSeconds.Rest),
        DurationItem(15, TimeSeconds.ReturnLower),
        DurationItem(16, TimeSeconds.ReturnHigher),
        DurationItem(22, TimeHours.OperatingHoursCompressor),
        DurationItem(23, StatusData.Type.TypeWithOffset.Number.CompressorImpulses),
        DurationItem(24, StatusData.Type.CompressorAverageRuntime),
        DurationItem(29, TimeHours.OperatingHoursSecondaryHeater1),
        DurationItem(30, TimeHours.OperatingHoursSecondaryHeater2),
        DurationItem(31, TimeHours.OperatingHoursSecondaryHeater3),
        DurationItem(25, TimeHours.OperatingHoursHeatPump),
        DurationItem(26, TimeHours.OperatingHoursHeating),
        DurationItem(27, TimeHours.OperatingHoursWater),
        DurationItem(28, TimeHours.OperatingHoursSolar),
        TextItem(17, StatusData.Type.FirmwareVersion),
        TextItem(34, StatusData.Type.CurrentTime)
    )
    /** Returns a copy of all DisplayItems that are enabled. */
    val enabled: List<DisplayItem>
        get() = all.filter { it.enabled }.toList()

    @Synchronized
    fun readDisabledStateFromPreferences(context: Context) {
        val disabledEncoded = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_DISABLED_DISPLAY_ITEMS, "")
        // should never be null if there is a non-null default value, annotation issue?
        val disabledIds = disabledEncoded?.split(',')?.mapNotNull {
            if (it == "") null else it.toInt()
        } ?: listOf()
        all.forEach { item ->
            item.enabled = disabledIds.find { it == item.id } == null
        }
    }

    @Synchronized
    fun saveDisabledStateToPreferences(context: Context) {
        val disabledEncoded = all
            .filter { !it.enabled }
            .joinToString(",") { it.id.toString() }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(KEY_DISABLED_DISPLAY_ITEMS, disabledEncoded)
            .apply()
    }

}