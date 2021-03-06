package com.netanel.irrigator_app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.netanel.irrigator_app.services.AppServices;

import org.jetbrains.annotations.NotNull;

// TODO: 04/03/2021 refactor radio button behavior where a thin line presents at the bottom of the button if checked.
// TODO: 04/03/2021 add color selector for radio button disabled state.
public class ManualFragment extends Fragment implements
        View.OnClickListener,
        CircularSeekBar.OnCircularSeekBarChangeListener,
        ManualFragContract.IView {

    private RadioGroup mValveRadioGroup;

    private ViewSwitcher mViewSwitcher;

    private TextView mTvTimer;
    private TextView mTvTitle;
    private StateImageButton mButtonState;
    private CircularSeekBar mSeekBar;
    private TextView mTvMax;
    private TextView mTvQuarter;
    private TextView mTvHalf;
    private TextView mTvThreeQuarter;
    private TextView mTvZero;
//    private Button mButtonSet;

    private ManualFragContract.IPresenter mPresenter;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mPresenter = new ViewModelProvider(this,
                AppServices.getInstance().getViewModelFactory()).get(ManualFragPresenter.class);

        mPresenter.bindView(this);


        return inflater.inflate(R.layout.fragment_manual, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initUI();
        initializeListeners();
        mPresenter.onViewCreated();
    }

    private void initUI() {
        mValveRadioGroup = getView().findViewById(R.id.radioGroup_valves);

        mSeekBar = getView().findViewById(R.id.circular_seekbar);
        mTvTimer = getView().findViewById(R.id.tv_elapsed_time);
        mButtonState = getView().findViewById(R.id.i_btn_valve_state);
        mTvTitle = getView().findViewById(R.id.tv_valve_name);

        mTvZero = getView().findViewById(R.id.tv_time_zero);
        mTvMax = getView().findViewById(R.id.tv_time_max);
        mTvQuarter = getView().findViewById(R.id.tv_time_quarter);
        mTvHalf = getView().findViewById(R.id.tv_time_half);
        mTvThreeQuarter = getView().findViewById(R.id.tv_time_three_quarter);
        mViewSwitcher = getView().findViewById(R.id.view_switcher);

        mButtonState = getView().findViewById(R.id.i_btn_valve_state);
    }

    private void initializeListeners() {
        mSeekBar.setOnSeekBarChangeListener(this);
        mTvThreeQuarter.setOnClickListener(this);
        mTvMax.setOnClickListener(this);
        mTvZero.setOnClickListener(this);
        mTvHalf.setOnClickListener(this);
        mTvQuarter.setOnClickListener(this);
        mButtonState.setOnClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPresenter.onDestroy();
    }

    @Override
    public void onClick(View view) {
        if (view instanceof StateRadioButton) {
            mPresenter.onStateRadioButtonClicked(view.getId());
        } else {
            int viewId = view.getId();
            if (viewId == R.id.tv_time_zero) {
                mPresenter.onPredefinedTimeClicked(ManualFragContract.PredefinedTime.Zero);
            } else if (viewId == R.id.tv_time_max) {
                mPresenter.onPredefinedTimeClicked(ManualFragContract.PredefinedTime.Max);
            } else if (viewId == R.id.tv_time_three_quarter) {
                mPresenter.onPredefinedTimeClicked(ManualFragContract.PredefinedTime.ThreeQuarters);
            } else if (viewId == R.id.tv_time_half) {
                mPresenter.onPredefinedTimeClicked(ManualFragContract.PredefinedTime.Half);
            } else if (viewId == R.id.tv_time_quarter) {
                mPresenter.onPredefinedTimeClicked(ManualFragContract.PredefinedTime.Quarter);
            } else if (viewId == R.id.i_btn_valve_state) {
                mPresenter.onButtonPowerClicked();
            }
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

    ///region ManualFragContract.IView
    @Override
    public void setTitleText(String nameString) {
        mTvTitle.setText(nameString);
    }

    @Override
    public void setTimerText(String timeString) {
        mTvTimer.setText(timeString);
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
    public void setSeekBarMaxProgress(int maxProgress) {
        mSeekBar.setMax(maxProgress);
    }

    @Override
    public void setSeekBarProgress(int progress) {
        mSeekBar.setProgress(progress);
    }

    @Override
    public int getSeekBarProgress() {
        return mSeekBar.getProgress();
    }

    @Override
    public void setPowerIconActivatedState(boolean state) {
        mButtonState.setActivatedState(state);
    }

    @Override
    public void setPowerIconEditedState(boolean isEdited) {
        mButtonState.setEdited(isEdited);
    }

    @Override
    public void setUiEnabled(boolean enabled) {
        if (!enabled) {
            if(mViewSwitcher.getCurrentView() == getView().findViewById(R.id.view_manual)) {
                mViewSwitcher.showPrevious();
            }
            mValveRadioGroup.clearCheck();

            if (mValveRadioGroup.isEnabled()) {
                setRadioGroupEnabled(false);
            }
        } else {
            if (!mValveRadioGroup.isEnabled()) {
                setRadioGroupEnabled(true);
            }
        }
    }

    private void setRadioGroupEnabled(boolean enabled) {
        for (int i = 0; i < mValveRadioGroup.getChildCount(); i++) {
            mValveRadioGroup.getChildAt(i).setEnabled(enabled);
        }
        mValveRadioGroup.setEnabled(enabled);
    }

    @Override
    public int addStateRadioButton(boolean valveState, String viewString) {
        RadioButton btnValve = initStateRadioButton(valveState, viewString);
        mValveRadioGroup.addView(btnValve);

        return btnValve.getId();
    }

    private RadioButton initStateRadioButton(boolean state, String btnText) {
        final StateRadioButton btnValve =
                new StateRadioButton(
                        new ContextThemeWrapper(getContext(), R.style.ManualFrag_StateRadioButton),
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
        btnValve.setState(state);

        return btnValve;
    }

    @Override
    public void setRadioButtonState(int btnId, boolean newState) {
        StateRadioButton btnValve = getView().findViewById(btnId);
        btnValve.setState(newState);
    }

    @Override
    public void switchToValveView() {
        mViewSwitcher.showNext();
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void runOnUiThread(Runnable runnable) {
        this.getActivity().runOnUiThread(runnable);
    }
    ///endregion


}