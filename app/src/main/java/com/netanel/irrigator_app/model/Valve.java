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
public class Valve extends Observable {
    public static final boolean OPEN = true;
    public static final int PROPERTY_DESCRIPTION = 0;
    public static final int PROPERTY_LAST_ON = 1;
    public static final int PROPERTY_DURATION = 2;
    public static final int PROPERTY_INDEX = 3;
    public static final int PROPERTY_MAX_DURATION = 4;

    @DocumentId
    public String mId;

    private int mIndex;
    private int mMaxDuration;
    private String mDescription;
    private Date mLastOpen;
    private int mDurationInSec;

    public Valve(){}

    public Valve(int index) {
        mIndex = index;
        mLastOpen = new Date();
    }

    public void update(Valve updatedValve) {
        this.setLastOpen(updatedValve.getLastOpen());
        this.setDuration(updatedValve.getDuration());
        this.setDescription(updatedValve.getDescription());
        this.setIndex(updatedValve.getIndex());
        this.setMaxDuration(updatedValve.getMaxDuration());
    }

    public long getTimeLeft() {
        long leftDuration = 0;
        Date now = Calendar.getInstance().getTime();
        if (this.mLastOpen != null && mLastOpen.before(now)) {
            long diffInSec = TimeUnit.MILLISECONDS.toSeconds(
                    Calendar.getInstance().getTime().getTime() - this.mLastOpen.getTime());
            if (this.mDurationInSec - diffInSec > 0) {
                leftDuration = this.mDurationInSec - diffInSec;
            }
        }
        return leftDuration;
    }

    public boolean isOpen() {
        return getTimeLeft() > 0;
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
            notifyPropertyChanged(PROPERTY_DURATION, oldDuration, seconds);
        }
    }

    public Date getLastOpen() {
        return mLastOpen;
    }

    public void setLastOpen(Date lastOpen) {
        if(mLastOpen != lastOpen) {
            if (mLastOpen == null || !mLastOpen.equals(lastOpen)) {
                Date oldLastOpen = mLastOpen;
                mLastOpen = lastOpen;
                notifyPropertyChanged(PROPERTY_LAST_ON, oldLastOpen, lastOpen);
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
            notifyPropertyChanged(PROPERTY_DESCRIPTION, oldName, description);
        }
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndex(int index) {
        if(this.mIndex != index) {
            int oldIndex = this.mIndex;
            this.mIndex = index;
            notifyPropertyChanged(PROPERTY_INDEX, oldIndex,index);
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
            notifyPropertyChanged(PROPERTY_MAX_DURATION, oldMaxDuration,maxDuration);
        }
    }
}
