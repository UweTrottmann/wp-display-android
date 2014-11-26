package com.uwetrottmann.wpdisplay.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.uwetrottmann.wpdisplay.R;
import com.uwetrottmann.wpdisplay.model.StatusData;
import com.uwetrottmann.wpdisplay.util.ConnectionTools;
import com.uwetrottmann.wpdisplay.util.DataRequestRunnable;
import de.greenrobot.event.EventBus;
import java.text.DateFormat;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class DisplayFragment extends Fragment {

    @InjectView(R.id.buttonDisplayPause) Button buttonPause;
    @InjectView(R.id.textViewDisplayStatus) TextView textStatus;
    @InjectView(R.id.textViewDisplayTempOutgoing) TextView textTempOutgoing;
    @InjectView(R.id.textViewDisplayTempReturn) TextView textTempReturn;
    @InjectView(R.id.textViewDisplayTempOutdoors) TextView textTempOutdoors;
    @InjectView(R.id.textViewDisplayTempReturnShould) TextView textTempReturnShould;
    @InjectView(R.id.textViewDisplayTempWater) TextView textTempWater;
    @InjectView(R.id.textViewDisplayTempWaterShould) TextView textTempWaterShould;
    @InjectView(R.id.textViewDisplayTempSourceIn) TextView textTempSourceIn;
    @InjectView(R.id.textViewDisplayTempSourceOut) TextView textTempSourceOut;
    @InjectView(R.id.textViewDisplayTimeActive) TextView textTimeActive;
    @InjectView(R.id.textViewDisplayTimeInactive) TextView textTimeInactive;
    @InjectView(R.id.textViewDisplayTime) TextView textTime;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_display, container, false);
        ButterKnife.inject(this, v);

        buttonPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConnectionTools.get().isPaused()) {
                    ConnectionTools.get().pause(false);
                    ConnectionTools.get().requestStatusData();
                    buttonPause.setText(R.string.action_pause);
                } else {
                    ConnectionTools.get().pause(true);
                    buttonPause.setText(R.string.action_resume);
                }
            }
        });
        buttonPause.setText(ConnectionTools.get().isPaused() ? R.string.action_resume
                : R.string.action_pause);
        buttonPause.setEnabled(false);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.title_display);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().registerSticky(this);
        ConnectionTools.get().connect(getActivity());
        ConnectionTools.get().requestStatusData();
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

        ButterKnife.reset(this);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(ConnectionTools.ConnectionEvent event) {
        if (!isAdded()) {
            return;
        }

        buttonPause.setEnabled(event.isConnected);

        // status text
        int statusResId;
        if (event.isConnecting) {
            statusResId = R.string.label_connecting;
        } else if (event.isConnected) {
            statusResId = R.string.label_connected;
        } else {
            statusResId = R.string.label_connection_error;
        }
        textStatus.setText(getString(statusResId, event.host + ":" + event.port));
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(DataRequestRunnable.DataEvent event) {
        if (!isAdded()) {
            return;
        }

        setTemperature(textTempOutgoing, R.string.label_temp_outgoing,
                event.data.getTemperature(StatusData.Temperature.OUTGOING));
        setTemperature(textTempReturn, R.string.label_temp_return,
                event.data.getTemperature(StatusData.Temperature.RETURN));
        setTemperature(textTempOutdoors, R.string.label_temp_outdoors,
                event.data.getTemperature(StatusData.Temperature.OUTDOORS));
        setTemperature(textTempReturnShould, R.string.label_temp_return_should,
                event.data.getTemperature(StatusData.Temperature.RETURN_SHOULD));
        setTemperature(textTempWater, R.string.label_temp_water,
                event.data.getTemperature(StatusData.Temperature.WATER));
        setTemperature(textTempWaterShould, R.string.label_temp_water_should,
                event.data.getTemperature(StatusData.Temperature.WATER_SHOULD));
        setTemperature(textTempSourceIn, R.string.label_temp_source_in,
                event.data.getTemperature(StatusData.Temperature.SOURCE_IN));
        setTemperature(textTempSourceOut, R.string.label_temp_source_out,
                event.data.getTemperature(StatusData.Temperature.SOURCE_OUT));

        setTime(textTimeActive, R.string.label_time_pump_active,
                event.data.getTime(StatusData.Time.TIME_PUMP_ACTIVE));
        setTime(textTimeInactive, R.string.label_time_compressor_inactive,
                event.data.getTime(StatusData.Time.TIME_COMPRESSOR_NOOP));


        textTime.setText(DateFormat.getDateTimeInstance().format(event.data.getTimestamp()));

        // request new data
        ConnectionTools.get().requestStatusDataDelayed();
    }

    private void setTemperature(TextView view, int labelResId, double value) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        builder.append(String.format(Locale.getDefault(), "%.1f", value));
        builder.setSpan(new TextAppearanceSpan(getActivity(),
                        R.style.TextAppearance_AppCompat_Display3), 0, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int lengthOld = builder.length();
        builder.append(getString(R.string.unit_celsius));
        builder.setSpan(new TextAppearanceSpan(getActivity(),
                        R.style.TextAppearance_AppCompat_Headline), lengthOld, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        builder.append("\n");

        lengthOld = builder.length();
        builder.append(getString(labelResId));
        builder.setSpan(new TextAppearanceSpan(getActivity(),
                        R.style.TextAppearance_AppCompat_Caption), lengthOld, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        view.setText(builder);
    }

    private void setTime(TextView view, int labelResId, String value) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        builder.append(value);
        builder.setSpan(new TextAppearanceSpan(getActivity(),
                        R.style.TextAppearance_AppCompat_Display1), 0, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        builder.append("\n");

        int lengthOld = builder.length();
        builder.append(getString(labelResId));
        builder.setSpan(new TextAppearanceSpan(getActivity(),
                        R.style.TextAppearance_AppCompat_Caption), lengthOld, builder.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        view.setText(builder);
    }
}
