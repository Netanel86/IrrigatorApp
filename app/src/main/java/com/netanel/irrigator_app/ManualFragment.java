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
public class ManualFragment extends Fragment{

    private ManualFragContract.IPresenter mPresenter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mPresenter = new ViewModelProvider(this,
                AppServices.getInstance().getViewModelFactory()).get(ManualFragPresenter.class);

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

//        mPresenter.onViewCreated();
    }

//    public void addHumiditySensorView(float currValue, float maxValue) {
//        addSensorView(currValue, maxValue, "%", R.drawable.ic_humidity_filled);
//    }
//
//    public void addTemperatureSensorView(float currValue, float maxValue) {
//        addSensorView(currValue, maxValue, StringExt.SYMBOL_CELSIUS, R.drawable.ic_thermometer);
//    }
//
//    public void addFlowSensorView(float currValue, float maxValue) {
//        addSensorView(currValue, maxValue, "L/s", R.drawable.ic_flow_meter);
//    }
//
//    public void addPhSensorView(float currValue, float maxValue) {
//        addSensorView(currValue, maxValue, "pH", R.drawable.ic_ph_meter);
//    }

//    private void addSensorView(float currValue, float maxValue, String unitSymbol, int iconResId) {
//        String formattedValue =
//                String.format(Locale.getDefault(), "%.1f%s", currValue, unitSymbol);
//        final SensorView sensorView = new SensorView(this.getContext(), null);
//        sensorView.setValueText(formattedValue);
//        sensorView.setMaxProgress((int) maxValue);
//        sensorView.setProgress((int) currValue);
//        sensorView.setIcon(iconResId);
//
//        sensorView.setPaddingInDp(3, 3, 3, 3);
//        mGridSensors.addView(sensorView);
//        setSensorViewOptimalDimensions(sensorView);
//    }

    private int mSensorDimensions;

//    private void setSensorViewOptimalDimensions(final SensorView sensorView) {
//        final View parentView = getView().findViewById(R.id.cv_sensors);
//        ViewTreeObserver viewTreeObserver = parentView.getViewTreeObserver();
//        if (viewTreeObserver.isAlive()) {
//            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//                @Override
//                public void onGlobalLayout() {
//                    parentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//
//                    if (mSensorDimensions == 0) {
//                        ViewGroup.MarginLayoutParams gridParams =
//                                (ViewGroup.MarginLayoutParams) getView()
//                                        .findViewById(R.id.grid_sensors).getLayoutParams();
//                        int parentWidth = parentView.getMeasuredWidth();
//                        int columnCount = mGridSensors.getColumnCount();
//                        int gridMargins = gridParams.leftMargin + gridParams.rightMargin +
//                                (gridParams.rightMargin * (columnCount - 1));
//                        int padding = sensorView.getPaddingStart() * columnCount * 2;
//                        mSensorDimensions = (parentWidth - gridMargins - padding) / columnCount;
//                    }
//                    sensorView.setViewDimensions(mSensorDimensions);
//                }
//            });
//        }
//    }

//    public void addValve(String valveId, String description, boolean isOpen) {
//        View tabView = getLayoutInflater().inflate(R.layout.tab_valve, null);
//
//        TabLayout.Tab tab = mValveTabs.newTab().setCustomView(tabView);
//        tab.setId(View.generateViewId());
//
//        mValveTabs.addTab(tab, mValveTabs.getTabCount());
//
//        if (mValveTabs.getVisibility() == View.GONE) {
//            mValveTabs.setVisibility(View.VISIBLE);
//        }
//    }
}