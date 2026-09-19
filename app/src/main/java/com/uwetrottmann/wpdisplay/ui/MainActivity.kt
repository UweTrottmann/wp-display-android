// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.databinding.ActivityMainBinding
import com.uwetrottmann.wpdisplay.display.DisplayFragment
import com.uwetrottmann.wpdisplay.settings.ThemeSettings
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION") // Not using R only WindowInsetsController, yet.
            binding.root.systemUiVisibility =
                    // Tells the system that the window wishes the content to
                    // be laid out at the most extreme scenario.
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        // Tells the system that the window wishes the content to
                        // be laid out as if the navigation bar was hidden.
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }

        // setup action bar
        setSupportActionBar(binding.toolbar)

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
