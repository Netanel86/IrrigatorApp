package com.netanel.irrigator_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.netanel.irrigator_app.model.Valve;
import com.netanel.irrigator_app.services.AppServices;


public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, ManualFragContract.IView {
    //for testing
    private Button mAddValveButton;
    private Button mCmndButton;


    private static final String TEMP_SYMBOL = "\u2103";
    private RadioGroup mValveRadioGroup;
    private SensorView mSensorViewTemp;
    private SensorView mSensorViewHumid;
    private SensorView mSensorViewFlow;


    ManualFragContract.IViewModel mManualViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        initTestUI();

        mManualViewModel = new ViewModelProvider(this).get(ManualFragViewModel.class);
        mManualViewModel.bindView(this);
        mManualViewModel.onCreate();
    }

    private void initUI() {
        mValveRadioGroup = findViewById(R.id.radioGroup_valves);
        mSensorViewTemp = findViewById(R.id.sensor_view_temp);
        mSensorViewHumid = findViewById(R.id.sensor_view_humid);
        mSensorViewFlow = findViewById(R.id.sensor_view_flow);
    }

    private void initTestUI() {
        mAddValveButton = findViewById(R.id.btn_add);
        mCmndButton = findViewById(R.id.btn_command);

        mAddValveButton.setOnClickListener(this);
        mCmndButton.setOnClickListener(this);
        mCmndButton.setText("Send Command");

        mSensorViewTemp.setValue("25 " + TEMP_SYMBOL);
        mSensorViewHumid.setValue("60" + "%");
        mSensorViewFlow.setValue("1.2" + "L/s");
    }

    private RadioButton initValveRadioButton(boolean valveState, String btnText) {
        final StateRadioButton btnValve =
                new StateRadioButton(
                        new ContextThemeWrapper(this, R.style.ManualFrag_StateRadioButton),
                        null, 0);

        RadioGroup.LayoutParams layoutParams =
                new RadioGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1.0f);

        btnValve.setLayoutParams(layoutParams);
        btnValve.setId(View.generateViewId());
        btnValve.setOnClickListener(this);

        btnValve.setText(btnText);
        btnValve.setState(valveState);

        return btnValve;
    }

    @Override
    public void onClick(View view) {

        if (view instanceof StateRadioButton) {
            mManualViewModel.onStateRadioButtonClicked(view.getId());
        }

        switch (view.getId()) {
            case R.id.btn_command:
                mManualViewModel.onButtonCommandClicked();
                break;
            case R.id.btn_add:
                mManualViewModel.onButtonAddClicked();
                break;
        }
    }

    @Override
    public int addStateRadioButton(boolean valveState, String viewString) {
        RadioButton btnValve = initValveRadioButton(valveState, viewString);
        mValveRadioGroup.addView(btnValve);

        return btnValve.getId();
    }

    @Override
    public void updateStateRadioButton(int btnId, boolean newState) {
        StateRadioButton btnValve = findViewById(btnId);
        btnValve.setState(newState);
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    // TODO: 06/10/2020 save field on instance state changed
    String lastShownValveId = null;

    @Override
    public void showValvePage(String name, boolean state, int duration, Valve valve) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment lastShownFragment = fragmentManager.findFragmentByTag(lastShownValveId);
        Fragment currFragment;
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if(lastShownFragment != null) {
            transaction.hide(lastShownFragment);
        }
        lastShownValveId = valve.getId();

        if ((currFragment = fragmentManager.findFragmentByTag(valve.getId())) != null) {
            transaction.show(currFragment).commit();
        } else {
            currFragment = initNewValveFragment(valve);
            transaction.add(R.id.fragment_container_view, currFragment, valve.getId()).commit();
        }
    }

    private Fragment initNewValveFragment(Valve valve) {
        ValveFragment newFragment = ValveFragment.newInstance();
        String valveJson = AppServices.getInstance().getJsonParser().toJson(valve);

        Bundle bundle = new Bundle();
        bundle.putString(ValveFragment.BUNDLE_VALVE, valveJson);
        newFragment.setArguments(bundle);
        return newFragment;
    }
}
