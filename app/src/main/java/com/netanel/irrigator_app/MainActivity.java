package com.netanel.irrigator_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import com.netanel.irrigator_app.services.AppServices;


public class MainActivity extends AppCompatActivity {
    private static final String TAG_FRAGMENT_MANUAL = "manual";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setElevation(0);

        initUI();

        if(savedInstanceState != null) {
            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().show(manager.findFragmentByTag(TAG_FRAGMENT_MANUAL));
        } else {
            AppServices.getInstance()
                    .setViewModelFactory(new ViewModelProvider.AndroidViewModelFactory(getApplication()));

            getSupportFragmentManager()
                    .beginTransaction().add(R.id.fragment_container_view, new ManualFragment(), TAG_FRAGMENT_MANUAL).commit();
        }
    }

    private void initUI() {

    }
}
