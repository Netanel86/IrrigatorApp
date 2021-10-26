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

public class ValveViewModel implements Observable{

    private final Valve mValve;
    private EnumSet<StateFlag> mViewStates = StateFlag.NONE;
    private Map<ManualFragContract.TimeScale,String> mTimeScales;
    private PropertyChangeRegistry mCallBacks;

    public ValveViewModel(Valve valve) {
        mValve = valve;
        initializeListener();
        resetViewStates();
        initTimeScales();
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
                        notifyPropertyChanged(BR.timeLeft);

                        resetViewStates();

//                        if (mSelectedValve == updatedValve.getId()) {
//                            updateSelectedValveProgressView();
//                        }
                        break;

                    case Valve.PROPERTY_MAX_DURATION:
                        notifyPropertyChanged(BR.maxDuration);
                        initTimeScales();
//                        if (mSelectedValve == updatedValve) {
//                            mView.setSelectedValveMaxProgress(mSelectedValve.getMaxDuration());
//                            updateSelectedValveProgressView();
//                        }
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

    @Bindable
    public String getDescription() {
        return mValve.getDescription() == null || mValve.getDescription().isEmpty() ?
                "#" + mValve.getIndex() : mValve.getDescription();
    }

    @Bindable
    public int getTimeLeft() {
        return (int) mValve.timeLeftOpen();
    }

    public Date getLastOpen() {
        return mValve.getLastOnTime();
    }

    @Bindable
    public boolean isOpen() {
        return mValve.isOpen();
    }

    private int mProgress;

    @Bindable
    public int getProgress() {
        return mProgress > 0 ? mProgress : getTimeLeft();
    }

    public void setProgress(int progress) {
        if(this.mProgress != progress) {
            this.mProgress = progress;
            notifyPropertyChanged(BR.progress);
            onProgressChanged();
        }
    }

    private void onProgressChanged() {
        if(isUserProgressChange()) {
            if (isOpen()) {
                setViewStates(StateFlag.ALL_STATES);
            } else {
                setViewStates(EnumSet.of(StateFlag.ENABLED,StateFlag.EDITED));
            }
        }
    }
    private boolean isUserProgressChange() {
        long time = getTimeLeft();
        int progress = getProgress();
        int diff = (int) time - progress;
        return diff >= 5 || diff <= -5;
    }
    @Bindable
    public int getMaxDuration() {
        return mValve.getMaxDuration();
    }

    public int getIndex() {
        return mValve.getIndex();
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

    private void resetViewStates() {
        if(isOpen()) {
            setViewStates(EnumSet.of(StateFlag.ACTIVATED, StateFlag.ENABLED));
        } else {
            setViewStates(StateFlag.NONE);
        }
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



    public enum StateFlag {
        ACTIVATED,
        ENABLED,
        EDITED;

        public static EnumSet<StateFlag> ALL_STATES = EnumSet.allOf(StateFlag.class);
        public static EnumSet<StateFlag> NONE = EnumSet.noneOf(StateFlag.class);
    }
}

