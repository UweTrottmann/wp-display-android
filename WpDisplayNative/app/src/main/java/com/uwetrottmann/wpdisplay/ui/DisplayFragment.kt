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

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.*
import android.widget.TextView
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.display.DisplayAdapter
import com.uwetrottmann.wpdisplay.model.ConnectionStatus
import com.uwetrottmann.wpdisplay.model.StatusData
import com.uwetrottmann.wpdisplay.settings.ConnectionSettings
import com.uwetrottmann.wpdisplay.util.ConnectionTools
import com.uwetrottmann.wpdisplay.util.DataRequestRunnable
import kotlinx.android.synthetic.main.fragment_display_rv.*
import kotlinx.android.synthetic.main.layout_snackbar.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class DisplayFragment : Fragment() {

    private lateinit var viewAdapter: DisplayAdapter
    private lateinit var viewManager: GridLayoutManager

    private var isConnected: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_display_rv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activeItems = MutableList(15) { it + 1 }
        viewAdapter = DisplayAdapter(activeItems)

        // TODO ut: use dimen
        viewManager = GridLayoutManager(requireContext(), 2).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (viewAdapter.getItemViewType(position)) {
                        DisplayAdapter.VIEW_TYPE_HEADER -> viewManager.spanCount
                        DisplayAdapter.VIEW_TYPE_DURATION -> viewManager.spanCount
                        else -> 1
                    }
                }
            }
        }

        recyclerViewDisplay.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        // show empty data
//        setTextSelectable(ConnectionTools.isPaused)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val actionBar = (activity as AppCompatActivity).supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_display)
            actionBar.setDisplayHomeAsUpEnabled(false)
        }

        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()

        showSnackBar(false)
        EventBus.getDefault().register(this)
        connectOrNotify()
    }

    private fun connectOrNotify() {
        val host = ConnectionSettings.getHost(requireContext())
        val port = ConnectionSettings.getPort(requireContext())
        if (TextUtils.isEmpty(host) || port < 0 || port > 65535) {
            setupSnackBar(R.string.setup_missing, R.string.action_setup,
                    View.OnClickListener { showSettingsFragment() })
            showSnackBar(true)
        } else {
            ConnectionTools.connect(requireContext())
        }
    }

    override fun onStop() {
        super.onStop()

        ConnectionTools.disconnect()
        EventBus.getDefault().unregister(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_display, menu)

        val paused = ConnectionTools.isPaused
        val item = menu.findItem(R.id.menu_action_display_pause)
        item.setIcon(if (paused) R.drawable.ic_play_arrow_white_24dp else R.drawable.ic_pause_white_24dp)
        item.setTitle(if (paused) R.string.action_resume else R.string.action_pause)

        item.isEnabled = isConnected
        item.isVisible = isConnected
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_action_display_pause -> {
                togglePause()
                return true
            }
            R.id.menu_action_display_settings -> {
                showSettingsFragment()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showSettingsFragment() {
        requireFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .replace(R.id.container, SettingsFragment())
                .addToBackStack(null)
                .commit()
    }

    private fun togglePause() {
        if (ConnectionTools.isPaused) {
            ConnectionTools.resume()
            setTextSelectable(false)
        } else {
            ConnectionTools.pause()
            setTextSelectable(true)
        }
        requireActivity().invalidateOptionsMenu()
    }

    /**
     * Only enable text selection if views are not updating. Otherwise scroll state resets.
     */
    private fun setTextSelectable(selectable: Boolean) {
        // TODO
//        for (textView in selectableViews) {
//            textView.setTextIsSelectable(selectable)
//        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionTools.ConnectionEvent) {
        if (!isAdded) {
            return
        }

        // pause button
        isConnected = event.isConnected
        requireActivity().invalidateOptionsMenu()

        // status text
        val statusResId: Int
        var isWarning = false
        when {
            event.isConnecting -> statusResId = R.string.label_connecting
            event.isConnected -> {
                statusResId = R.string.label_connected
                // start requesting data
                ConnectionTools.requestStatusData(true)
            }
            else -> {
                isWarning = true
                statusResId = R.string.label_connection_error
                setupSnackBar(R.string.message_no_connection, R.string.action_retry,
                        View.OnClickListener {
                            ConnectionTools.connect(requireContext())
                            showSnackBar(false)
                        })
                showSnackBar(true)
                ConnectionTools.disconnect()
            }
        }

        val message = if (TextUtils.isEmpty(event.host) || event.port < 1) {
            // display generic connection error if host or port not sent
            getString(R.string.message_no_connection)
        } else {
            getString(statusResId, event.host + ":" + event.port)
        }
        viewAdapter.updateStatus(ConnectionStatus(message, isWarning))
    }

    @Suppress("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DataRequestRunnable.DataEvent) {
        if (!isAdded) {
            return
        }

        viewAdapter.updateStatusData(event.data)
    }

    private fun populateViews(data: StatusData) {
        viewAdapter.updateStatusData(data)

        // text values
//        setText(textState, R.string.label_operating_state,
//                requireContext().getString(data.operatingState))
//        setText(textFirmware, R.string.label_firmware, data.firmwareVersion)
//
//        textTime.text = DateFormat.getDateTimeInstance().format(data.timestamp)
    }

    private fun setTemperature(view: TextView?, labelResId: Int, value: Double) {
        val builder = SpannableStringBuilder()

        builder.append(getString(labelResId))
        builder.setSpan(TextAppearanceSpan(activity,
                R.style.TextAppearance_AppCompat_Caption), 0, builder.length, 0)

        builder.append("\n")

        var lengthOld = builder.length
        builder.append(String.format(Locale.getDefault(), "%.1f", value))
        builder.setSpan(TextAppearanceSpan(activity,
                R.style.TextAppearance_AppCompat_Display3), lengthOld, builder.length, 0)

        lengthOld = builder.length
        builder.append(getString(R.string.unit_celsius))
        builder.setSpan(TextAppearanceSpan(activity,
                R.style.TextAppearance_App_Unit), lengthOld, builder.length, 0)

        view!!.text = builder
    }

    private fun setText(view: TextView, labelResId: Int, value: String) {
        val builder = SpannableStringBuilder()

        builder.append(getString(labelResId))
        builder.setSpan(TextAppearanceSpan(activity,
                R.style.TextAppearance_AppCompat_Caption), 0, builder.length, 0)

        builder.append("\n")

        val lengthOld = builder.length
        builder.append(value)
        builder.setSpan(TextAppearanceSpan(activity,
                R.style.TextAppearance_AppCompat_Display1), lengthOld, builder.length, 0)

        view.text = builder
    }

    private fun showSnackBar(visible: Boolean) {
        containerDisplaySnackbar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setupSnackBar(titleResId: Int, actionResId: Int, action: View.OnClickListener) {
        textViewDisplaySnackbar.setText(titleResId)
        if (actionResId > 0) {
            buttonDisplaySnackbar.setText(actionResId)
            buttonDisplaySnackbar.setOnClickListener(action)
            buttonDisplaySnackbar.visibility = View.VISIBLE
        } else {
            buttonDisplaySnackbar.visibility = View.GONE
        }
    }
}
