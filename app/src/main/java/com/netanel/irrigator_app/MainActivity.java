package com.netanel.irrigator_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import com.netanel.irrigator_app.services.AppServices;


public class MainActivity extends AppCompatActivity {

    private static final String TEMP_SYMBOL = "\u2103";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        AppServices.getInstance()
                .setViewModelFactory(new ViewModelProvider.AndroidViewModelFactory(getApplication()));
        getSupportFragmentManager()
                .beginTransaction().add(R.id.fragment_container_view,new ManualFragment()).commit();
        getSupportActionBar().setElevation(0);
//        ManualFragRouter router = new ManualFragRouter(this);
    }

    private void initUI() {

    }
}
