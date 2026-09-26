// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2023 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import timber.log.Timber

fun openWebPage(context: Context, url: String) {
    val webpage = url.toUri()
    val intent = Intent(Intent.ACTION_VIEW, webpage)
    context.tryStartActivity(intent)
}

/**
 * Calls [Context.startActivity] with the given [Intent]. Returns false if
 * no activity found to handle it.
 *
 * This is useful for example for an implicit intent that may fail to open the web browser
 * app if it is disabled in a restricted profile.
 */
fun Context.tryStartActivity(intent: Intent): Boolean {
    // Note: Android docs suggest to use resolveActivity,
    // but won't work on Android 11+ due to package visibility changes.
    // https://developer.android.com/about/versions/11/privacy/package-visibility
    var handled: Boolean
    try {
        startActivity(intent)
        handled = true
    } catch (e: ActivityNotFoundException) {
        Timber.i(e, "Failed to launch intent.")
        handled = false
    } catch (e: SecurityException) {
        Timber.i(e, "Failed to launch intent.")
        handled = false
    }
    return handled
}
