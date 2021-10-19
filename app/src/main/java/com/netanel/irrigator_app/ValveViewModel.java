package com.netanel.irrigator_app;


import android.app.Application;

import com.netanel.irrigator_app.model.Valve;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.databinding.Bindable;
import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 10/10/2021
 */

public class ValveViewModel implements Observable{
    private final Valve mValve;

    public ValveViewModel(Valve valve) {
        mValve = valve;
        initializeListener();
    }

    @Bindable
    public String getDescription() {
        return mValve.getDescription() == null || mValve.getDescription().isEmpty() ?
                "#" + mValve.getIndex() : mValve.getDescription();
    }

    public String getId() {
        return mValve.getId();
    }

    public long timeLeftOpen() {
        return mValve.timeLeftOpen();
    }

    public Date getLastOnTime() {
        return mValve.getLastOnTime();
    }

    @Bindable
    public boolean isOpen() {
        return mValve.isOpen();
    }

    public int getMaxDuration() {
        return mValve.getMaxDuration();
    }

    public int getIndex() {
        return mValve.getIndex();
    }

    private void initializeListener() {
        mValve.setOnPropertyChangedCallback(new Valve.OnPropertyChangedCallback() {
            @Override
            public void OnPropertyChanged(Valve updatedValve, String propertyName, Object oldValue) {
                switch (propertyName) {
                    case Valve.PROPERTY_DURATION:
                    case Valve.PROPERTY_LAST_ON_TIME:
                    case Valve.PROPERTY_OPEN:
                        notifyPropertyChanged(BR.open);
//                        if (tabId != null) {
//                            mView.setTabBadge(tabId, updatedValve.isOpen());
//                        }
//                        mView.setValveOpen(updatedValve.getId(),updatedValve.isOpen());

//                        if (mSelectedValve == updatedValve.getId()) {
//                            updateSelectedValveProgressView();
//                        }
                        break;

                    case Valve.PROPERTY_MAX_DURATION:
//                        if (mSelectedValve == updatedValve) {
//                            mView.setSelectedValveMaxProgress(mSelectedValve.getMaxDuration());
//                            updateSelectedValveProgressView();
//                        }
                        break;

                    case Valve.PROPERTY_DESCRIPTION:
                    case Valve.PROPERTY_INDEX:
                        notifyPropertyChanged(BR.description);
//                        mView.setValveDescription(updatedValve.getId(),formatValveDescription(updatedValve));
//
//                        if (tabId != null) {
//                            mView.setTabDescription(tabId, formatValveDescription(updatedValve));
//                        }
                        break;
                }
            }
        });
    }

    private PropertyChangeRegistry mCallBacks;

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

