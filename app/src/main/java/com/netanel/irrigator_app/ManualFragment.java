package com.netanel.irrigator_app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ViewSwitcher;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textview.MaterialTextView;
import com.netanel.irrigator_app.databinding.FragmentManualBinding;
import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.services.StringExt;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// TODO: 27/04/2021 refactor sensor progress to increase in decimal values instead of integers for higher resolution.
// TODO: 27/04/2021 remove custom material dimensions and use android material styles instead.
public class ManualFragment extends Fragment implements
        View.OnClickListener,
        ManualFragContract.IView {

    private FilledTabLayout mValveTabs;
    private GridLayout mGridSensors;

    private ViewSwitcher mViewSwitcher;
    private String[] mTimeNames;
    private MaterialTextView mTvTimer;
    private MaterialTextView mTvTitle;
    private StateImageButton mButtonPower;
    private StateCircularSeekBar mSeekBar;

    private MaterialButton mTvMax;
    private MaterialButton mTvQuarter;
    private MaterialButton mTvHalf;
    private MaterialButton mTvThreeQuarter;

    private ManualFragContract.IPresenter mPresenter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mPresenter = new ViewModelProvider(this,
                AppServices.getInstance().getViewModelFactory()).get(ManualFragPresenter.class);

        mPresenter.bindView(this);

//        mTimeNames = new String[]{
//                getResources().getString(R.string.time_counter_seconds),
//                getResources().getString(R.string.time_counter_minutes),
//                getResources().getString(R.string.time_counter_hours),
//                getResources().getString(R.string.time_counter_days)};

        FragmentManualBinding binding =
                DataBindingUtil
                        .inflate(inflater, R.layout.fragment_manual, container, false);
        binding.setManualViewModel((ManualFragPresenter) mPresenter);
        binding.setLifecycleOwner(this);
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initUI();
        initListeners();

        if (mTabMap == null) {
            mTabMap = new HashMap<>();
        } else {
            mTabMap.clear();
        }

        if (mTabMapInverse == null) {
            mTabMapInverse = new HashMap<>();
        } else {
            mTabMap.clear();
        }

        mPresenter.onViewCreated();
    }

    private void initUI() {
        mValveTabs = getView().findViewById(R.id.tab_layout_valves);
//        mSeekBar = getView().findViewById(R.id.seekbar_timer);

        mButtonPower = getView().findViewById(R.id.img_btn_power);

        mTvTitle = getView().findViewById(R.id.tv_title);
        mTvTimer = getView().findViewById(R.id.tv_elapsed_time);
        mTvMax = getView().findViewById(R.id.tv_time_max);
        mTvQuarter = getView().findViewById(R.id.tv_time_quarter);
        mTvHalf = getView().findViewById(R.id.tv_time_half);
        mTvThreeQuarter = getView().findViewById(R.id.tv_time_three_quarter);

        mViewSwitcher = getView().findViewById(R.id.view_switcher);

        mGridSensors = getView().findViewById(R.id.grid_sensors);
    }

    private void initListeners() {
//        mValveTabs.addOnTabSelectedListener(this);
//        mSeekBar.setOnSeekBarChangeListener(this);
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
            mPresenter.onSendCommand();
        }
    }

    @Override
    public void addHumiditySensorView(float currValue, float maxValue) {
        addSensorView(currValue, maxValue, "%", R.drawable.ic_humidity_filled);
    }

    @Override
    public void addTemperatureSensorView(float currValue, float maxValue) {
        addSensorView(currValue, maxValue, StringExt.SYMBOL_CELSIUS, R.drawable.ic_thermometer);
    }

    @Override
    public void addFlowSensorView(float currValue, float maxValue) {
        addSensorView(currValue, maxValue, "L/s", R.drawable.ic_flow_meter);
    }

    @Override
    public void addPhSensorView(float currValue, float maxValue) {
        addSensorView(currValue, maxValue, "pH", R.drawable.ic_ph_meter);
    }

    private void addSensorView(float currValue, float maxValue, String unitSymbol, int iconResId) {
        String formattedValue =
                String.format(Locale.getDefault(), "%.1f%s", currValue, unitSymbol);
        final SensorView sensorView = new SensorView(this.getContext(), null);
        sensorView.setValueText(formattedValue);
        sensorView.setMaxProgress((int) maxValue);
        sensorView.setProgress((int) currValue);
        sensorView.setIcon(iconResId);

        sensorView.setPaddingInDp(3, 3, 3, 3);
        mGridSensors.addView(sensorView);
        setSensorViewOptimalDimensions(sensorView);
    }

    private int mSensorDimensions;

    private void setSensorViewOptimalDimensions(final SensorView sensorView) {
        final View parentView = getView().findViewById(R.id.cv_sensors);
        ViewTreeObserver viewTreeObserver = parentView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    parentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    if (mSensorDimensions == 0) {
                        ViewGroup.MarginLayoutParams gridParams =
                                (ViewGroup.MarginLayoutParams) getView()
                                        .findViewById(R.id.grid_sensors).getLayoutParams();
                        int parentWidth = parentView.getMeasuredWidth();
                        int columnCount = mGridSensors.getColumnCount();
                        int gridMargins = gridParams.leftMargin + gridParams.rightMargin +
                                (gridParams.rightMargin * (columnCount - 1));
                        int padding = sensorView.getPaddingStart() * columnCount * 2;
                        mSensorDimensions = (parentWidth - gridMargins - padding) / columnCount;
                    }
                    sensorView.setViewDimensions(mSensorDimensions);
                }
            });
        }
    }



//    @Override
//    public void onTabSelected(TabLayout.Tab tab) {
//        final int tabId = tab.getId();
//        mValveTabs.getChildAt(0).postDelayed(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        mPresenter.onValveSelected(mTabMapInverse.get(tabId));
//                    }
//                }, 150
//        );
//    }

    private boolean isTabSelected(int valveTabId) {
        int selectedTabId = mValveTabs.getTabAt(mValveTabs.getSelectedTabPosition()).getId();
        return selectedTabId == valveTabId;
    }

//    @Override
//    public void setSeekBarMaxProgress(int maxProgress) {
//        mSeekBar.setMax(maxProgress);
//    }

    @Override
    public void setSendCommandEnabledState(boolean enabled) {
        mButtonPower.setEnabled(enabled);
    }

    @Override
    public void setUiEnabled(boolean enabled) {
        if (!enabled) {
            if (mViewSwitcher.getCurrentView() == getView().findViewById(R.id.manual_layout)) {
                mViewSwitcher.showPrevious();
            }
        } else {
            if (mViewSwitcher.getCurrentView() == getView().findViewById(R.id.empty_layout)) {
                mViewSwitcher.showNext();
            }
        }
        setTabLayoutEnabled(enabled);
    }

    private void setTabLayoutEnabled(boolean enabled) {
        ViewGroup tabLayout = (ViewGroup) mValveTabs.getChildAt(0);
        tabLayout.setEnabled(enabled);
            for (int i = 0; i < tabLayout.getChildCount(); i++) {
            tabLayout.getChildAt(i).setEnabled(enabled);
        }
    }

    private Map<String, Integer> mTabMap;
    private Map<Integer, String> mTabMapInverse;

    @Override
    public void addValve(String valveId, String description, boolean isOpen) {
        View tabView = getLayoutInflater().inflate(R.layout.tab_valve, null);

        TabLayout.Tab tab = mValveTabs.newTab().setCustomView(tabView);
        tab.setId(View.generateViewId());

//        setTabText(tabView, description);

//        if (isOpen) {
//            setTabBadgeVisibility(tabView, View.VISIBLE);
//        }

        mValveTabs.addTab(tab, mValveTabs.getTabCount());

        if (mValveTabs.getVisibility() == View.GONE) {
            mValveTabs.setVisibility(View.VISIBLE);
        }

//        mTabMap.put(valveId, tab.getId());
//        mTabMapInverse.put(tab.getId(), valveId);
    }

//    @Override
//    public void addTab(int tabId, String description, boolean isActive) {
//        View tabView = getLayoutInflater().inflate(R.layout.tab_valve, null);
//
//        TabLayout.Tab tab = mValveTabs.newTab().setCustomView(tabView);
//        tab.setId(tabId);
//
//        setTabText(tabView, description);
//
//        if (isActive) {
//            setTabBadgeVisibility(tabView, View.VISIBLE);
//        }
//
//        mValveTabs.addTab(tab, mValveTabs.getTabCount());
//
//        if (mValveTabs.getVisibility() == View.GONE) {
//            mValveTabs.setVisibility(View.VISIBLE);
//        }
//    }
//
//    @Override
//    public void setTabBadge(int tabId, boolean showActiveBadge) {
//        View tabView = mValveTabs.getChildAt(0).findViewById(tabId);
//        if (showActiveBadge) {
//            setTabBadgeVisibility(tabView, View.VISIBLE);
//        } else {
//            setTabBadgeVisibility(tabView, View.GONE);
//        }
//    }

    @Override
    public void showMessage(String message) {
//        Toast.makeText(getContext(), resString, Toast.LENGTH_LONG).show();
        Snackbar.make(this.getView(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void runOnUiThread(Runnable runnable) {
        this.getActivity().runOnUiThread(runnable);
    }
    private static final boolean EDITED = true;
    private static final boolean ENABLED = true;

//    @Override
//    public void setValveOpen(String valveId, boolean isOpen) {
//
//        int valveTabId = mTabMap.get(valveId);
////        View tabView = mValveTabs.getChildAt(0).findViewById(valveTabId);
////
////        if (isOpen) {
////            setTabBadgeVisibility(tabView, View.VISIBLE);
////        } else {
////            setTabBadgeVisibility(tabView, View.GONE);
////        }
//
//        if(isTabSelected(valveTabId)) {
//            mButtonPower.setStateActivated(isOpen);
//            mButtonPower.setStateEdited(!EDITED);
//            mSeekBar.setStateEdited(!EDITED);
//            mButtonPower.setEnabled(isOpen);
//        }
//    }

//    @Override
//    public void setSelectedValveEdited() {
//        mButtonPower.setStateEdited(EDITED);
//        mSeekBar.setStateEdited(EDITED);
//    }
}