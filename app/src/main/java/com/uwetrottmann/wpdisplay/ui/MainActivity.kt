/*
 * Copyright 2015 Uwe Trottmann
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

package com.uwetrottmann.wpdisplay.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.display.DisplayFragment
import com.uwetrottmann.wpdisplay.settings.ThemeSettings
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // setup action bar
        val actionBarToolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(actionBarToolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, DisplayFragment())
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                supportFragmentManager.popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()

        // Recreate activity if theme changes based on time.
        if (ThemeSettings.getThemeMode(this) == ThemeSettings.THEME_DAY_NIGHT) {
            val currentNightMode =
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES
            val expectedNightMode = ThemeSettings.getNightMode(this)
            val isExpectedNightMode = expectedNightMode == AppCompatDelegate.MODE_NIGHT_YES
            if (isNightMode != isExpectedNightMode) {
                Timber.d("Changing night mode, is $isNightMode, should be $isExpectedNightMode")
                AppCompatDelegate.setDefaultNightMode(expectedNightMode)
            }
        }
    }
}
