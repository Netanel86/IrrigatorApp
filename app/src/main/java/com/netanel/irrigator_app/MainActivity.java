package com.netanel.irrigator_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.devadvance.circularseekbar.CircularSeekBar;


public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, ManualFragContract.IView, CircularSeekBar.OnCircularSeekBarChangeListener {
    //for testing
    private Button mAddValveButton;
    private Button mCmndButton;


    private static final String TEMP_SYMBOL = "\u2103";
    private RadioGroup mValveRadioGroup;
    private SensorView mSensorViewTemp;
    private SensorView mSensorViewHumid;
    private SensorView mSensorViewFlow;
    private TextView mTvValveName;
    private TimeSetSeekBar mTimeSetSeekBar;



    ManualFragContract.IViewModel mManualViewModel;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        initTestUI();

        mManualViewModel = ViewModelProviders.of(this).get(ManualFragViewModel.class);
        mManualViewModel.bindView(this);
        mManualViewModel.onCreate();
    }

    private void initUI() {
        mValveRadioGroup = findViewById(R.id.radioGroup_valves);
        mTimeSetSeekBar = findViewById(R.id.csb_time);
        mSensorViewTemp = findViewById(R.id.sensor_view_temp);
        mSensorViewHumid = findViewById(R.id.sensor_view_humid);
        mSensorViewFlow = findViewById(R.id.sensor_view_flow);
        mTvValveName = findViewById(R.id.textView_name);

        mTimeSetSeekBar.setOnChangedListener(this);
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
                        new ContextThemeWrapper(this, R.style.ValveRadioButton),
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


    @Override
    public void showValvePage(String name, boolean state, int duration) {
        mTimeSetSeekBar.setProgress(duration);
        mTvValveName.setText(name);
    }

    @Override
    public void setSeekBarProgress(long progress) {
        mTimeSetSeekBar.setProgress((int)progress);
    }

    @Override
    public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
        mManualViewModel.onSeekBarProgressChanged(progress, fromUser);
    }

    @Override
    public void onStopTrackingTouch(CircularSeekBar seekBar) {

    }

    @Override
    public void onStartTrackingTouch(CircularSeekBar seekBar) {

    }
}
