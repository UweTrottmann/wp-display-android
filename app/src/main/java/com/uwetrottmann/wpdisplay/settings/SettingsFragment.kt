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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.model.DisplayItem
import com.uwetrottmann.wpdisplay.model.DisplayItems
import kotlinx.android.synthetic.main.fragment_settings.*

/**
 * App settings.
 */
class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    private lateinit var viewAdapter: SettingsListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        buttonSettingsStore.setOnClickListener { openWebPage(getString(R.string.store_page_url)) }

        val version = try {
            val packageInfo = requireContext().packageManager
                    .getPackageInfo(requireContext().packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }

        textViewSettingsVersion.text = getString(R.string.version, version)

        viewAdapter = SettingsListAdapter()
        val viewManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
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

        val viewModel = ViewModelProviders.of(this)[SettingsViewModel::class.java]
        viewModel.availableItems.observe(this, Observer<List<DisplayItem>> { list ->
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

    private fun populateViews() {
        editTextSettingsHost.setText(ConnectionSettings.getHost(requireContext()))
        editTextSettingsPort.setText(ConnectionSettings.getPort(requireContext()).toString())
    }

    private fun saveSettings() {
        val host = editTextSettingsHost.text.toString()
        val port = Integer.valueOf(editTextSettingsPort.text.toString())
        ConnectionSettings.saveConnectionSettings(requireContext(), host, port)

        DisplayItems.saveDisabledStateToPreferences(requireContext())
    }

    private fun openWebPage(url: String) {
        val webpage = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        }
    }
}
