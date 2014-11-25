package com.uwetrottmann.wpdisplay.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.uwetrottmann.wpdisplay.util.ConnectionTools;

public class ConnectionFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        ConnectionTools.get().connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ConnectionTools.get().disconnect();
    }
}
