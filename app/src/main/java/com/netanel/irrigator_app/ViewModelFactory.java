package com.netanel.irrigator_app;


import android.app.Application;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
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
//    private ManualFragRouter mRouter;
    private final Application mApplication;

    public ViewModelFactory(@Nonnull Application app) {
        mApplication = app;
//        mRouter = router;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        T viewModel = null;
        try {
            if (modelClass == ManualFragPresenter.class) {
                viewModel = (T)new ManualFragPresenter(mApplication);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert viewModel != null;
        return viewModel;
    }

}
