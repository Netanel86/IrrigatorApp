package com.netanel.irrigator_app.model;


import com.google.firebase.Timestamp;
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
    public static final boolean OPEN = true;
    public static final String PROPERTY_DESCRIPTION = "description";
    public static final String PROPERTY_LAST_ON_TIME = "lastOnTime";
    public static final String PROPERTY_DURATION = "duration";
    public static final String PROPERTY_OPEN = "isOpen";
    public static final String PROPERTY_INDEX = "index";
    public static final String PROPERTY_MAX_DURATION = "maxDuration";

    @DocumentId
    public String mId;

    private int mIndex;
    private int mMaxDuration;
    private String mDescription;
    private Date mLastOn;
    private int mDurationInSec;
    private boolean mIsOpen;

    private OnPropertyChangedCallback mOnPropertyChangedCallback;

    public Valve(){}

    public Valve(int index) {
        mIndex = index;
        mLastOn = new Date();
    }

    public void update(Valve updatedValve) {
        this.setOpen(updatedValve.isOpen());
        this.setLastOnTime(updatedValve.getLastOnTime());
        this.setDuration(updatedValve.getDuration());
        this.setDescription(updatedValve.getDescription());
        this.setIndex(updatedValve.getIndex());
        this.setMaxDuration(updatedValve.getMaxDuration());
    }

    public long timeLeftOpen() {
        long leftDuration = 0;
        Date now = Calendar.getInstance().getTime();
        if (this.mLastOn != null && mLastOn.before(now)) {
            long diffInSec = TimeUnit.MILLISECONDS.toSeconds(
                    Calendar.getInstance().getTime().getTime() - this.mLastOn.getTime());
            if (this.mDurationInSec - diffInSec > 0) {
                leftDuration = this.mDurationInSec - diffInSec;
            }
        }
        return leftDuration;
    }

    public boolean isOpen() {
        return mIsOpen && timeLeftOpen() > 0;
    }

    public void setOpen(boolean isOpen) {
        if (mIsOpen != isOpen) {
            mIsOpen = isOpen;
            onPropertyChanged(PROPERTY_OPEN, !mIsOpen);
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
        if(mLastOn != lastOnTime) {
            if (mLastOn == null || (mLastOn != null && !mLastOn.equals(lastOnTime))) {
                Date oldLastOn = mLastOn;
                mLastOn = lastOnTime;
                onPropertyChanged(PROPERTY_LAST_ON_TIME, oldLastOn);
            }
        }
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        if (mDescription == null || !mDescription.equals(description)) {
            String oldName = this.mDescription;
            this.mDescription = description;
            onPropertyChanged(PROPERTY_DESCRIPTION, oldName);
        }
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndex(int index) {
        if(this.mIndex != index) {
            int oldIndex = this.mIndex;
            this.mIndex = index;
            onPropertyChanged(PROPERTY_INDEX, oldIndex);
        }
    }

    public int getMaxDuration() {
        return mMaxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        if(mMaxDuration != maxDuration) {
            mDurationInSec = mDurationInSec > maxDuration ? 0 : mDurationInSec;
            int oldMaxDuration = this.mMaxDuration;
            this.mMaxDuration = maxDuration;
            onPropertyChanged(PROPERTY_MAX_DURATION, oldMaxDuration);
        }
    }

    public void setOnPropertyChangedCallback(OnPropertyChangedCallback onPropertyChangedCallback) {
        this.mOnPropertyChangedCallback = onPropertyChangedCallback;
    }

    private void onPropertyChanged(String changedProperty, Object oldValue) {
        if (mOnPropertyChangedCallback != null) {
            mOnPropertyChangedCallback.OnPropertyChanged(this, changedProperty, oldValue);
        }
    }

    public interface OnPropertyChangedCallback {
        void OnPropertyChanged(Valve updatedValve, String propertyName, Object oldValue);
    }
}
