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
import java.util.Date;
import java.util.Locale;
import org.apache.http.impl.cookie.DateUtils;

/**
 * A simple {@link Fragment} subclass.
 */
public class DisplayFragment extends Fragment {

    @InjectView(R.id.buttonDisplayPause) Button buttonPause;
    @InjectView(R.id.textViewDisplayStatus) TextView textStatus;
    @InjectView(R.id.textViewDisplayTemperature) TextView textTemperature;
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
        buttonPause.setText(
                ConnectionTools.get().isPaused() ? R.string.action_resume : R.string.action_pause);

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
        ConnectionTools.get().requestStatusData();
    }

    @Override
    public void onStop() {
        super.onStop();

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

        textStatus.setText(event.isConnected ? "Connected at " + DateUtils.formatDate(new Date())
                : "Disconnected at " + DateUtils.formatDate(new Date()));
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(DataRequestRunnable.DataEvent event) {
        if (!isAdded()) {
            return;
        }

        setTemperature(textTemperature, R.string.label_temp_outdoors,
                event.data.getTemperature(StatusData.Temperature.OUTDOORS));
        textTime.setText(DateUtils.formatDate(event.data.getTimestamp()));

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
}
