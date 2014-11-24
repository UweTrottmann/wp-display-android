package com.uwetrottmann.wpdisplay;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.uwetrottmann.wpdisplay.util.ConnectionTools;

public class ConnectionFragment extends Fragment {

    private ConnectionTools connectionTools;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        connectionTools = new ConnectionTools();
        connectionTools.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        connectionTools.disconnect();
    }
}
