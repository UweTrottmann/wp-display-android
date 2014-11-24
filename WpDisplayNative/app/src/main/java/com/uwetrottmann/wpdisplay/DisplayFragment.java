package com.uwetrottmann.wpdisplay;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.uwetrottmann.wpdisplay.util.ConnectionTools;
import de.greenrobot.event.EventBus;
import java.util.Date;
import org.apache.http.impl.cookie.DateUtils;

/**
 * A simple {@link Fragment} subclass.
 */
public class DisplayFragment extends Fragment {

    @InjectView(R.id.textViewDisplayStatus) TextView textStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_display, container, false);
        ButterKnife.inject(this, v);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().registerSticky(this);
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
        textStatus.setText(event.isConnected ? "Connected at " + DateUtils.formatDate(new Date())
                : "Disconnected at " + DateUtils.formatDate(new Date()));
    }
}
