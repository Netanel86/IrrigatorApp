package com.netanel.irrigator_app;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.netanel.irrigator_app.databinding.SensorSeekbarBinding;
import com.netanel.irrigator_app.databinding.TabValveBinding;
import com.netanel.irrigator_app.services.StringExt;

import java.util.EnumSet;
import java.util.List;

import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.gridlayout.widget.GridLayout;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 10/05/2021
 */

public class ManualBindingAdapters {

    private static int mSensorDimensions = 0;
    private static int mOptimizedSensorCount = 0;

    @BindingAdapter("tabs")
    public static void setValveTabs(FilledTabLayout tabLayout, List<ValveViewModel> valves) {

        if(valves != null) {
            for (ValveViewModel viewModel :
            valves) {

                TabValveBinding binding =
                        DataBindingUtil.inflate(
                                LayoutInflater.from(tabLayout.getContext()),
                                R.layout.tab_valve,
                                tabLayout,
                                false);

                binding.setValveViewModel(viewModel);
                binding.setLifecycleOwner(FragmentManager.findFragment(tabLayout));

                TabLayout.Tab tab = tabLayout.newTab().setCustomView(binding.getRoot());
                tab.setId(View.generateViewId());

                tabLayout.addTab(tab, tabLayout.getTabCount());
            }
        }
    }

    @BindingAdapter("cells")
    public static void setSensorsGrid(GridLayout grid, List<SensorViewModel> sensors) {
        if(sensors != null) {
            for (SensorViewModel viewModel :
                    sensors) {

                SensorSeekbarBinding binding =
                        DataBindingUtil.inflate(LayoutInflater.from(grid.getContext()),
                                R.layout.sensor_seekbar,
                                grid,
                                false);
                binding.setSensorVM(viewModel);
                binding.setLifecycleOwner(FragmentManager.findFragment(grid));
                View sensorView = binding.getRoot();

                grid.addView(sensorView);
                calculateSensorOptimalDimen(sensorView, grid, sensors.size());
            }
        }
    }

    @BindingAdapter("onTabSelected")
    public static void setOnTabSelectedListener(FilledTabLayout tabLayout, ManualBindingAdapters.OnTabSelectedListener listener) {

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                TabValveBinding binding = DataBindingUtil.getBinding(tab.getCustomView());
                ValveViewModel selectedVm = binding.getValveViewModel();
                listener.onTabSelected(selectedVm);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @BindingAdapter("activeView")
    public static void setActiveView(ViewSwitcher viewSwitcher, boolean enabled) {
        if (!enabled) {
            if (viewSwitcher.getCurrentView() != viewSwitcher.findViewById(R.id.empty_layout)) {
                viewSwitcher.showPrevious();
            }
        } else if (viewSwitcher.getCurrentView() != viewSwitcher.findViewById(R.id.manual_layout)) {
            viewSwitcher.showNext();
        }
    }

    @BindingAdapter("states")
    public static void setViewStates(View view, EnumSet<ValveViewModel.State> states) {
        if(view instanceof IMultiStateView) {
            IMultiStateView stateView = (IMultiStateView) view;
            boolean isActivated = states != null && states.contains(ValveViewModel.State.ACTIVATED);
            boolean isEnabled = states != null && states.contains(ValveViewModel.State.ENABLED);
            boolean isEdited = states != null && states.contains(ValveViewModel.State.EDITED);

            stateView.setEnabled(isEnabled);
            stateView.setActivated(isActivated);
            stateView.setEdited(isEdited);

            view.refreshDrawableState();
        }
    }

    @BindingAdapter("textTime")
    public static void formatSecToTimeString(TextView textView, int seconds) {
        String[] mTimeNames = new String[]{
                textView.getResources().getString(R.string.time_unit_seconds),
                textView.getResources().getString(R.string.time_unit_minutes),
                textView.getResources().getString(R.string.time_unit_hours),
                textView.getResources().getString(R.string.time_unit_days)};
        textView.setText(StringExt.formatSecToTimeString(seconds, mTimeNames));
    }

    @BindingAdapter("message")
    public static void showMessage(View parentView, String message) {
        if (message != null && !message.isEmpty()) {
            Snackbar.make(parentView, message, Snackbar.LENGTH_LONG).show();
        }
    }

    @BindingAdapter("messageResource")
    public static void showResourceMessage(View parentView, int resource) {
        if(resource != 0) {
            Snackbar.make(parentView, resource, Snackbar.LENGTH_LONG).show();
        }
    }

    @BindingAdapter("messageComposed")
    public static void showComposedMessage(View parentView, Object[] messageArr) {
        if (messageArr != null) {
            StringBuilder builder = new StringBuilder();
            for (Object obj :
                    messageArr) {
                {
                    String append = null;
                    if(obj instanceof Integer) {
                        append = parentView.getResources().getString((int)obj);
                    } else if(obj instanceof String) {
                        append = (String) obj;
                    }
                    builder.append(append);
                }
            }
            Snackbar.make(parentView, builder.toString(), Snackbar.LENGTH_LONG).show();
        }
    }

    public static void setCircularSeekBarDimensions(CircularSeekBar seekBar, int widthAndHeight) {

        ViewGroup.LayoutParams params = seekBar.getLayoutParams();
        params.width = widthAndHeight;
        params.height = widthAndHeight;
        seekBar.setLayoutParams(params);
        seekBar.setCircleRadius((float) widthAndHeight / 2);
    }

    private static void calculateSensorOptimalDimen(final View sensorView, GridLayout grid, int sensorCount) {
        final View parentView = (View) grid.getParent().getParent();
        ViewTreeObserver viewTreeObserver = parentView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    parentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    if (mSensorDimensions == 0) {
                        ViewGroup.MarginLayoutParams gridParams =
                                (ViewGroup.MarginLayoutParams) grid.getLayoutParams();
                        int parentWidth = parentView.getMeasuredWidth();
                        int columnCount = grid.getColumnCount();
                        int gridMargins = gridParams.leftMargin + gridParams.rightMargin +
                                (gridParams.rightMargin * (columnCount - 1));
                        int padding = sensorView.getPaddingStart() * columnCount * 2;
                        mSensorDimensions = (parentWidth - gridMargins - padding) / columnCount;
                    }
                    CircularSeekBar seekBar = sensorView.findViewById(R.id.seekbar_sensor);
                    setCircularSeekBarDimensions(seekBar, mSensorDimensions);

                    mOptimizedSensorCount++;
                    if(mOptimizedSensorCount == sensorCount) { ////reset before next state change
                        mSensorDimensions = 0;
                        mOptimizedSensorCount = 0;
                    }
                }
            });
        }
    }

    /**
     * <p></p>
     *
     * @author Netanel Iting
     * @version %I%, %G%
     * @since 1.0
     * Created on 13/10/2021
     */

    public interface OnTabSelectedListener {
        void onTabSelected(ValveViewModel valveVm);
    }
}
