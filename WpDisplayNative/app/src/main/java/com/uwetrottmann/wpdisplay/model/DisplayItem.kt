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

import android.support.annotation.StringRes
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import android.widget.TextView
import com.uwetrottmann.wpdisplay.R
import java.util.*

abstract class DisplayItem(val id: Int)

class TemperatureItem(
        id: Int,
        @StringRes val labelResId: Int,
        val temperature: StatusData.Temperature
) : DisplayItem(id) {

    fun setTemperature(view: TextView, statusData: StatusData) {
        val value = statusData.getTemperature(temperature)

        val context = view.context
        val builder = SpannableStringBuilder()

        builder.append(context.getString(labelResId))
        builder.setSpan(TextAppearanceSpan(context,
                R.style.TextAppearance_AppCompat_Caption), 0, builder.length, 0)

        builder.append("\n")

        var lengthOld = builder.length
        builder.append(String.format(Locale.getDefault(), "%.1f", value))
        builder.setSpan(TextAppearanceSpan(context,
                R.style.TextAppearance_AppCompat_Display3), lengthOld, builder.length, 0)

        lengthOld = builder.length
        builder.append(context.getString(R.string.unit_celsius))
        builder.setSpan(TextAppearanceSpan(context,
                R.style.TextAppearance_App_Unit), lengthOld, builder.length, 0)

        view.text = builder
    }

}

class DurationItem(
        id: Int,
        @StringRes val labelResId: Int,
        val time: StatusData.Time
) : DisplayItem(id) {

    fun setDuration(view: TextView, statusData: StatusData) {
        val value = statusData.getTime(time)

        val context = view.context
        val builder = SpannableStringBuilder()

        builder.append(context.getString(labelResId))
        builder.setSpan(TextAppearanceSpan(context,
                R.style.TextAppearance_AppCompat_Caption), 0, builder.length, 0)

        builder.append("\n")

        val lengthOld = builder.length
        builder.append(value)
        builder.setSpan(TextAppearanceSpan(context,
                R.style.TextAppearance_AppCompat_Display1), lengthOld, builder.length, 0)

        view.text = builder
    }

}

class TextItem(
        id: Int,
        @StringRes val labelResId: Int
) : DisplayItem(id) {

    fun setText(view: TextView, statusData: StatusData) {
        val context = view.context
        val value = when (labelResId) {
            R.string.label_operating_state -> context.getString(statusData.operatingState)
            R.string.label_firmware -> statusData.firmwareVersion
            else -> ""
        }
        val builder = SpannableStringBuilder()

        builder.append(context.getString(labelResId))
        builder.setSpan(TextAppearanceSpan(context,
                R.style.TextAppearance_AppCompat_Caption), 0, builder.length, 0)

        builder.append("\n")

        val lengthOld = builder.length
        builder.append(value)
        builder.setSpan(TextAppearanceSpan(context,
                R.style.TextAppearance_AppCompat_Display1), lengthOld, builder.length, 0)

        view.text = builder
    }

}
