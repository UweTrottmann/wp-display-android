// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
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
import com.uwetrottmann.wpdisplay.util.tryStartActivity

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
                val bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout()
                )
                v.updatePadding(
                    left = bars.left,
                    right = bars.right,
                    bottom = bars.bottom,
                )
                insets
            }
        }

        // Local network permission (only shown on Android 17 and up)
        val isAtLeastAndroid17 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN
        binding.permissionsLayout.isVisible = isAtLeastAndroid17
        if (isAtLeastAndroid17) {
            binding.buttonRequestLocalNetworkPermission.setOnClickListener {
                requestLocalNetworkPermission()
            }
            binding.buttonManagePermissions.setOnClickListener {
                openSystemAppSettings()
            }
        }

        binding.buttonSoftwareUpdate.setOnClickListener {
            openWebPage(requireContext(), getString(R.string.url_software_update))
        }
        binding.buttonWebInterface.setOnClickListener {
            val url = "http://${ConnectionSettings.getHost(requireContext())}"
            openWebPage(requireContext(), url)
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

        binding.buttonSettingsRepo.setOnClickListener {
            openWebPage(requireContext(), getString(R.string.url_repo))
        }

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
        updateLocalNetworkPermissionUi()

        val host = ConnectionSettings.getHost(requireContext())
        binding.editTextSettingsHost.setText(host)
        // Don't use a local format for the port number
        @SuppressLint("SetTextI18n")
        binding.editTextSettingsPort.setText(
            ConnectionSettings.getPort(requireContext()).toString()
        )
        binding.buttonWebInterface.isEnabled = !host.isNullOrEmpty()

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

    private val requestLocalNetworkPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        updateLocalNetworkPermissionUi()
    }

    /**
     * On Android 17 and up requests the local network permission. Otherwise, does nothing.
     */
    private fun requestLocalNetworkPermission() {
        // Double check to avoid lint error
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) {
            return
        }
        if (!isLocalNetworkPermissionGranted()) {
            requestLocalNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }

    /**
     * On Android 17 and up returns whether the local network permission is granted. Otherwise,
     * returns true.
     */
    private fun isLocalNetworkPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_LOCAL_NETWORK
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun updateLocalNetworkPermissionUi() {
        val isGranted = isLocalNetworkPermissionGranted()
        binding.textViewLocalNetworkPermission.setText(
            if (isGranted) {
                R.string.local_network_permission_given
            } else {
                R.string.local_network_permission_required
            }
        )
        binding.buttonRequestLocalNetworkPermission.isEnabled = !isGranted
    }

    private fun getPackageNameUri() = Uri.fromParts("package", requireContext().packageName, null)

    /**
     * Tries to open system app settings where users can configure permissions for this app. If not
     * possible, tries to open the manage all apps screen.
     */
    private fun openSystemAppSettings() {
        // Try to open app info where user can clear app cache folders
        val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .apply { data = getPackageNameUri() }
        if (!requireActivity().tryStartActivity(detailsIntent)) {
            // Try to open all apps view if detail view not available
            val allIntent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            requireActivity().tryStartActivity(allIntent)
        }
    }

}
