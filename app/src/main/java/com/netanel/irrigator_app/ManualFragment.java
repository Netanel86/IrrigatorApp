package com.netanel.irrigator_app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textview.MaterialTextView;
import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.services.StringExt;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ManualFragment extends Fragment implements
        View.OnClickListener,
        TabLayout.OnTabSelectedListener,
        CircularSeekBar.OnCircularSeekBarChangeListener,
        ManualFragContract.IView {

    private FilledTabLayout mValveTabs;

    private ViewSwitcher mViewSwitcher;

    private MaterialTextView mTvTimer;
    private MaterialTextView mTvTitle;
    private StateImageButton mButtonPower;
    private StateCircularSeekBar mSeekBar;
    private AppCompatButton mTvMax;
    private MaterialTextView mTvQuarter;
    private MaterialTextView mTvHalf;
    private MaterialTextView mTvThreeQuarter;

    private ManualFragContract.IPresenter mPresenter;

    private GridView mGridSensors;


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
        initListeners();
        mPresenter.onViewCreated();
    }

    private SensorsAdapter mSensorsAdapter;
    private void initUI() {
        mValveTabs = getView().findViewById(R.id.tab_group_valves);
        mSeekBar = getView().findViewById(R.id.seekbar_timer);

        mButtonPower = getView().findViewById(R.id.img_btn_power);

        mTvTitle = getView().findViewById(R.id.tv_title);
        mTvTimer = getView().findViewById(R.id.tv_elapsed_time);
        mTvMax = getView().findViewById(R.id.tv_time_max);
        mTvQuarter = getView().findViewById(R.id.tv_time_quarter);
        mTvHalf = getView().findViewById(R.id.tv_time_half);
        mTvThreeQuarter = getView().findViewById(R.id.tv_time_three_quarter);

        mViewSwitcher = getView().findViewById(R.id.view_switcher);

        mGridSensors = getView().findViewById(R.id.grid_sensors);

        mSensorsAdapter = new SensorsAdapter(this.getContext());
        mGridSensors.setAdapter(mSensorsAdapter);

    }

    private void initListeners() {
        mValveTabs.addOnTabSelectedListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
        mTvThreeQuarter.setOnClickListener(this);
        mTvMax.setOnClickListener(this);
        mTvHalf.setOnClickListener(this);
        mTvQuarter.setOnClickListener(this);
        mButtonPower.setOnClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPresenter.onDestroy();
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.tv_time_max) {
            mPresenter.onTimeScaleClicked(ManualFragContract.TimeScale.Max);
        } else if (viewId == R.id.tv_time_three_quarter) {
            mPresenter.onTimeScaleClicked(ManualFragContract.TimeScale.ThreeQuarters);
        } else if (viewId == R.id.tv_time_half) {
            mPresenter.onTimeScaleClicked(ManualFragContract.TimeScale.Half);
        } else if (viewId == R.id.tv_time_quarter) {
            mPresenter.onTimeScaleClicked(ManualFragContract.TimeScale.Quarter);
        } else if (viewId == R.id.img_btn_power) {
            mPresenter.onButtonPowerClicked();
        }
    }


    @Override
    public void addHumiditySensorView(float value) {
        addSensorView(value, "%", R.drawable.ic_humidity_filled);
    }

    @Override
    public void addTemperatureSensorView(float value) {
        addSensorView(value, StringExt.SYMBOL_CELSIUS, R.drawable.ic_thermometer);
    }

    @Override
    public void addFlowSensorView(float value) {
        addSensorView(value, "L/s", R.drawable.ic_flow_meter);
    }

    @Override
    public void addPhSensorView(float value) {
        addSensorView(value, "pH", R.drawable.ic_ph_meter);
    }

    private void addSensorView(float value, String unitSymbol, int iconResId) {
        String formattedValue =
                String.format(Locale.getDefault(), "%.1f%s", value, unitSymbol);
        mSensorsAdapter.addItem(value, formattedValue, iconResId);
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
        final int tabId = tab.getId();
        mValveTabs.getChildAt(0).postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        mPresenter.onTabClicked(tabId);
                    }
                }, 150
        );
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
    public void setSeekBarEditedState(boolean edited) {
        mSeekBar.setStateEdited(edited);
    }

    @Override
    public void setPowerButtonActiveState(boolean activated) {
        mButtonPower.setStateActivated(activated);
    }

    @Override
    public void setPowerButtonEditedState(boolean edited) {
        mButtonPower.setStateEdited(edited);
    }

    @Override
    public void setPowerButtonEnabled(boolean enabled) {
        mButtonPower.setEnabled(enabled);
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

        if(mValveTabs.getVisibility() == View.GONE) {
            mValveTabs.setVisibility(View.VISIBLE);
        }
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
//        Toast.makeText(getContext(), resString, Toast.LENGTH_LONG).show();
        Snackbar.make(this.getView(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void runOnUiThread(Runnable runnable) {
        this.getActivity().runOnUiThread(runnable);
    }
    ///endregion
}