package com.netanel.irrigator_app;


import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 12/10/2020
 */

public class ViewModelFactory extends ViewModelProvider.NewInstanceFactory {

    private final Application mApplication;

    public ViewModelFactory(@NonNull Application app) {
        mApplication = app;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        T viewModel = null;
        try {
            if (modelClass == ManualViewModel.class) {
                viewModel = (T) new ManualViewModel(mApplication);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert viewModel != null;
        return viewModel;
    }
}
