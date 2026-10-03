// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.uwetrottmann.wpdisplay.display

import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.databinding.FragmentDisplayRvBinding
import com.uwetrottmann.wpdisplay.graph.StatsFragment
import com.uwetrottmann.wpdisplay.model.ConnectionStatus
import com.uwetrottmann.wpdisplay.model.DisplayItems
import com.uwetrottmann.wpdisplay.model.DisplayRow
import com.uwetrottmann.wpdisplay.model.StatusData
import com.uwetrottmann.wpdisplay.settings.ConnectionSettings
import com.uwetrottmann.wpdisplay.settings.SettingsFragment
import com.uwetrottmann.wpdisplay.util.ConnectionTools
import com.uwetrottmann.wpdisplay.util.DataRequestRunnable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

class DisplayFragment : Fragment() {

    private lateinit var viewAdapter: DisplayAdapter
    private lateinit var viewManager: GridLayoutManager

    private var _binding: FragmentDisplayRvBinding? = null
    private val binding get() = _binding!!

    private var isConnected: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisplayRvBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actionBar = (activity as AppCompatActivity).supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_display)
            actionBar.setDisplayHomeAsUpEnabled(false)
        }

        // Drawing behind navigation bar on Android 10+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerViewDisplay) { v, insets ->
                val bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout()
                )
                val basePaddingHorizontal =
                    resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
                val basePaddingVertical =
                    resources.getDimensionPixelSize(R.dimen.activity_vertical_margin)
                v.updatePadding(
                    left = basePaddingHorizontal + bars.left,
                    right = basePaddingHorizontal + bars.right,
                    bottom = basePaddingVertical + bars.bottom,
                )
                insets
            }
            ViewCompat.setOnApplyWindowInsetsListener(binding.messageBanner.root) { v, insets ->
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

        // TODO maybe read state async
        DisplayItems.readDisabledStateFromPreferences(requireContext())
        // Empty text until the first status data is built
        viewAdapter = DisplayAdapter(DisplayItems.enabled.map { DisplayRow(it, "") })

        val spanCount = resources.getInteger(R.integer.spanCount)
        val spanSizeTemperatures = resources.getInteger(R.integer.spanSizeTemperatures)
        val spanSizeDurations = resources.getInteger(R.integer.spanSizeDurations)
        viewManager = GridLayoutManager(requireContext(), spanCount).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (viewAdapter.getItemViewType(position)) {
                        DisplayAdapter.VIEW_TYPE_TEMPERATURE -> spanSizeTemperatures
                        DisplayAdapter.VIEW_TYPE_DURATION -> spanSizeDurations
                        else -> viewManager.spanCount
                    }
                }
            }
        }

        binding.recyclerViewDisplay.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
            (itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        }

        DataRequestRunnable.statusData.observe(viewLifecycleOwner) {
            buildDataAndUpdateAdapter(it)
        }
        ConnectionTools.connectionEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                handleConnectionEvent(it)
            }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_display, menu)

                val paused = ConnectionTools.isPaused
                val item = menu.findItem(R.id.menu_action_display_pause)
                item.setIcon(if (paused) R.drawable.ic_play_arrow_white_24dp else R.drawable.ic_pause_white_24dp)
                item.setTitle(if (paused) R.string.action_resume else R.string.action_pause)

                item.isEnabled = isConnected
                item.isVisible = isConnected
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_action_display_pause -> {
                        togglePause()
                        true
                    }

                    R.id.menu_action_display_stats -> {
                        // Disconnect early to not interfere with loading data file.
                        ConnectionTools.disconnect()
                        showStatsFragment()
                        true
                    }

                    R.id.menu_action_display_settings -> {
                        showSettingsFragment()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onStart() {
        super.onStart()

        showMessageBanner(false)
        connectOrNotify()
    }

    private fun connectOrNotify() {
        val host = ConnectionSettings.getHost(requireContext())
        val port = ConnectionSettings.getPort(requireContext())
        if (TextUtils.isEmpty(host) || port < 0 || port > 65535) {
            setupMessageBanner(
                R.string.setup_missing,
                R.string.action_setup
            ) { showSettingsFragment() }
            showMessageBanner(true)
        } else {
            ConnectionTools.connect(requireContext())
        }
    }

    override fun onStop() {
        super.onStop()

        ConnectionTools.disconnect()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showStatsFragment() {
        parentFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
            .replace(R.id.container, StatsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun showSettingsFragment() {
        parentFragmentManager.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
            .replace(R.id.container, SettingsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun togglePause() {
        if (ConnectionTools.isPaused) {
            ConnectionTools.resume()
        } else {
            ConnectionTools.pause()
        }
        requireActivity().invalidateOptionsMenu()
    }

    private fun handleConnectionEvent(event: ConnectionTools.ConnectionEvent) {
        // pause button
        isConnected = event is ConnectionTools.ConnectedEvent
        requireActivity().invalidateOptionsMenu()

        // status text
        val message: String
        var isWarning = false
        when (event) {
            is ConnectionTools.MissingSettingsEvent -> {
                isWarning = true
                message = getString(R.string.setup_missing)
            }

            is ConnectionTools.ConnectingEvent -> {
                message = getString(R.string.label_connecting, event.host + ":" + event.port)
            }

            is ConnectionTools.ConnectedEvent -> {
                message = getString(R.string.label_connected, event.host + ":" + event.port)
                // start requesting data
                ConnectionTools.requestStatusData(true)
            }

            is ConnectionTools.ConnectionErrorEvent -> {
                isWarning = true
                // Use different message on Android 17 and up, where a missing local network
                // permission can also cause a connection timeout (it won't have a cause).
                // https://developer.android.com/privacy-and-security/local-network-permission#errors
                val messageId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                    R.string.label_no_connection_permission
                } else {
                    R.string.label_no_connection
                }
                // like "Could not connect to 192.168.0.42:8889 (SocketTimeoutException)."
                val optionalCause = event.errorCause?.let { " ($it)" } ?: ""
                message = getString(
                    messageId,
                    "${event.host}:${event.port}$optionalCause"
                )

                setupMessageBanner(R.string.message_connection_error, R.string.action_retry) {
                    ConnectionTools.connect(requireContext())
                    showMessageBanner(false)
                }
                showMessageBanner(true)
                ConnectionTools.disconnect()
            }
        }

        viewAdapter.updateStatus(ConnectionStatus(message, isWarning))
    }

    private var buildJob: Job? = null

    private fun buildDataAndUpdateAdapter(statusData: StatusData) {
        val context = this.requireContext()
        // Only the latest data needs to be displayed
        buildJob?.cancel()
        // Canceled when the view is destroyed
        buildJob = viewLifecycleOwner.lifecycleScope.launch {
            val displayItems = DisplayItems.enabled
            // Only builds new objects, so builds can run in parallel
            val (timestamp, displayRows) = withContext(Dispatchers.Default) {
                val rows = displayItems.map {
                    ensureActive()
                    // need to use theme context!
                    it.toDisplayRow(context, statusData)
                }
                DateFormat.getDateTimeInstance().format(statusData.timestamp) to rows
            }
            viewAdapter.updateDisplayRows(timestamp, displayRows)
        }
    }

    private fun showMessageBanner(visible: Boolean) {
        binding.messageBanner.root.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setupMessageBanner(
        titleResId: Int,
        actionResId: Int,
        action: View.OnClickListener
    ) {
        binding.messageBanner.apply {
            textViewMessage.setText(titleResId)
            buttonAction.apply {
                if (actionResId > 0) {
                    visibility = View.VISIBLE
                    setText(actionResId)
                    setOnClickListener(action)
                } else {
                    visibility = View.GONE
                }
            }
        }
    }
}
