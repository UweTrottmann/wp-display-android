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

import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.model.StatusData.Temperature
import com.uwetrottmann.wpdisplay.model.StatusData.Time

object DisplayItems {

    val all: List<DisplayItem>
    val enabled = mutableListOf<DisplayItem>()

    init {
        all = listOf(
                TemperatureItem(1, R.string.label_temp_outgoing, Temperature.OUTGOING),
                TemperatureItem(2, R.string.label_temp_return, Temperature.RETURN),
                TemperatureItem(3, R.string.label_temp_outdoors, Temperature.OUTDOORS),
                TemperatureItem(4, R.string.label_temp_return_should, Temperature.RETURN_SHOULD),
                TemperatureItem(5, R.string.label_temp_outdoors_average, Temperature.OUTDOORS_AVERAGE),
                TemperatureItem(6, R.string.label_temp_hot_gas, Temperature.HOT_GAS),
                TemperatureItem(7, R.string.label_temp_water, Temperature.WATER),
                TemperatureItem(8, R.string.label_temp_water_should, Temperature.WATER_SHOULD),
                TemperatureItem(9, R.string.label_temp_source_in, Temperature.SOURCE_IN),
                TemperatureItem(10, R.string.label_temp_source_out, Temperature.SOURCE_OUT),
                TextItem(11, R.string.label_operating_state),
                DurationItem(12, R.string.label_time_pump_active, Time.TIME_PUMP_ACTIVE),
                DurationItem(13, R.string.label_time_compressor_inactive, Time.TIME_COMPRESSOR_NOOP),
                DurationItem(14, R.string.label_time_rest, Time.TIME_REST),
                DurationItem(15, R.string.label_time_return_lower, Time.TIME_RETURN_LOWER),
                DurationItem(16, R.string.label_time_return_higher, Time.TIME_RETURN_HIGHER),
                TextItem(17, R.string.label_firmware)
        )
        enabled.addAll(all)
    }

}