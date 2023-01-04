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

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uwetrottmann.wpdisplay.BuildConfig
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.databinding.FragmentSettingsBinding
import com.uwetrottmann.wpdisplay.model.DisplayItems
import com.uwetrottmann.wpdisplay.util.openWebPage

/**
 * App settings.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private lateinit var viewAdapter: SettingsListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val actionBar = (activity as AppCompatActivity).supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_settings)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        // Drawing behind navigation bar on Android 10+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.scrollViewSettings) { v, insets ->
                v.updatePadding(bottom = insets.systemWindowInsetBottom)
                insets
            }
        }

        binding.buttonSettingsStore.setOnClickListener {
            openWebPage(requireContext(), getString(R.string.store_page_url))
        }
        binding.radioSettingsColorSchemeSystem.apply {
            setText(
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    R.string.color_scheme_battery_saver
                } else {
                    R.string.color_scheme_system
                }
            )
            setOnClickListener {
                updateColorScheme()
                binding.linearLayoutSettingsTime.visibility = View.GONE
            }
        }
        binding.radioSettingsColorSchemeLight.setOnClickListener {
            updateColorScheme()
            binding.linearLayoutSettingsTime.visibility = View.GONE
        }
        binding.radioSettingsColorSchemeDark.setOnClickListener {
            updateColorScheme()
            binding.linearLayoutSettingsTime.visibility = View.GONE
        }
        binding.radioSettingsColorSchemeAuto.setOnClickListener {
            updateColorScheme()
            binding.linearLayoutSettingsTime.visibility = View.VISIBLE
        }
        binding.buttonSettingsNightFrom.setOnClickListener {
            NightTimePickerFragment.showIfSafe(
                parentFragmentManager, true,
                ThemeSettings.getNightStartHour(it.context),
                ThemeSettings.getNightStartMinute(it.context)
            )
        }
        binding.buttonSettingsNightUntil.setOnClickListener {
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

        binding.textViewSettingsVersion.text = getString(R.string.version, versionString)

        viewAdapter = SettingsListAdapter()
        val viewManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.recyclerViewSettings.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        val viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        viewModel.availableItems.observe(viewLifecycleOwner) { list ->
            viewAdapter.submitList(list)
        }
    }

    override fun onResume() {
        super.onResume()

        populateViews()
    }

    override fun onPause() {
        super.onPause()

        saveSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun updateColorScheme() {
        saveSettings()
        AppCompatDelegate.setDefaultNightMode(ThemeSettings.getNightMode(requireContext()))
    }

    fun populateViews() {
        binding.editTextSettingsHost.setText(ConnectionSettings.getHost(requireContext()))
        binding.editTextSettingsPort.setText(
            ConnectionSettings.getPort(requireContext()).toString()
        )

        when (ThemeSettings.getThemeMode(requireContext())) {
            ThemeSettings.THEME_ALWAYS_DAY -> {
                binding.radioGroupColorScheme.check(R.id.radioSettingsColorSchemeLight)
                binding.linearLayoutSettingsTime.visibility = View.GONE
            }
            ThemeSettings.THEME_ALWAYS_NIGHT -> {
                binding.radioGroupColorScheme.check(R.id.radioSettingsColorSchemeDark)
                binding.linearLayoutSettingsTime.visibility = View.GONE
            }
            ThemeSettings.THEME_DAY_NIGHT -> {
                binding.radioGroupColorScheme.check(R.id.radioSettingsColorSchemeAuto)
                binding.linearLayoutSettingsTime.visibility = View.VISIBLE
            }
            else -> {
                binding.radioGroupColorScheme.check(R.id.radioSettingsColorSchemeSystem)
                binding.linearLayoutSettingsTime.visibility = View.GONE
            }
        }

        binding.buttonSettingsNightFrom.text = ThemeSettings.getNightStartTime(requireContext())
        binding.buttonSettingsNightUntil.text = ThemeSettings.getNightEndTime(requireContext())
    }

    private fun saveSettings() {
        val host = binding.editTextSettingsHost.text.toString()
        val port = Integer.valueOf(binding.editTextSettingsPort.text.toString())
        ConnectionSettings.saveConnectionSettings(requireContext(), host, port)

        DisplayItems.saveDisabledStateToPreferences(requireContext())

        val themeMode = when (binding.radioGroupColorScheme.checkedRadioButtonId) {
            R.id.radioSettingsColorSchemeLight -> ThemeSettings.THEME_ALWAYS_DAY
            R.id.radioSettingsColorSchemeDark -> ThemeSettings.THEME_ALWAYS_NIGHT
            R.id.radioSettingsColorSchemeAuto -> ThemeSettings.THEME_DAY_NIGHT
            else -> ThemeSettings.THEME_DAY_NIGHT_SYSTEM
        }
        ThemeSettings.saveThemeMode(requireContext(), themeMode)
    }

}
