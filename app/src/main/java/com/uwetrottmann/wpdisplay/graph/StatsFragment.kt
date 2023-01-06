/*
 * Copyright 2023 Uwe Trottmann
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

package com.uwetrottmann.wpdisplay.graph

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uwetrottmann.dtareader.DtaFileReader
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.databinding.FragmentStatsBinding
import com.uwetrottmann.wpdisplay.settings.ConnectionSettings
import com.uwetrottmann.wpdisplay.util.openWebPage
import java.text.SimpleDateFormat
import java.util.*

/**
 * Displays historical values from the DTA file provided by the controller.
 */
class StatsFragment : Fragment() {

    private var binding: FragmentStatsBinding? = null
    private val model by viewModels<StatsViewModel> {
        StatsViewModel.Factory(
            ConnectionSettings.getHost(requireContext()),
            requireActivity().application
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentStatsBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as AppCompatActivity).supportActionBar?.apply {
            setTitle(R.string.title_stats)
            setDisplayHomeAsUpEnabled(true)
        }

        val binding = binding!!

        // Drawing behind navigation bar on Android 10+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.chart) { v, insets ->
                v.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = insets.systemWindowInsetBottom
                }
                insets
            }
        }

        val textColor = MaterialColors.getColor(binding.chart, R.attr.colorOnBackground)
        binding.chart.apply {
            isGone = true
            setPinchZoom(false)
            description.isEnabled = false
            legend.textColor = textColor
            axisLeft.textColor = textColor
            axisRight.textColor = textColor
            xAxis.textColor = textColor
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val epochTime = value.toLong()
                    return SimpleDateFormat.getTimeInstance().format(Date(epochTime * 1000))
                }
            }
        }
        binding.textViewStatsEmpty.apply {
            setText(R.string.stats_empty)
            isGone = false
        }
        binding.buttonStatsRetry.apply {
            isGone = true
            setOnClickListener {
                it.isEnabled = false
                binding.textViewStatsEmpty.setText(R.string.stats_empty)
                model.loadChartData()
            }
        }

        model.chartData.observe(viewLifecycleOwner) { result ->
            val chartData = result.chartData
            if (chartData != null) {
                chartData.dataSets.forEach { it.valueTextColor = textColor }
                binding.chart.data = chartData
                binding.chart.isGone = false
                binding.chart.invalidate()
                binding.textViewStatsEmpty.isGone = true
                binding.buttonStatsRetry.isGone = true
            } else {
                binding.chart.isGone = true
                binding.textViewStatsEmpty.text = result.errorMessage
                binding.textViewStatsEmpty.isGone = false
                binding.buttonStatsRetry.isEnabled = true
                binding.buttonStatsRetry.isGone = false
            }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_stats, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_action_stats_info -> {
                        showInfoDialog()
                        true
                    }
                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun showInfoDialog() {
        val dtaUrl = ConnectionSettings.getHost(requireContext())
            .let { if (it.isNullOrEmpty()) null else DtaFileReader().getUrl(it) }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_info)
            .setMessage(getString(R.string.stats_info, dtaUrl ?: getString(R.string.setup_missing)))
            .setNegativeButton(R.string.stats_visit_opendta) { _, _ ->
                openWebPage(requireContext(), requireContext().getString(R.string.url_opendta))
            }
            .setNeutralButton(R.string.action_close, null)
        if (dtaUrl != null) {
            builder.setPositiveButton(R.string.stats_get_dtafile) { _, _ ->
                openWebPage(requireContext(), dtaUrl)
            }
        }
        builder.show()
    }

}