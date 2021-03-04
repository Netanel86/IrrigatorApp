package com.netanel.irrigator_app.model;


import com.google.firebase.firestore.DocumentId;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 02/09/2020
 */
public class Valve {
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_LAST_ON = "lastOn";
    public static final String PROPERTY_DURATION = "duration";
    public static final String PROPERTY_STATE = "state";

    @DocumentId
    public String mId;

    private int mIndex;
    private int mMaxDuration;
    private String mName;
    private Date mLastOn;
    private int mDurationInSec;

    private boolean mState;

    private OnPropertyChangedListener onPropertyChangedListener;

    public void update(Valve updatedValve) {
        this.setState(updatedValve.getState());
        this.setLastOnTime(updatedValve.getLastOnTime());
        this.setDuration(updatedValve.getDuration());
        this.setName(updatedValve.getName());
    }

    public boolean getState() {
        return mState;
    }

    public void setState(boolean newState) {
        if (mState != newState) {
            mState = newState;
            onPropertyChanged(PROPERTY_STATE, !mState);
        }
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public int getDuration() {
        return mDurationInSec;
    }

    public void setDuration(int seconds) {
        if (this.mDurationInSec != seconds) {
            int oldDuration = this.mDurationInSec;
            this.mDurationInSec = seconds;
            onPropertyChanged(PROPERTY_DURATION, oldDuration);
        }
    }

    public Date getLastOnTime() {
        return mLastOn;
    }

    public void setLastOnTime(Date lastOnTime) {
        if (this.mLastOn != lastOnTime) {
            Date oldLastOn = this.mLastOn;
            this.mLastOn = lastOnTime;
            onPropertyChanged(PROPERTY_LAST_ON, oldLastOn);
        }
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        if (mName == null || !mName.equals(name)) {
            String oldName = this.mName;
            this.mName = name;
            onPropertyChanged(PROPERTY_NAME, oldName);
        }
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndex(int index) {
        this.mIndex = index;
    }

    public long getTimeLeftOn() {
        long leftDuration = 0;
        if (this.mLastOn != null) {
            long diffInSec = TimeUnit.MILLISECONDS.toSeconds(
                    Calendar.getInstance().getTime().getTime() - this.mLastOn.getTime());
            if (this.mDurationInSec - diffInSec > 0) {
                leftDuration = this.mDurationInSec - diffInSec;
            }
        }
        return leftDuration;
    }

    public int getMaxDuration() {
        return mMaxDuration;
    }

    public void setMaxDuration(int mMaxDuration) {
        this.mMaxDuration = mMaxDuration;
    }

    public void setOnPropertyChangedListener(OnPropertyChangedListener onPropertyChangedListener) {
        this.onPropertyChangedListener = onPropertyChangedListener;
    }

    private void onPropertyChanged(String changedProperty, Object oldValue) {
        if (onPropertyChangedListener != null) {
            onPropertyChangedListener.OnPropertyChanged(this, changedProperty, oldValue);
        }
    }

    public interface OnPropertyChangedListener {
        void OnPropertyChanged(Valve updatedValve, String propertyName, Object oldValue);
    }
}
