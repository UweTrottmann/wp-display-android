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

package com.uwetrottmann.wpdisplay.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.uwetrottmann.wpdisplay.R;
import com.uwetrottmann.wpdisplay.model.StatusData;
import com.uwetrottmann.wpdisplay.settings.ConnectionSettings;
import com.uwetrottmann.wpdisplay.util.ConnectionTools;
import com.uwetrottmann.wpdisplay.util.DataRequestRunnable;
import de.greenrobot.event.EventBus;
import java.text.DateFormat;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class DisplayFragment extends Fragment {

    @BindView(R.id.containerDisplaySnackbar) View snackBar;
    @BindView(R.id.textViewDisplaySnackbar) TextView snackBarText;
    @BindView(R.id.buttonDisplaySnackbar) Button snackBarButton;

    @BindView(R.id.textViewDisplayStatus) TextView textStatus;
    @BindView(R.id.textViewDisplayTempOutgoing) TextView textTempOutgoing;
    @BindView(R.id.textViewDisplayTempReturn) TextView textTempReturn;
    @BindView(R.id.textViewDisplayTempOutdoors) TextView textTempOutdoors;
    @BindView(R.id.textViewDisplayTempReturnShould) TextView textTempReturnShould;
    @BindView(R.id.textViewDisplayTempWater) TextView textTempWater;
    @BindView(R.id.textViewDisplayTempWaterShould) TextView textTempWaterShould;
    @BindView(R.id.textViewDisplayTempSourceIn) TextView textTempSourceIn;
    @BindView(R.id.textViewDisplayTempSourceOut) TextView textTempSourceOut;
    @BindView(R.id.textViewDisplayTimeActive) TextView textTimeActive;
    @BindView(R.id.textViewDisplayTimeInactive) TextView textTimeInactive;
    @BindView(R.id.textViewDisplayTimeRest) TextView textTimeResting;
    @BindView(R.id.textViewDisplayTimeReturnLower) TextView textTimeReturnLower;
    @BindView(R.id.textViewDisplayTimeReturnHigher) TextView textTimeReturnHigher;
    @BindView(R.id.textViewDisplayTime) TextView textTime;

    @BindView(R.id.textViewDisplayFirmware) TextView textFirmware;
    @BindView(R.id.textViewDisplayState) TextView textState;

    private boolean isConnected;
    private Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_display, container, false);
        unbinder = ButterKnife.bind(this, v);

        // show empty data
        populateViews(new StatusData(new int[StatusData.LENGTH_BYTES]));

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_display);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        showSnackBar(false);
        EventBus.getDefault().registerSticky(this);
        connectOrNotify();
    }

    private void connectOrNotify() {
        String host = ConnectionSettings.getHost(getActivity());
        int port = ConnectionSettings.getPort(getActivity());
        if (TextUtils.isEmpty(host) || port < 0 || port > 65535) {
            setupSnackBar(R.string.setup_missing, R.string.action_setup,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            EventBus.getDefault()
                                    .post(new NavigationDrawerFragment.NavigationRequest(
                                            NavigationDrawerFragment.POSITION_SETTINGS));
                        }
                    });
            showSnackBar(true);
        } else {
            ConnectionTools.get().connect(getActivity());
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        ConnectionTools.get().disconnect();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_display, menu);

        boolean paused = ConnectionTools.get().isPaused();
        MenuItem item = menu.findItem(R.id.menu_action_display_pause);
        item.setIcon(paused ? R.drawable.ic_play_arrow_white_24dp : R.drawable.ic_pause_white_24dp);
        item.setTitle(paused ? R.string.action_resume : R.string.action_pause);

        item.setEnabled(isConnected);
        item.setVisible(isConnected);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_display_pause:
                togglePause();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void togglePause() {
        if (ConnectionTools.get().isPaused()) {
            ConnectionTools.get().resume();
        } else {
            ConnectionTools.get().pause();
        }
        getActivity().supportInvalidateOptionsMenu();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(ConnectionTools.ConnectionEvent event) {
        if (!isAdded()) {
            return;
        }

        // pause button
        isConnected = event.isConnected;
        getActivity().supportInvalidateOptionsMenu();

        // status text
        int statusResId;
        if (event.isConnecting) {
            statusResId = R.string.label_connecting;
        } else if (event.isConnected) {
            statusResId = R.string.label_connected;
            // start requesting data
            ConnectionTools.get().requestStatusData(true);
        } else {
            statusResId = R.string.label_connection_error;
            setupSnackBar(R.string.message_no_connection, R.string.action_retry,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ConnectionTools.get().connect(getActivity());
                            showSnackBar(false);
                        }
                    });
            showSnackBar(true);
            ConnectionTools.get().disconnect();
        }

        if (TextUtils.isEmpty(event.host) || event.port < 1) {
            // display generic connection error if host or port not sent
            textStatus.setText(getString(R.string.message_no_connection));
        } else {
            textStatus.setText(getString(statusResId, event.host + ":" + event.port));
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(DataRequestRunnable.DataEvent event) {
        if (!isAdded()) {
            return;
        }

        populateViews(event.data);
    }

    private void populateViews(StatusData data) {
        // temperature values
        setTemperature(textTempOutgoing, R.string.label_temp_outgoing,
                data.getTemperature(StatusData.Temperature.OUTGOING));
        setTemperature(textTempReturn, R.string.label_temp_return,
                data.getTemperature(StatusData.Temperature.RETURN));
        setTemperature(textTempOutdoors, R.string.label_temp_outdoors,
                data.getTemperature(StatusData.Temperature.OUTDOORS));
        setTemperature(textTempReturnShould, R.string.label_temp_return_should,
                data.getTemperature(StatusData.Temperature.RETURN_SHOULD));
        setTemperature(textTempWater, R.string.label_temp_water,
                data.getTemperature(StatusData.Temperature.WATER));
        setTemperature(textTempWaterShould, R.string.label_temp_water_should,
                data.getTemperature(StatusData.Temperature.WATER_SHOULD));
        setTemperature(textTempSourceIn, R.string.label_temp_source_in,
                data.getTemperature(StatusData.Temperature.SOURCE_IN));
        setTemperature(textTempSourceOut, R.string.label_temp_source_out,
                data.getTemperature(StatusData.Temperature.SOURCE_OUT));

        // time values
        setText(textTimeActive, R.string.label_time_pump_active,
                data.getTime(StatusData.Time.TIME_PUMP_ACTIVE));
        setText(textTimeInactive, R.string.label_time_compressor_inactive,
                data.getTime(StatusData.Time.TIME_COMPRESSOR_NOOP));
        setText(textTimeResting, R.string.label_time_rest,
                data.getTime(StatusData.Time.TIME_REST));
        setText(textTimeReturnLower, R.string.label_time_return_lower,
                data.getTime(StatusData.Time.TIME_RETURN_LOWER));
        setText(textTimeReturnHigher, R.string.label_time_return_higher,
                data.getTime(StatusData.Time.TIME_RETURN_HIGHER));

        // text values
        setText(textState, R.string.label_operating_state,
                getContext().getString(data.getOperatingState()));
        setText(textFirmware, R.string.label_firmware, data.getFirmwareVersion());

        textTime.setText(DateFormat.getDateTimeInstance().format(data.getTimestamp()));
    }

    private void setTemperature(TextView view, int labelResId, double value) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        builder.append(getString(labelResId));
        builder.setSpan(new TextAppearanceSpan(getActivity(),
                R.style.TextAppearance_AppCompat_Caption), 0, builder.length(), 0);

        builder.append("\n");

        int lengthOld = builder.length();
        builder.append(String.format(Locale.getDefault(), "%.1f", value));
        builder.setSpan(new TextAppearanceSpan(getActivity(),
                R.style.TextAppearance_AppCompat_Display3), lengthOld, builder.length(), 0);

        lengthOld = builder.length();
        builder.append(getString(R.string.unit_celsius));
        builder.setSpan(new TextAppearanceSpan(getActivity(),
                R.style.TextAppearance_App_Unit), lengthOld, builder.length(), 0);

        view.setText(builder);
    }

    private void setText(TextView view, int labelResId, String value) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        builder.append(getString(labelResId));
        builder.setSpan(new TextAppearanceSpan(getActivity(),
                R.style.TextAppearance_AppCompat_Caption), 0, builder.length(), 0);

        builder.append("\n");

        int lengthOld = builder.length();
        builder.append(value);
        builder.setSpan(new TextAppearanceSpan(getActivity(),
                R.style.TextAppearance_AppCompat_Display1), lengthOld, builder.length(), 0);

        view.setText(builder);
    }

    private void showSnackBar(boolean visible) {
        snackBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setupSnackBar(int titleResId, int actionResId, View.OnClickListener action) {
        snackBarText.setText(titleResId);
        if (actionResId > 0 && action != null) {
            snackBarButton.setText(actionResId);
            snackBarButton.setOnClickListener(action);
            snackBarButton.setVisibility(View.VISIBLE);
        } else {
            snackBarButton.setVisibility(View.GONE);
        }
    }
}
