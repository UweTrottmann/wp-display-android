package com.uwetrottmann.wpdisplay.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.uwetrottmann.wpdisplay.util.ConnectionTools;

public class ConnectionFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ConnectionTools.get(getActivity()).connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ConnectionTools.get(getActivity()).disconnect();
    }
}
