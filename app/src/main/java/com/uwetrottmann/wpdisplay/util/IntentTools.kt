package com.uwetrottmann.wpdisplay.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

fun openWebPage(context: Context, url: String) {
    val webpage = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, webpage)
    // Note: Android docs suggest to use resolveActivity,
    // but won't work on Android 11+ due to package visibility changes.
    // https://developer.android.com/about/versions/11/privacy/package-visibility
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Ignored
    }
}
