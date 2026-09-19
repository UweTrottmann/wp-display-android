// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2018 Uwe Trottmann <uwe@uwetrottmann.com>

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

        builder.append(statusData.getLabelFor(type, context))
        builder.setSpan(
            TextAppearanceSpan(
                context,
                R.style.TextAppearance_App_Caption
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

        builder.append(statusData.getLabelFor(type, context))
        builder.setSpan(
            TextAppearanceSpan(
                context,
                R.style.TextAppearance_App_Caption
            ), 0, builder.length, 0
        )

        builder.append("\n")

        val lengthOld = builder.length
        builder.append(statusData.getValueFor(type, context))
        builder.setSpan(
            TextAppearanceSpan(
                context,
                R.style.TextAppearance_App_TextItem
            ), lengthOld, builder.length, 0
        )

        charSequence = builder
    }

}

/**
 * Same as [FullWidthItem], but gets less span count (width).
 */
class HalfWidthItem(id: Int, type: StatusData.Type) : FullWidthItem(id, type)
