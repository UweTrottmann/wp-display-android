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
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.uwetrottmann.wpdisplay.R;
import com.uwetrottmann.wpdisplay.settings.ConnectionSettings;

/**
 * App settings.
 */
public class SettingsFragment extends Fragment {

    @InjectView(R.id.editTextSettingsHost) EditText editTextHost;
    @InjectView(R.id.editTextSettingsPort) EditText editTextPort;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.inject(this, v);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.title_settings);
    }

    @Override
    public void onResume() {
        super.onResume();

        populateViews();
    }

    @Override
    public void onPause() {
        super.onPause();

        saveSettings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    private void populateViews() {
        editTextHost.setText(ConnectionSettings.getHost(getActivity()));
        editTextPort.setText(String.valueOf(ConnectionSettings.getPort(getActivity())));
    }

    private void saveSettings() {
        String host = editTextHost.getText().toString();
        int port = Integer.valueOf(editTextPort.getText().toString());
        ConnectionSettings.saveConnectionSettings(getActivity(), host, port);
    }
}
