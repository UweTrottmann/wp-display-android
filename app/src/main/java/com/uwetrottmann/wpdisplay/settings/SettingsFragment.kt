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

package com.uwetrottmann.wpdisplay.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uwetrottmann.wpdisplay.BuildConfig
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.model.DisplayItem
import com.uwetrottmann.wpdisplay.model.DisplayItems
import kotlinx.android.synthetic.main.fragment_settings.*

/**
 * App settings.
 */
class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    private lateinit var viewAdapter: SettingsListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        buttonSettingsStore.setOnClickListener { openWebPage(getString(R.string.store_page_url)) }
        radioSettingsColorSchemeLight.setOnClickListener {
            updateColorScheme()
            linearLayoutSettingsTime.visibility = View.GONE
        }
        radioSettingsColorSchemeDark.setOnClickListener {
            updateColorScheme()
            linearLayoutSettingsTime.visibility = View.GONE
        }
        radioSettingsColorSchemeAuto.setOnClickListener {
            updateColorScheme()
            linearLayoutSettingsTime.visibility = View.VISIBLE
        }
        buttonSettingsNightFrom.setOnClickListener {
            NightTimePickerFragment.showIfSafe(
                parentFragmentManager, true,
                ThemeSettings.getNightStartHour(it.context),
                ThemeSettings.getNightStartMinute(it.context)
            )
        }
        buttonSettingsNightUntil.setOnClickListener {
            NightTimePickerFragment.showIfSafe(
                parentFragmentManager, false,
                ThemeSettings.getNightEndHour(it.context),
                ThemeSettings.getNightEndMinute(it.context)
            )
        }

        val version = try {
            val packageInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }

        val versionString = if (BuildConfig.DEBUG) {
            "$version-debug"
        } else {
            version
        }

        textViewSettingsVersion.text = getString(R.string.version, versionString)

        viewAdapter = SettingsListAdapter()
        val viewManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        recyclerViewSettings.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val actionBar = (activity as AppCompatActivity).supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_settings)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        val viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        viewModel.availableItems.observe(viewLifecycleOwner, Observer<List<DisplayItem>> { list ->
            viewAdapter.submitList(list)
        })
    }

    override fun onResume() {
        super.onResume()

        populateViews()
    }

    override fun onPause() {
        super.onPause()

        saveSettings()
    }

    fun updateColorScheme() {
        saveSettings()

        val nightMode = if (ThemeSettings.isNight(requireContext())) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        (requireActivity() as AppCompatActivity).delegate.setLocalNightMode(nightMode)
    }

    fun populateViews() {
        editTextSettingsHost.setText(ConnectionSettings.getHost(requireContext()))
        editTextSettingsPort.setText(ConnectionSettings.getPort(requireContext()).toString())

        when (ThemeSettings.getThemeMode(requireContext())) {
            ThemeSettings.THEME_ALWAYS_NIGHT -> {
                radioGroupColorScheme.check(R.id.radioSettingsColorSchemeDark)
                linearLayoutSettingsTime.visibility = View.GONE
            }
            ThemeSettings.THEME_DAY_NIGHT -> {
                radioGroupColorScheme.check(R.id.radioSettingsColorSchemeAuto)
                linearLayoutSettingsTime.visibility = View.VISIBLE
            }
            else -> {
                radioGroupColorScheme.check(R.id.radioSettingsColorSchemeLight)
                linearLayoutSettingsTime.visibility = View.GONE
            }
        }

        buttonSettingsNightFrom.text = ThemeSettings.getNightStartTime(requireContext())
        buttonSettingsNightUntil.text = ThemeSettings.getNightEndTime(requireContext())
    }

    private fun saveSettings() {
        val host = editTextSettingsHost.text.toString()
        val port = Integer.valueOf(editTextSettingsPort.text.toString())
        ConnectionSettings.saveConnectionSettings(requireContext(), host, port)

        DisplayItems.saveDisabledStateToPreferences(requireContext())

        val themeMode = when (radioGroupColorScheme.checkedRadioButtonId) {
            R.id.radioSettingsColorSchemeDark -> ThemeSettings.THEME_ALWAYS_NIGHT
            R.id.radioSettingsColorSchemeAuto -> ThemeSettings.THEME_DAY_NIGHT
            else -> ThemeSettings.THEME_ALWAYS_DAY
        }
        ThemeSettings.saveThemeMode(requireContext(), themeMode)
    }

    private fun openWebPage(url: String) {
        val webpage = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        }
    }
}
