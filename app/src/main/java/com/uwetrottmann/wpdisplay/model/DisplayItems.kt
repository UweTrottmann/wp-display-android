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
import com.uwetrottmann.wpdisplay.model.StatusData.Type
import com.uwetrottmann.wpdisplay.model.StatusData.Type.TypeWithOffset.HeatQuantity
import com.uwetrottmann.wpdisplay.model.StatusData.Type.TypeWithOffset.Temperature
import com.uwetrottmann.wpdisplay.model.StatusData.Type.TypeWithOffset.TimeHours
import com.uwetrottmann.wpdisplay.model.StatusData.Type.TypeWithOffset.TimeSeconds

object DisplayItems {

    const val KEY_DISABLED_DISPLAY_ITEMS = "DISABLED_DISPLAY_ITEMS"

    // Current highest id is 50.
    // Make sure to assign the next highest available id when adding a new item.
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
        TemperatureItem(38, Temperature.MixingCircuit1),
        TemperatureItem(39, Temperature.MixingCircuit1Target),
        TemperatureItem(40, Temperature.MixingCircuit2),
        TemperatureItem(41, Temperature.MixingCircuit2Target),
        TemperatureItem(49, Temperature.MixingCircuit3),
        TemperatureItem(50, Temperature.MixingCircuit3Target),
        TemperatureItem(18, Temperature.SolarCollector),
        TemperatureItem(19, Temperature.SolarTank),
        TemperatureItem(21, Temperature.ExternalEnergySource),
        FullWidthItem(11, Type.OperatingState),
        HalfWidthItem(12, TimeSeconds.PumpActive),
        HalfWidthItem(32, TimeSeconds.SecondaryHeater1Active),
        HalfWidthItem(33, TimeSeconds.SecondaryHeater2Active),
        HalfWidthItem(13, TimeSeconds.CompressorNoOp),
        HalfWidthItem(14, TimeSeconds.Rest),
        HalfWidthItem(15, TimeSeconds.ReturnLower),
        HalfWidthItem(16, TimeSeconds.ReturnHigher),
        HalfWidthItem(22, TimeHours.OperatingHoursCompressor),
        HalfWidthItem(23, Type.TypeWithOffset.Number.CompressorImpulses),
        HalfWidthItem(24, Type.CompressorAverageRuntime),
        HalfWidthItem(42, Type.TypeWithOffset.Number.CompressorFrequency),
        HalfWidthItem(35, TimeHours.OperatingHoursCompressor2),
        HalfWidthItem(36, Type.TypeWithOffset.Number.Compressor2Impulses),
        HalfWidthItem(37, Type.Compressor2AverageRuntime),
        HalfWidthItem(29, TimeHours.OperatingHoursSecondaryHeater1),
        HalfWidthItem(30, TimeHours.OperatingHoursSecondaryHeater2),
        HalfWidthItem(31, TimeHours.OperatingHoursSecondaryHeater3),
        HalfWidthItem(25, TimeHours.OperatingHoursHeatPump),
        HalfWidthItem(26, TimeHours.OperatingHoursHeating),
        HalfWidthItem(27, TimeHours.OperatingHoursWater),
        HalfWidthItem(28, TimeHours.OperatingHoursSolar),
        HalfWidthItem(43, HeatQuantity.HeatQuantityHeating),
        HalfWidthItem(44, HeatQuantity.HeatQuantityWater),
        HalfWidthItem(45, HeatQuantity.HeatQuantitySwimmingPool),
        HalfWidthItem(47, Type.HeatQuantityTotal),
        HalfWidthItem(46, HeatQuantity.HeatQuantitySince),
        HalfWidthItem(48, Type.HeatQuantitySinceDate),
        FullWidthItem(17, Type.FirmwareVersion),
        FullWidthItem(34, Type.CurrentTime)
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