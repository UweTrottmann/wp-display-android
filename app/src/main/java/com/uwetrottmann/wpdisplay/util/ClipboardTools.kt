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
        clipboard.primaryClip = clip
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