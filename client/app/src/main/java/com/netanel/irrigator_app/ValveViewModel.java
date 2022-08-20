package com.netanel.irrigator_app;


import com.netanel.irrigator_app.model.ListenerRegistration;
import com.netanel.irrigator_app.model.Valve;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
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

public class ValveViewModel extends ObservableViewModel{

    private final Valve mValve;

    private int mEditedProgress;

    private EnumSet<State> mViewStates;

    private Map<IManualViewModel.Scale, String> mScaleStrings;

    private final ListenerRegistration mListenerRegistration;

    public ValveViewModel(Valve valve) {
        super();
        mValve = valve;
        mViewStates = EnumSet.noneOf(State.class);

        mListenerRegistration = mValve.addPropertyChangedListener(this::onValvePropertyChanged);
        resetViewStates();
        initTimeScales();
    }

    public String getId() {
        return mValve.getId();
    }

    public int getIndex() {
        return mValve.getIndex();
    }

    public Date getLastOpen() {
        return mValve.getOnTime();
    }

    public int getTimeLeft() {
        return (int) mValve.getTimeLeft();
    }

    @Bindable
    public boolean isOpen() {
        return mValve.isOn();
    }

    @Bindable
    public String getDescription() {
        return mValve.getDescription() == null || mValve.getDescription().isEmpty() ?
                "#" + mValve.getIndex() : mValve.getDescription();
    }

    @Bindable
    public int getMaxDuration() {
        return mValve.getMaxDuration();
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
            addViewState(State.ENABLED);
            addViewState(State.EDITED);
        } else {
            resetViewStates();
        }
    }

    @Override
    protected void onCleared() {
        mValve.removeListenerRegistration(mListenerRegistration);
        super.onCleared();
    }

    public void onValvePropertyChanged(Object sender, int propertyId, Object oldValue, Object newValue) {
        switch (propertyId) {
            case Valve.PROP_ID_DURATION:
            case Valve.PROP_ID_ON_TIME:
                resetViewStates();
                mEditedProgress = 0;
                notifyPropertyChanged(BR.progress);
                notifyPropertyChanged(BR.open);
                break;

            case Valve.PROP_ID_MAX_DURATION:
                initTimeScales();

                notifyPropertyChanged(BR.maxDuration);
                break;

            case Valve.PROP_ID_DESCRIPTION:
            case Valve.PROP_ID_INDEX:
                notifyPropertyChanged(BR.description);
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

    public enum State {
        ACTIVATED,
        ENABLED,
        EDITED;
    }
}

