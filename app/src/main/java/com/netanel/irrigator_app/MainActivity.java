package com.netanel.irrigator_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import com.netanel.irrigator_app.services.AppServices;


public class MainActivity extends AppCompatActivity {

    private static final String TEMP_SYMBOL = "\u2103";

    private SensorView mSensorViewTemp;
    private SensorView mSensorViewHumid;
    private SensorView mSensorViewFlow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        initTestUI();

        AppServices.getInstance()
                .setViewModelFactory(new ViewModelProvider.AndroidViewModelFactory(getApplication()));
        getSupportFragmentManager()
                .beginTransaction().add(R.id.fragment_container_view,new ManualFragment()).commit();

//        ManualFragRouter router = new ManualFragRouter(this);
    }

    private void initUI() {
        mSensorViewTemp = findViewById(R.id.sensor_view_temp);
        mSensorViewHumid = findViewById(R.id.sensor_view_humid);
        mSensorViewFlow = findViewById(R.id.sensor_view_flow);
    }

    private void initTestUI() {
        mSensorViewTemp.setValue("25 " + TEMP_SYMBOL);
        mSensorViewHumid.setValue("60" + "%");
        mSensorViewFlow.setValue("1.2" + "L/s");
    }
}
