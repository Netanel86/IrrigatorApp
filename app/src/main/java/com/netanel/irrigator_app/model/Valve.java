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
    public static final boolean STATE_ON = true;
    public static final boolean STATE_OFF = false;

    @DocumentId
    public String mId;

    private int mIndex;
    private boolean mState = STATE_OFF;
    private String mName;
    private Date mLastOn;
    private int mDurationInSec;

    private OnChangedListener onChangedListener;

    public boolean getState() {
        return mState;
    }

    public void setState(boolean state) {
        if (this.mState != state) {
            this.mState = state;
            if (onChangedListener != null) {
                onChangedListener.OnStateChanged(this);
            }
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
        this.mDurationInSec = seconds;
    }

    public Date getLastOnTime() {
        return mLastOn;
    }

    public void setLastOnTime(Date lastOnTime) {
        this.mLastOn = lastOnTime;
    }

    public void update(Valve newValve) {
        this.setState(newValve.getState());
        this.setLastOnTime(newValve.getLastOnTime());
        this.setDuration(newValve.getDuration());
    }

    public void setOnChangedListener(OnChangedListener onChangedListener) {
        this.onChangedListener = onChangedListener;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndex(int mIndex) {
        this.mIndex = mIndex;
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

    public interface OnChangedListener {
        void OnStateChanged(Valve updatedValve);
    }
}
