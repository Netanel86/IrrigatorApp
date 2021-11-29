package com.netanel.irrigator_app;


import com.netanel.irrigator_app.model.Valve;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

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

public class ValveViewModel implements Observable {

    private final Valve mValve;

    private int mEditedProgress;

    private EnumSet<StateFlag> mViewStates;

    private Map<ManualFragContract.TimeScale, String> mTimeScales;

    private PropertyChangeRegistry mCallBacks;

    public ValveViewModel(Valve valve) {
        mValve = valve;
        mViewStates = EnumSet.noneOf(StateFlag.class);

        initializeListener();
        resetViewStates();
        initTimeScales();
    }

    private void initializeListener() {
        mValve.setOnPropertyChangedCallback(new PropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Object sender, int propertyId, Object oldValue, Object newValue) {
                switch (propertyId) {
                    case Valve.PROPERTY_DURATION:
                    case Valve.PROPERTY_LAST_ON:
                        resetViewStates();
                        mEditedProgress = 0;

                        notifyPropertyChanged(BR.open);
                        notifyPropertyChanged(BR.progress);
                        break;

                    case Valve.PROPERTY_MAX_DURATION:
                        initTimeScales();

                        notifyPropertyChanged(BR.maxDuration);
                        break;

                    case Valve.PROPERTY_DESCRIPTION:
                    case Valve.PROPERTY_INDEX:
                        notifyPropertyChanged(BR.description);
                        break;
                }
            }
        });
    }

    public String getId() {
        return mValve.getId();
    }

    public int getIndex() {
        return mValve.getIndex();
    }

    public Date getLastOpen() {
        return mValve.getLastOpen();
    }

    public int getEditedProgress() {
        return mEditedProgress;
    }

    @Bindable
    public int getProgress() {
        return mEditedProgress > 0 ? mEditedProgress : getTimeLeft();
    }

    public void setProgress(int progress) {
        if (this.mEditedProgress != progress) {
            this.mEditedProgress = progress;
            notifyPropertyChanged(BR.progress);
        }
    }

    @Bindable
    public String getDescription() {
        return mValve.getDescription() == null || mValve.getDescription().isEmpty() ?
                "#" + mValve.getIndex() : mValve.getDescription();
    }

    public int getTimeLeft() {
        return (int) mValve.getTimeLeft();
    }

    @Bindable
    public boolean isOpen() {
        return mValve.isOpen();
    }

    @Bindable
    public int getMaxDuration() {
        return mValve.getMaxDuration();
    }

    @Bindable
    public Map<ManualFragContract.TimeScale, String> getTimeScales() {
        return mTimeScales;
    }

    private void initTimeScales() {
        if (mTimeScales == null) {
            mTimeScales = new HashMap<>();
        } else {
            mTimeScales.clear();
        }

        for (ManualFragContract.TimeScale scale : ManualFragContract.TimeScale.values()
        ) {
            mTimeScales.put(scale, String.valueOf((int) (getMaxDuration() * scale.value / 60)));
        }

        notifyPropertyChanged(BR.timeScales);
    }

    @Bindable
    public EnumSet<StateFlag> getViewStates() {
        return mViewStates;
    }

    public void setViewStates(EnumSet<StateFlag> currentStates) {
        mViewStates = currentStates;
        notifyPropertyChanged(BR.viewStates);
    }

    public void removeViewState(StateFlag state) {
        mViewStates.remove(state);
        notifyPropertyChanged(BR.viewStates);
    }

    public void addViewState(StateFlag state) {
        mViewStates.add(state);
        notifyPropertyChanged(BR.viewStates);
    }

    public void resetViewStates() {
        if (isOpen()) {
            setViewStates(EnumSet.of(StateFlag.ACTIVATED, StateFlag.ENABLED));
        } else {
            setViewStates(EnumSet.noneOf(StateFlag.class));
        }
    }

    public boolean isEdited() {
        return mViewStates.contains(StateFlag.EDITED);
    }

    public void setEdited(boolean isEdited) {
        if (isEdited) {
            addViewState(StateFlag.ENABLED);
            addViewState(StateFlag.EDITED);
        } else {
            resetViewStates();
        }
    }

    @Override
    public void addOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        if (mCallBacks == null) {
            mCallBacks = new PropertyChangeRegistry();
        }

        mCallBacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        if (mCallBacks != null) {
            mCallBacks.remove(callback);
        }
    }

    protected void notifyPropertyChanged(int fieldId) {
        if (mCallBacks != null) {
            mCallBacks.notifyCallbacks(this, fieldId, null);
        }
    }

    public enum StateFlag {
        ACTIVATED,
        ENABLED,
        EDITED;
    }
}

