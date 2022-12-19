package com.netanel.irrigator_app;


import com.netanel.irrigator_app.model.ListenerRegistration;
import com.netanel.irrigator_app.model.Module;
import com.netanel.irrigator_app.model.Sensor;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.databinding.Bindable;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 10/10/2021
 */

public class ModuleViewModel extends ObservableViewModel{

    private final Module mModule;

    private int mEditedProgress;

    private EnumSet<State> mViewStates;

    private Map<IManualViewModel.Scale, String> mScaleStrings;

    private final ListenerRegistration mListenerRegistration;

    private ArrayList<SensorViewModel> mSensorsViewModels;

    public ModuleViewModel(Module module) {
        super();
        mModule = module;
        mViewStates = EnumSet.noneOf(State.class);

        mListenerRegistration = mModule.addPropertyChangedListener(this::onValvePropertyChanged);
        resetViewStates();
        initTimeScales();

        if (module.getSensors() != null) {
            initSensorViewModels(module.getSensors());
        }
    }

    public String getId() {
        return mModule.getId();
    }

    public String getIp() {
        return mModule.getIp();
    }

    public Date getLastOpen() {
        return mModule.getOnTime();
    }

    public int getTimeLeft() {
        return (int) mModule.getTimeLeft();
    }

    @Bindable
    public boolean isOpen() {
        return mModule.isOn();
    }

    @Bindable
    public String getDescription() {
        return mModule.getDescription() == null || mModule.getDescription().isEmpty() ?
                "#" + mModule.getIp() : mModule.getDescription();
    }

    @Bindable
    public int getMaxDuration() {
        return mModule.getMaxDuration();
    }

    @Bindable
    public int getProgress() {
        return mEditedProgress > 0 ? mEditedProgress : getTimeLeft();
    }

    public void setProgress(int progress) {
        if (this.mEditedProgress != progress) {
            this.mEditedProgress = progress;
            notifyPropertyChanged(BR.progress);
            if(mEditedProgress == 0) {
                notifyPropertyChanged(BR.open);
                resetViewStates();
            }
        }
    }

    public int getEditedProgress() {
        return mEditedProgress;
    }

    @Bindable
    public Map<IManualViewModel.Scale, String> getScaleStrings() {
        return mScaleStrings;
    }

    @Bindable
    public EnumSet<State> getViewStates() {
        return mViewStates;
    }

    public void setViewStates(EnumSet<State> currentStates) {
        mViewStates = currentStates;
        notifyPropertyChanged(BR.viewStates);
    }

    public void addViewStates(EnumSet<State> currentStates) {
        mViewStates.addAll(currentStates);
        notifyPropertyChanged(BR.viewStates);
    }

    public void addViewState(State state) {
        mViewStates.add(state);
        notifyPropertyChanged(BR.viewStates);
    }

    public void removeViewState(State state) {
        mViewStates.remove(state);
        notifyPropertyChanged(BR.viewStates);
    }

    public void resetViewStates() {
        if (isOpen()) {
            setViewStates(EnumSet.of(State.ACTIVATED, State.ENABLED));
        } else {
            setViewStates(EnumSet.noneOf(State.class));
        }
    }

    public boolean isEdited() {
        return mViewStates.contains(State.EDITED);
    }

    public void setEdited(boolean isEdited) {
        if (isEdited) {
            addViewStates(EnumSet.of(State.ENABLED, State.EDITED));
        } else {
            resetViewStates();
        }
    }

    @Override
    protected void onCleared() {
        mModule.removeListenerRegistration(mListenerRegistration);
        super.onCleared();
    }

    public void onValvePropertyChanged(Object sender, int propertyId, Object oldValue, Object newValue) {
        switch (propertyId) {
            case Module.PROP_ID_ON_TIME:
            case Module.PROP_ID_DURATION:
                resetViewStates();
                mEditedProgress = 0;
                notifyPropertyChanged(BR.open);
                notifyPropertyChanged(BR.progress);
                break;

            case Module.PROP_ID_MAX_DURATION:
                initTimeScales();
                notifyPropertyChanged(BR.maxDuration);
                break;

            case Module.PROP_ID_DESCRIPTION:
            case Module.PROP_ID_INDEX:
                notifyPropertyChanged(BR.description);
                break;
            case Module.PROP_ID_SENSORS:
                initSensorViewModels(mModule.getSensors());
                break;
        }
    }

    private void initTimeScales() {
        if (mScaleStrings == null) {
            mScaleStrings = new HashMap<>();
        } else {
            mScaleStrings.clear();
        }

        for (IManualViewModel.Scale scale : IManualViewModel.Scale.values()
        ) {
            mScaleStrings.put(scale, String.valueOf((int) (getMaxDuration() * scale.value / 60)));
        }

        notifyPropertyChanged(BR.scaleStrings);
    }

    public List<SensorViewModel> getSensorsViewModels() {
        return this.mSensorsViewModels;
    }

    public void initSensorViewModels(List<Sensor> sensors) {
        if( mSensorsViewModels != null) {
            mSensorsViewModels.clear();
        }
        mSensorsViewModels = new ArrayList<>();
        if(sensors != null) {
            for (Sensor sensor :
                    sensors) {
                mSensorsViewModels.add(new SensorViewModel(sensor));
            }
        }
    }

    public enum State {
        ACTIVATED,
        ENABLED,
        EDITED;
    }
}

