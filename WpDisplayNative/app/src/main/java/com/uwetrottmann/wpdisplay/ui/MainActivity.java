package com.uwetrottmann.wpdisplay.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import com.uwetrottmann.wpdisplay.R;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup action bar
        Toolbar actionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(actionBarToolbar);

        // setup nav drawer
        NavigationDrawerFragment navDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        if (navDrawerFragment == null) {
            navDrawerFragment = new NavigationDrawerFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.navigation_drawer, navDrawerFragment)
                    .commit();
        }
        navDrawerFragment.setUp((DrawerLayout) findViewById(R.id.drawer_layout), actionBarToolbar);

        // setup connection fragment
        ConnectionFragment connectionFragment
                = (ConnectionFragment) getSupportFragmentManager().findFragmentByTag(
                "connection");
        if (connectionFragment == null) {
            connectionFragment = new ConnectionFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(connectionFragment, "connection")
                    .commit();
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Fragment f;
        switch (position) {
            case 0:
                f = new DisplayFragment();
                break;
            default:
                f = new SettingsFragment();
                break;
        }

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, f)
                .commit();
    }
}
