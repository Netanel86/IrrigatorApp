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
// TODO: 03/02/2021 add mState property and make it work, dont forget to add it also in firebase.
public class Valve {
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_LAST_ON = "lastOn";
    public static final String PROPERTY_DURATION = "duration";
    public static final String PROPERTY_STATUS = "status";

    @DocumentId
    public String mId;

    private int mIndex;
    private int mMaxDuration;
    private String mName;
    private Date mLastOn;
    private int mDurationInSec;

    private boolean mStatus;

    private OnChangedListener onChangedListener;

    public void update(Valve updatedValve) {
        this.setStatus(updatedValve.getStatus());
        this.setLastOnTime(updatedValve.getLastOnTime());
        this.setDuration(updatedValve.getDuration());
        this.setName(updatedValve.getName());
    }

    public boolean getStatus() { return mStatus/* = getTimeLeftOn() > 0*/; }

    public void setStatus(boolean newStatus) {
        if (mStatus != newStatus) {
            mStatus = newStatus;
            onPropertyChanged(PROPERTY_STATUS, !mStatus);
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
            onPropertyChanged(PROPERTY_LAST_ON,oldLastOn);
        }
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        if(mName == null || !mName.equals(name)) {
            String oldName = this.mName;
            this.mName = name;
            onPropertyChanged(PROPERTY_NAME,oldName);
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

    public void setOnChangedListener(OnChangedListener onChangedListener) {
        this.onChangedListener = onChangedListener;
    }

    private void onPropertyChanged(String changedProperty, Object oldValue) {
        if (onChangedListener != null) {
            onChangedListener.OnPropertyChanged(this, changedProperty, oldValue);
        }
    }

    public interface OnChangedListener {
        void OnPropertyChanged(Valve updatedValve, String propertyName, Object oldValue);
    }
}
