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
    /** Read on the main thread, but changed on a background thread when loading preferences. */
    @Volatile
    var enabled: Boolean = true

    /**
     * Returns a [DisplayRow] with the text to display for this item built from [statusData].
     * Does not modify this item, so it is safe to call on any thread.
     */
    abstract fun toDisplayRow(context: Context, statusData: StatusData): DisplayRow
}

/**
 * A [DisplayItem] and its text built from a specific [StatusData].
 */
class DisplayRow(
    val item: DisplayItem,
    val text: CharSequence
)

class TemperatureItem(
    id: Int,
    type: StatusData.Type
) : DisplayItem(id, type) {

    override fun toDisplayRow(context: Context, statusData: StatusData): DisplayRow {
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

        return DisplayRow(this, builder)
    }

}

open class FullWidthItem(
    id: Int,
    type: StatusData.Type
) : DisplayItem(id, type) {

    override fun toDisplayRow(context: Context, statusData: StatusData): DisplayRow {
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

        return DisplayRow(this, builder)
    }

}

/**
 * Same as [FullWidthItem], but gets less span count (width).
 */
class HalfWidthItem(id: Int, type: StatusData.Type) : FullWidthItem(id, type)
