// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.uwetrottmann.wpdisplay.settings.ThemeSettings
import timber.log.Timber

class ApplicationImpl : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        AppCompatDelegate.setDefaultNightMode(ThemeSettings.getNightMode(this))
    }
}
