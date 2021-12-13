package com.netanel.irrigator_app;


import android.app.Application;

import com.netanel.irrigator_app.services.AppServices;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.AndroidViewModel;







/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 13/10/2021
 */

public class ObservableViewModel extends AndroidViewModel implements Observable {

    private PropertyChangeRegistry mCallBacks;

    public ObservableViewModel() {
        super(AppServices.getInstance().getApplication());
    }

    @Override
    public void addOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        if(mCallBacks == null) {
            mCallBacks = new PropertyChangeRegistry();
        }

        mCallBacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        if(mCallBacks != null) {
            mCallBacks.remove(callback);
        }
    }

    protected void notifyPropertyChanged(int fieldId) {
        if (mCallBacks != null) {
            mCallBacks.notifyCallbacks(this, fieldId, null);
        }
    }

    @Override
    protected void onCleared() {
        mCallBacks.clear();
        super.onCleared();
    }
}
