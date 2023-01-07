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
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import com.uwetrottmann.wpdisplay.R

abstract class DisplayItem(
    val id: Int,
    val type: StatusData.Type
) {
    var enabled: Boolean = true
    var charSequence: CharSequence = ""

    abstract fun buildCharSequence(context: Context, statusData: StatusData)
}

class TemperatureItem(
    id: Int,
    type: StatusData.Type
) : DisplayItem(id, type) {

    override fun buildCharSequence(context: Context, statusData: StatusData) {
        val builder = SpannableStringBuilder()

        builder.append(context.getString(type.labelResId))
        builder.setSpan(
            TextAppearanceSpan(
                context,
                R.style.TextAppearance_MaterialComponents_Caption
            ), 0, builder.length, 0
        )

        builder.append("\n")

        var lengthOld = builder.length
        builder.append(statusData.getValueFor(type, context))
        builder.setSpan(
            TextAppearanceSpan(
                context,
                R.style.TextAppearance_App_Temperature
            ), lengthOld, builder.length, 0
        )

        lengthOld = builder.length
        builder.append(context.getString(R.string.unit_celsius))
        builder.setSpan(
            TextAppearanceSpan(
                context,
                R.style.TextAppearance_App_Unit
            ), lengthOld, builder.length, 0
        )

        charSequence = builder
    }

}

open class FullWidthItem(
    id: Int,
    type: StatusData.Type
) : DisplayItem(id, type) {

    override fun buildCharSequence(context: Context, statusData: StatusData) {
        val builder = SpannableStringBuilder()

        builder.append(context.getString(type.labelResId))
        builder.setSpan(
            TextAppearanceSpan(
                context,
                R.style.TextAppearance_MaterialComponents_Caption
            ), 0, builder.length, 0
        )

        builder.append("\n")

        val lengthOld = builder.length
        builder.append(statusData.getValueFor(type, context))
        builder.setSpan(
            TextAppearanceSpan(
                context,
                R.style.TextAppearance_MaterialComponents_Headline4
            ), lengthOld, builder.length, 0
        )

        charSequence = builder
    }

}

/**
 * Same as [FullWidthItem], but gets less span count (width).
 */
class HalfWidthItem(id: Int, type: StatusData.Type) : FullWidthItem(id, type)
