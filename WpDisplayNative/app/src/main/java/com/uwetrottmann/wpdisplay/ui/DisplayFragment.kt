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
import android.support.v4.widget.TextViewCompat
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.*
import android.widget.Button
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.uwetrottmann.wpdisplay.R
import com.uwetrottmann.wpdisplay.model.StatusData
import com.uwetrottmann.wpdisplay.settings.ConnectionSettings
import com.uwetrottmann.wpdisplay.util.ConnectionTools
import com.uwetrottmann.wpdisplay.util.DataRequestRunnable
import de.greenrobot.event.EventBus
import java.text.DateFormat
import java.util.*

class DisplayFragment : Fragment() {

    @BindView(R.id.containerDisplaySnackbar) lateinit var snackBar: View
    @BindView(R.id.textViewDisplaySnackbar) lateinit var snackBarText: TextView
    @BindView(R.id.buttonDisplaySnackbar) lateinit var snackBarButton: Button

    @BindView(R.id.textViewDisplayStatus) lateinit var textStatus: TextView
    @BindView(R.id.textViewDisplayTempOutgoing) lateinit var textTempOutgoing: TextView
    @BindView(R.id.textViewDisplayTempReturn) lateinit var textTempReturn: TextView
    @BindView(R.id.textViewDisplayTempOutdoors) lateinit var textTempOutdoors: TextView
    @BindView(R.id.textViewDisplayTempReturnShould) lateinit var textTempReturnShould: TextView
    @BindView(R.id.textViewDisplayTempOutdoorsAvg) lateinit var textTempOutdoorsAvg: TextView
    @BindView(R.id.textViewDisplayTempHotGas) lateinit var textTempHotGas: TextView
    @BindView(R.id.textViewDisplayTempWater) lateinit var textTempWater: TextView
    @BindView(R.id.textViewDisplayTempWaterShould) lateinit var textTempWaterShould: TextView
    @BindView(R.id.textViewDisplayTempSourceIn) lateinit var textTempSourceIn: TextView
    @BindView(R.id.textViewDisplayTempSourceOut) lateinit var textTempSourceOut: TextView
    @BindView(R.id.textViewDisplayTimeActive) lateinit var textTimeActive: TextView
    @BindView(R.id.textViewDisplayTimeInactive) lateinit var textTimeInactive: TextView
    @BindView(R.id.textViewDisplayTimeRest) lateinit var textTimeResting: TextView
    @BindView(R.id.textViewDisplayTimeReturnLower) lateinit var textTimeReturnLower: TextView
    @BindView(R.id.textViewDisplayTimeReturnHigher) lateinit var textTimeReturnHigher: TextView
    @BindView(R.id.textViewDisplayTime) lateinit var textTime: TextView

    @BindView(R.id.textViewDisplayFirmware) lateinit var textFirmware: TextView
    @BindView(R.id.textViewDisplayState) lateinit var textState: TextView

    private var isConnected: Boolean = false
    private lateinit var unbinder: Unbinder
    private lateinit var selectableViews: MutableList<TextView>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_display, container, false)
        unbinder = ButterKnife.bind(this, v)
        selectableViews = LinkedList()
        selectableViews.add(textTempOutgoing)
        selectableViews.add(textTempReturn)
        selectableViews.add(textTempOutdoors)
        selectableViews.add(textTempReturnShould)
        selectableViews.add(textTempOutdoorsAvg)
        selectableViews.add(textTempHotGas)
        selectableViews.add(textTempWater)
        selectableViews.add(textTempWaterShould)
        selectableViews.add(textTempSourceIn)
        selectableViews.add(textTempSourceOut)
        selectableViews.add(textTimeActive)
        selectableViews.add(textTimeInactive)
        selectableViews.add(textTimeResting)
        selectableViews.add(textTimeReturnLower)
        selectableViews.add(textTimeReturnHigher)
        selectableViews.add(textTime)

        // show empty data
        setTextSelectable(ConnectionTools.get().isPaused)
        populateViews(StatusData(IntArray(StatusData.LENGTH_BYTES)))

        return v
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
        EventBus.getDefault().registerSticky(this)
        connectOrNotify()
    }

    private fun connectOrNotify() {
        val host = ConnectionSettings.getHost(activity)
        val port = ConnectionSettings.getPort(activity)
        if (TextUtils.isEmpty(host) || port < 0 || port > 65535) {
            setupSnackBar(R.string.setup_missing, R.string.action_setup,
                    View.OnClickListener { showSettingsFragment() })
            showSnackBar(true)
        } else {
            ConnectionTools.get().connect(activity)
        }
    }

    override fun onStop() {
        super.onStop()

        ConnectionTools.get().disconnect()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        unbinder.unbind()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_display, menu)

        val paused = ConnectionTools.get().isPaused
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
        fragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .replace(R.id.container, SettingsFragment())
                .addToBackStack(null)
                .commit()
    }

    private fun togglePause() {
        if (ConnectionTools.get().isPaused) {
            ConnectionTools.get().resume()
            setTextSelectable(false)
        } else {
            ConnectionTools.get().pause()
            setTextSelectable(true)
        }
        activity.invalidateOptionsMenu()
    }

    /**
     * Only enable text selection if views are not updating. Otherwise scroll state resets.
     */
    private fun setTextSelectable(selectable: Boolean) {
        for (textView in selectableViews) {
            textView.setTextIsSelectable(selectable)
        }
    }

    @Suppress("unused")
    fun onEventMainThread(event: ConnectionTools.ConnectionEvent) {
        if (!isAdded) {
            return
        }

        // pause button
        isConnected = event.isConnected
        activity.invalidateOptionsMenu()

        // status text
        val statusResId: Int
        var isWarning = false
        when {
            event.isConnecting -> statusResId = R.string.label_connecting
            event.isConnected -> {
                statusResId = R.string.label_connected
                // start requesting data
                ConnectionTools.get().requestStatusData(true)
            }
            else -> {
                isWarning = true
                statusResId = R.string.label_connection_error
                setupSnackBar(R.string.message_no_connection, R.string.action_retry,
                        View.OnClickListener {
                            ConnectionTools.get().connect(activity)
                            showSnackBar(false)
                        })
                showSnackBar(true)
                ConnectionTools.get().disconnect()
            }
        }

        if (TextUtils.isEmpty(event.host) || event.port < 1) {
            // display generic connection error if host or port not sent
            textStatus.text = getString(R.string.message_no_connection)
        } else {
            textStatus.text = getString(statusResId, event.host + ":" + event.port)
        }
        TextViewCompat.setTextAppearance(textStatus,
                if (isWarning)
                    R.style.TextAppearance_App_Body1_Orange
                else
                    R.style.TextAppearance_App_Body1_Green)
    }

    @Suppress("unused")
    fun onEventMainThread(event: DataRequestRunnable.DataEvent) {
        if (!isAdded) {
            return
        }

        populateViews(event.data)
    }

    private fun populateViews(data: StatusData) {
        // temperature values
        setTemperature(textTempOutgoing, R.string.label_temp_outgoing,
                data.getTemperature(StatusData.Temperature.OUTGOING))
        setTemperature(textTempReturn, R.string.label_temp_return,
                data.getTemperature(StatusData.Temperature.RETURN))
        setTemperature(textTempOutdoors, R.string.label_temp_outdoors,
                data.getTemperature(StatusData.Temperature.OUTDOORS))
        setTemperature(textTempReturnShould, R.string.label_temp_return_should,
                data.getTemperature(StatusData.Temperature.RETURN_SHOULD))
        setTemperature(textTempOutdoorsAvg, R.string.label_temp_outdoors_average,
                data.getTemperature(StatusData.Temperature.OUTDOORS_AVERAGE))
        setTemperature(textTempHotGas, R.string.label_temp_hot_gas,
                data.getTemperature(StatusData.Temperature.HOT_GAS))
        setTemperature(textTempWater, R.string.label_temp_water,
                data.getTemperature(StatusData.Temperature.WATER))
        setTemperature(textTempWaterShould, R.string.label_temp_water_should,
                data.getTemperature(StatusData.Temperature.WATER_SHOULD))
        setTemperature(textTempSourceIn, R.string.label_temp_source_in,
                data.getTemperature(StatusData.Temperature.SOURCE_IN))
        setTemperature(textTempSourceOut, R.string.label_temp_source_out,
                data.getTemperature(StatusData.Temperature.SOURCE_OUT))

        // time values
        setText(textTimeActive, R.string.label_time_pump_active,
                data.getTime(StatusData.Time.TIME_PUMP_ACTIVE))
        setText(textTimeInactive, R.string.label_time_compressor_inactive,
                data.getTime(StatusData.Time.TIME_COMPRESSOR_NOOP))
        setText(textTimeResting, R.string.label_time_rest,
                data.getTime(StatusData.Time.TIME_REST))
        setText(textTimeReturnLower, R.string.label_time_return_lower,
                data.getTime(StatusData.Time.TIME_RETURN_LOWER))
        setText(textTimeReturnHigher, R.string.label_time_return_higher,
                data.getTime(StatusData.Time.TIME_RETURN_HIGHER))

        // text values
        setText(textState, R.string.label_operating_state,
                context.getString(data.operatingState))
        setText(textFirmware, R.string.label_firmware, data.firmwareVersion)

        textTime.text = DateFormat.getDateTimeInstance().format(data.timestamp)
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
        snackBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setupSnackBar(titleResId: Int, actionResId: Int, action: View.OnClickListener) {
        snackBarText.setText(titleResId)
        if (actionResId > 0) {
            snackBarButton.setText(actionResId)
            snackBarButton.setOnClickListener(action)
            snackBarButton.visibility = View.VISIBLE
        } else {
            snackBarButton.visibility = View.GONE
        }
    }
}
