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