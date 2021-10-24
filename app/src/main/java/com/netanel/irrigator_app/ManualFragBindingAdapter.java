package com.netanel.irrigator_app;


import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textview.MaterialTextView;
import com.netanel.irrigator_app.databinding.TabValveBinding;
import com.netanel.irrigator_app.model.Valve;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;

import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 10/05/2021
 */

public class ManualFragBindingAdapter {

    @BindingAdapter("android:TabMap")
    public static void setValveTabs(FilledTabLayout tabLayout, Map<String, ValveViewModel> valveMap) {

        if(valveMap != null) {
            ArrayList<ValveViewModel> valves = new ArrayList<>(valveMap.values());
            for (int i = 0; i < valves.size(); i++) {
                ValveViewModel currValve = valves.get(i);

                TabValveBinding binding =
                        DataBindingUtil.inflate(
                                LayoutInflater.from(tabLayout.getContext()),
                                R.layout.tab_valve,
                                tabLayout,
                                false);

                binding.setValveViewModel(currValve);
                binding.setLifecycleOwner(FragmentManager.findFragment(tabLayout));

                TabLayout.Tab tab = tabLayout.newTab().setCustomView(binding.getRoot());
                tab.setId(View.generateViewId());

                tabLayout.addTab(tab, tabLayout.getTabCount());
            }

//            if (tabLayout.getVisibility() == View.GONE) {
//                tabLayout.setVisibility(View.VISIBLE);
//            }
        }
    }

    @BindingAdapter("android:OnTabSelected")
    public static void setOnTabSelectedListener(FilledTabLayout tabLayout, ManualFragBindingAdapter.OnTabSelectedListener listener) {

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

    @BindingAdapter("android:activeView")
    public static void setActiveView(ViewSwitcher viewSwitcher, int viewId) {
        if(viewId == ManualFragPresenter.VIEW_EMPTY) {
            if(viewSwitcher.getCurrentView() != viewSwitcher.findViewById(R.id.empty_layout)) {
                viewSwitcher.showPrevious();
            }
        } else if( viewId == ManualFragPresenter.VIEW_VALVE) {
            if(viewSwitcher.getCurrentView() != viewSwitcher.findViewById(R.id.manual_layout)) {
                viewSwitcher.showNext();
            }
        }
    }

    @BindingAdapter("android:states")
    public static void setViewStates(View view, EnumSet<ValveViewModel.StateFlag> stateFlags) {
        if(view instanceof IMultiStateView) {
            IMultiStateView stateView = (IMultiStateView) view;
            boolean isActivated = stateFlags != null && stateFlags.contains(ValveViewModel.StateFlag.ACTIVATED);
            boolean isEnabled = stateFlags != null && stateFlags.contains(ValveViewModel.StateFlag.ENABLED);
            boolean isEdited = stateFlags != null && stateFlags.contains(ValveViewModel.StateFlag.EDITED);

            stateView.setEnabled(isEnabled);
            stateView.setStateActivated(isActivated);
            stateView.setStateEdited(isEdited);
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
