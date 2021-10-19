package com.netanel.irrigator_app;


import android.app.Application;

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

    public ObservableViewModel(@NonNull Application application) {
        super(application);
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
}
