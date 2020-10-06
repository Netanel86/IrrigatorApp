package com.netanel.irrigator_app;

import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.model.Valve;

import java.util.Locale;

public class ValveFragment extends Fragment
        implements View.OnClickListener, CircularSeekBar.OnCircularSeekBarChangeListener {

    public static final String BUNDLE_VALVE = "valve";

    public static ValveFragment newInstance() {
        return new ValveFragment();
    }

    private CircularSeekBar mSeekBar;
    private TextView mTvOnTime;
    private TextView mTvValveName;
    private ImageView mIvValveState;

    private TextView mTextViewMax;
    private TextView mTextViewOneQuarter;
    private TextView mTextViewHalf;
    private TextView mTextViewThreeQuarter;
    private TextView mTextViewZero;

    private ValveViewModel mValveViewModel;
    private Valve mValve;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if(getArguments() != null) {
            mValve = AppServices.getInstance()
                    .getJsonParser().fromJson(getArguments().getString(BUNDLE_VALVE), Valve.class);
            mValve.setOnChangedListener(new Valve.OnChangedListener() {
                @Override
                public void OnStateChanged(Valve updatedValve) {
                    setDrawableTintByState(updatedValve.getState());
                }
            });
            initValveDbListener(mValve);
        }


        return inflater.inflate(R.layout.valve_man_fragment, container, false);
    }

    private void initValveDbListener(final Valve valve) {
        AppServices.getInstance().getDbConnection()
                .addOnValveChangedListener(valve.getId(), new IDataBaseConnection.OnDataChangedListener<Valve>() {
                    @Override
                    public void onDataChanged(Valve changedObject, Exception ex) {
                        if (ex != null) {
//                            getView().showMessage(ex.getMessage());
                        }
                        valve.update(changedObject);
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initUI();

        mValveViewModel = new ViewModelProvider(this).get(ValveViewModel.class);

        if (mValve != null) showValve();
        else Toast.makeText(this.getContext(),"Error loading valve",Toast.LENGTH_LONG).show();

    }

    private void initUI() {
        mSeekBar = getView().findViewById(R.id.circular_seekbar);
        mTvOnTime = getView().findViewById(R.id.tv_elapsed_time);
        mIvValveState = getView().findViewById(R.id.iv_valve_state);
        mTextViewZero = getView().findViewById(R.id.tv_time_zero);
        mTextViewMax = getView().findViewById(R.id.tv_time_max);
        mTextViewOneQuarter = getView().findViewById(R.id.tv_time_quarter);
        mTextViewHalf = getView().findViewById(R.id.tv_time_half);
        mTextViewThreeQuarter = getView().findViewById(R.id.tv_time_three_quarter);
        mTvValveName = getView().findViewById(R.id.tv_valve_name);


        mSeekBar.setOnSeekBarChangeListener(this);
        mTextViewThreeQuarter.setOnClickListener(this);
        mTextViewMax.setOnClickListener(this);
        mTextViewZero.setOnClickListener(this);
        mTextViewHalf.setOnClickListener(this);
        mTextViewOneQuarter.setOnClickListener(this);

        mTextViewZero.setText(String.valueOf(0));
        mTextViewOneQuarter.setText(String.valueOf((int) (mSeekBar.getMax() * 0.25 / 60)));
        mTextViewHalf.setText(String.valueOf((int) (mSeekBar.getMax() * 0.5 / 60)));
        mTextViewThreeQuarter.setText(String.valueOf((int) (mSeekBar.getMax() * 0.75 / 60)));
        mTextViewMax.setText(String.valueOf((int) (mSeekBar.getMax() / 60)));
    }

    private void showValve() {
        mTvValveName.setText(mValve.getName());
        mSeekBar.setProgress((int) mValve.getTimeLeftOn());
        setDrawableTintByState(mValve.getState());

        if (mValve.getTimeLeftOn() > 0) {
            mTimer = new CountDownTimer(
                    mValve.getTimeLeftOn() * 1000,
                    1000) {
                @Override
                public void onTick(long l) {
                    mSeekBar.setProgress((int) l / 1000);
                }

                @Override
                public void onFinish() {}
            }.start();
        } else {
            mTvOnTime.setText(getCurrentProgressText(0));
            mSeekBar.setProgress(0);
        }
    }


    private void setDrawableTintByState(boolean state) {
        if(state == Valve.STATE_ON) {
            mIvValveState.getDrawable()
                    .setTint(ResourcesCompat.getColor(getResources(),
                            android.R.color.holo_green_light,null));
        } else {
            mIvValveState.getDrawable()
                    .setTint(ResourcesCompat.getColor(getResources(),
                            android.R.color.holo_red_dark,null));
        }
    }

    private String getCurrentProgressText(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d %s",
                minutes, seconds, minutes > 0 ? "Minutes" : "Seconds");
    }

    @Override
    public void onClick(View view) {

        if(mTimer != null && view instanceof TextView) {
            mTimer.cancel();
        }

        switch (view.getId()) {
            case R.id.tv_time_zero:
                mSeekBar.setProgress(0);
                break;
            case R.id.tv_time_max:
                mSeekBar.setProgress(mSeekBar.getMax());
                break;
            case R.id.tv_time_three_quarter:
                mSeekBar.setProgress((int) (mSeekBar.getMax() * 0.75));
                break;
            case R.id.tv_time_half:
                mSeekBar.setProgress((int) (mSeekBar.getMax() * 0.5));
                break;
            case R.id.tv_time_quarter:
                mSeekBar.setProgress((int) (mSeekBar.getMax() * 0.25));
                break;
        }
    }

    private CountDownTimer mTimer;

    @Override
    public void onProgressChanged(CircularSeekBar circularSeekBar, final int progress, boolean fromUser) {

        if(mTimer != null && fromUser) {
            mTimer.cancel();
        }

        mTvOnTime.setText(getCurrentProgressText(progress));
    }

    @Override
    public void onStopTrackingTouch(CircularSeekBar seekBar) {

    }

    @Override
    public void onStartTrackingTouch(CircularSeekBar seekBar) {

    }
}