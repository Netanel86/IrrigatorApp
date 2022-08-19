package com.netanel.irrigator_app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.netanel.irrigator_app.databinding.FragmentManualBinding;
import com.netanel.irrigator_app.services.AppServices;

// TODO: 27/04/2021 remove custom material dimensions and use android material styles instead.
public class ManualFragment extends Fragment{

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        ManualViewModel viewModel = new ViewModelProvider(this,
                (ViewModelProvider.Factory) AppServices.getInstance().getViewModelFactory()).get(ManualViewModel.class);

        FragmentManualBinding binding =
                DataBindingUtil
                        .inflate(inflater, R.layout.fragment_manual, container, false);

        binding.setManualViewModel(viewModel);
        binding.setLifecycleOwner(this);
        return binding.getRoot();
    }
}