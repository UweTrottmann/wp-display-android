// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2018 Uwe Trottmann <uwe@uwetrottmann.com>

@file:JvmName("ClipboardTools")

package com.uwetrottmann.wpdisplay.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.uwetrottmann.wpdisplay.R

fun copyTextToClipboard(context: Context, text: CharSequence): Boolean {
    val clip = ClipData.newPlainText("text", text)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    return if (clipboard != null) {
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.copy_to_clipboard, Toast.LENGTH_SHORT).show()
        true
    } else {
        false
    }
}

private val onClickListener = View.OnClickListener {
    it is TextView && copyTextToClipboard(it.context, it.text)
}

fun TextView.copyTextToClipboardOnClick() {
    // globally shared click listener instance
    setOnClickListener(onClickListener)
}