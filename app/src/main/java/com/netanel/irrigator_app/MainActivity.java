package com.netanel.irrigator_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.devadvance.circularseekbar.CircularSeekBar;

import org.jetbrains.annotations.NotNull;


public class MainActivity extends AppCompatActivity
        implements View.OnClickListener,
        CircularSeekBar.OnCircularSeekBarChangeListener, ManualFragContract.IView {

    private static final String TEMP_SYMBOL = "\u2103";

    private RadioGroup mValveRadioGroup;

    private SensorView mSensorViewTemp;
    private SensorView mSensorViewHumid;
    private SensorView mSensorViewFlow;

    private ViewSwitcher mViewSwitcher;

    private TextView mTvTimer;
    private TextView mTvTitle;
    private ImageView mIvState;
    private CircularSeekBar mSeekBar;
    private TextView mTvMax;
    private TextView mTvQuarter;
    private TextView mTvHalf;
    private TextView mTvThreeQuarter;
    private TextView mTvZero;
    private Button mButtonSet;

    ManualFragContract.IPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        initializeListeners();
        initTestUI();

//        ManualFragRouter router = new ManualFragRouter(this);
        ViewModelFactory factory = new ViewModelFactory();

        mPresenter = new ViewModelProvider(this, factory).get(ManualFragPresenter.class);
        mPresenter.bindView(this);
        mPresenter.onCreate();
    }

    private void initUI() {
        mValveRadioGroup = findViewById(R.id.radioGroup_valves);
        mSensorViewTemp = findViewById(R.id.sensor_view_temp);
        mSensorViewHumid = findViewById(R.id.sensor_view_humid);
        mSensorViewFlow = findViewById(R.id.sensor_view_flow);

        mSeekBar = findViewById(R.id.circular_seekbar);
        mTvTimer = findViewById(R.id.tv_elapsed_time);
        mIvState = findViewById(R.id.iv_valve_state);
        mTvTitle = findViewById(R.id.tv_valve_name);

        mTvZero = findViewById(R.id.tv_time_zero);
        mTvMax = findViewById(R.id.tv_time_max);
        mTvQuarter = findViewById(R.id.tv_time_quarter);
        mTvHalf = findViewById(R.id.tv_time_half);
        mTvThreeQuarter = findViewById(R.id.tv_time_three_quarter);
        mViewSwitcher = findViewById(R.id.view_switcher);

        mButtonSet = findViewById(R.id.btn_set);
    }

    private void initializeListeners() {
        mSeekBar.setOnSeekBarChangeListener(this);
        mTvThreeQuarter.setOnClickListener(this);
        mTvMax.setOnClickListener(this);
        mTvZero.setOnClickListener(this);
        mTvHalf.setOnClickListener(this);
        mTvQuarter.setOnClickListener(this);
        mButtonSet.setOnClickListener(this);
    }

    private void initTestUI() {
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

        int viewId = view.getId();
        if(viewId == R.id.tv_time_zero ) {
            mPresenter.onPredefinedTimeClicked(ManualFragContract.PredefinedTime.Zero);
        } else if (viewId == R.id.tv_time_max){
            mPresenter.onPredefinedTimeClicked(ManualFragContract.PredefinedTime.Max);
        } else if (viewId == R.id.tv_time_three_quarter) {
            mPresenter.onPredefinedTimeClicked(ManualFragContract.PredefinedTime.ThreeQuarters);
        } else if (viewId ==  R.id.tv_time_half){
            mPresenter.onPredefinedTimeClicked(ManualFragContract.PredefinedTime.Half);
        }else if(viewId == R.id.tv_time_quarter){
            mPresenter.onPredefinedTimeClicked(ManualFragContract.PredefinedTime.Quarter);
        } else if(viewId == R.id.btn_set) {
            mPresenter.onButtonSetClicked();
        }
    }

    @Override
    public void onProgressChanged(CircularSeekBar circularSeekBar, final int progress, boolean fromUser) {
        mPresenter.onSeekBarProgressChanged(progress,fromUser);
    }

    @Override
    public void onStopTrackingTouch(CircularSeekBar seekBar) {

    }

    @Override
    public void onStartTrackingTouch(CircularSeekBar seekBar) {

    }

    @Override
    public void setSeekBarMaxProgress(int maxProgress) {
        mSeekBar.setMax(maxProgress);
    }

    @Override
    public void setSeekBarProgress(int progress) {
        mSeekBar.setProgress(progress);
    }

    @Override
    public void setTimerText(String timeString) {
        mTvTimer.setText(timeString);
    }

    @Override
    public void setImageDrawableTint(int colorResource) {
        mIvState.getDrawable()
                .setTint(ResourcesCompat.getColor(getResources(),
                        colorResource,null));
    }

    @Override
    public void setTitleText(String nameString) {
        mTvTitle.setText(nameString);
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void switchToValveView() {
        mViewSwitcher.showNext();
    }

    @Override
    public void setPredefinedTimeText(@NotNull ManualFragContract.PredefinedTime predefinedTime, String timeString) {
        switch (predefinedTime) {
            case Zero:
                mTvZero.setText(timeString);
                break;
            case Quarter:
                mTvQuarter.setText(timeString);
                break;
            case Half:
                mTvHalf.setText(timeString);
                break;
            case ThreeQuarters:
                mTvThreeQuarter.setText(timeString);
                break;
            case Max:
                mTvMax.setText(timeString);
                break;
        }
    }

    @Override
    public int getSeekBarProgress() {
        return mSeekBar.getProgress();
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
}
