package com.netanel.irrigator_app;

import androidx.core.content.res.ResourcesCompat;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainer;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.devadvance.circularseekbar.CircularSeekBar;

import org.jetbrains.annotations.NotNull;

public class ValveFragment extends Fragment implements
        View.OnClickListener,
        CircularSeekBar.OnCircularSeekBarChangeListener,
        ValveFragContract.IView {

    public static final String BUNDLE_VALVE = "valve";

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

    public ValveFragment(ValveFragContract.IPresenter viewModel) {
        mPresenter = viewModel;
        mPresenter.bindView(this);
    }

    private ValveFragContract.IPresenter mPresenter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mPresenter.onCreateView();

        return inflater.inflate(R.layout.valve_man_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeUI();
        initializeListeners();
        mPresenter.onViewCreated();
    }

    private void initializeUI() {
        mSeekBar = getView().findViewById(R.id.circular_seekbar);
        mTvTimer = getView().findViewById(R.id.tv_elapsed_time);
        mIvState = getView().findViewById(R.id.iv_valve_state);
        mTvTitle = getView().findViewById(R.id.tv_valve_name);

        mTvZero = getView().findViewById(R.id.tv_time_zero);
        mTvMax = getView().findViewById(R.id.tv_time_max);
        mTvQuarter = getView().findViewById(R.id.tv_time_quarter);
        mTvHalf = getView().findViewById(R.id.tv_time_half);
        mTvThreeQuarter = getView().findViewById(R.id.tv_time_three_quarter);

        mButtonSet = getView().findViewById(R.id.btn_set);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_time_zero:
                mPresenter.onPredefinedTimeClicked(ValveFragContract.PredefinedTime.Zero);
                break;
            case R.id.tv_time_max:
                mPresenter.onPredefinedTimeClicked(ValveFragContract.PredefinedTime.Max);
                break;
            case R.id.tv_time_three_quarter:
                mPresenter.onPredefinedTimeClicked(ValveFragContract.PredefinedTime.ThreeQuarters);
                break;
            case R.id.tv_time_half:
                mPresenter.onPredefinedTimeClicked(ValveFragContract.PredefinedTime.Half);
                break;
            case R.id.tv_time_quarter:
                mPresenter.onPredefinedTimeClicked(ValveFragContract.PredefinedTime.Quarter);
                break;
            case R.id.btn_set:
                FragmentManager manager = getParentFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                for (Fragment frag:
                manager.getFragments()) {
                    transaction.remove(frag);
                }
                transaction.add(
                    R.id.fragment_container_view,
                    new Fragment(R.layout.valve_empty_layout),
                    "Empty").commit();
//                mPresenter.onButtonSetClicked();
                break;
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
        Toast.makeText(this.getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void setPredefinedTimeText(@NotNull ValveFragContract.PredefinedTime predefinedTime, String timeString) {
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
}