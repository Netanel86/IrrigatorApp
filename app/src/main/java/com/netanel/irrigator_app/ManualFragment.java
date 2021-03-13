package com.netanel.irrigator_app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textview.MaterialTextView;
import com.netanel.irrigator_app.services.AppServices;

import org.jetbrains.annotations.NotNull;

// TODO: 13/03/2021 change view of disabled UI objects somehow to show its disabled state
public class ManualFragment extends Fragment implements
        View.OnClickListener,
        TabLayout.OnTabSelectedListener,
        CircularSeekBar.OnCircularSeekBarChangeListener,
        ManualFragContract.IView {

    private FilledTabLayout mValveTabs;

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
        mValveTabs = getView().findViewById(R.id.tab_group_valves);
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
        mValveTabs.addOnTabSelectedListener(this);
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
        int viewId = view.getId();
        if (viewId == R.id.tv_time_zero) {
            mPresenter.onTimeScaleClicked(ManualFragContract.TimeScale.Zero);
        } else if (viewId == R.id.tv_time_max) {
            mPresenter.onTimeScaleClicked(ManualFragContract.TimeScale.Max);
        } else if (viewId == R.id.tv_time_three_quarter) {
            mPresenter.onTimeScaleClicked(ManualFragContract.TimeScale.ThreeQuarters);
        } else if (viewId == R.id.tv_time_half) {
            mPresenter.onTimeScaleClicked(ManualFragContract.TimeScale.Half);
        } else if (viewId == R.id.tv_time_quarter) {
            mPresenter.onTimeScaleClicked(ManualFragContract.TimeScale.Quarter);
        } else if (viewId == R.id.i_btn_valve_state) {
            mPresenter.onButtonPowerClicked();
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
    public void onTabSelected(TabLayout.Tab tab) {
        mPresenter.onTabClicked(tab.getId());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

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
    public void setTimeScaleText(@NotNull ManualFragContract.TimeScale timeScale, String timeString) {
        switch (timeScale) {
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
    public void setPowerIconActiveState(boolean isActive) {
        mButtonState.setActivatedState(isActive);
    }

    @Override
    public void setPowerIconEditedState(boolean isEdited) {
        mButtonState.setEdited(isEdited);
    }

    @Override
    public void setUiEnabled(boolean enabled) {
        if(!enabled) {
            if (mViewSwitcher.getCurrentView() == getView().findViewById(R.id.view_manual)) {
                mViewSwitcher.showPrevious();
            }
        } else {
            if (mViewSwitcher.getCurrentView() == getView().findViewById(R.id.view_empty)) {
                mViewSwitcher.showNext();
            }
        }
        setTabLayoutEnabled(enabled);
    }

    private void setTabLayoutEnabled(boolean enabled) {
        ViewGroup tabLayout = (ViewGroup) mValveTabs.getChildAt(0);
        tabLayout.setEnabled(enabled);
        for(int i = 0; i < tabLayout.getChildCount(); i++) {
            tabLayout.getChildAt(i).setEnabled(enabled);
        }
    }

    @Override
    public void addTab( int tabId, String description, boolean isActive) {
        View tabView = getLayoutInflater().inflate(R.layout.tab_valve, null);

        TabLayout.Tab tab = mValveTabs.newTab().setCustomView(tabView);
        tab.setId(tabId);

        setTabText(tabView, description);

        if (isActive) {
            setTabBadgeVisibility(tabView, View.VISIBLE);
        }

        mValveTabs.addTab(tab, mValveTabs.getTabCount());
    }

    @Override
    public void setTabBadge(int tabId, boolean showActiveBadge) {
        View tabView = mValveTabs.getChildAt(0).findViewById(tabId);
        if (showActiveBadge) {
            setTabBadgeVisibility(tabView,View.VISIBLE);
        } else {
            setTabBadgeVisibility(tabView, View.GONE);
        }
    }

    private void setTabBadgeVisibility(View tab, int visibility) {
        ImageView badge = tab.findViewById(R.id.tab_valve_badge);
        badge.setVisibility(visibility);
    }

    @Override
    public void setTabDescription(Integer tabId, String description) {
        View tabView = mValveTabs.getChildAt(0).findViewById(tabId);
        setTabText(tabView, description);
    }

    private void setTabText(View tab, String description) {
        MaterialTextView innerTv = tab.findViewById(R.id.tab_valve_text);
        innerTv.setText(description.toUpperCase());
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