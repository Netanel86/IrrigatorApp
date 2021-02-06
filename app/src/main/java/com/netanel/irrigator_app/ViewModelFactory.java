package com.netanel.irrigator_app;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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

class ViewModelFactory implements ViewModelProvider.Factory {
//    private ManualFragRouter mRouter;

    public ViewModelFactory() {
//        mRouter = router;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        T viewModel = null;
        try {
            viewModel = modelClass.getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert viewModel != null;
        return viewModel;
    }
}
