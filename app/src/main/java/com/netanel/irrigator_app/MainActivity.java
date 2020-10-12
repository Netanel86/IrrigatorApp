package com.netanel.irrigator_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;


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

    ManualFragContract.IPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        initTestUI();

        ManualFragRouter router = new ManualFragRouter(this);
        ViewModelFactory factory = new ViewModelFactory(router);

        mPresenter = new ViewModelProvider(this, factory).get(ManualFragPresenter.class);
        mPresenter.bindView(this);
        mPresenter.onCreate();
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
            mPresenter.onStateRadioButtonClicked(view.getId());
        }

        switch (view.getId()) {
            case R.id.btn_command:
                mPresenter.onButtonCommandClicked();
                break;
            case R.id.btn_add:
                mPresenter.onButtonAddClicked();
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
}
