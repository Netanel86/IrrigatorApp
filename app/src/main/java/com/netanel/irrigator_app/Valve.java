package com.netanel.irrigator_app;


import com.google.firebase.firestore.DocumentId;

import java.sql.Time;
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
    private Date mLastOnTime;
    private int mDurationSec;

    private OnChangedListener onChangedListener;

    public boolean getState() {
        return mState;
    }

    public void setState(boolean state) {
        if( this.mState != state ) {
            this.mState = state;
            if(onChangedListener != null) {
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
        return mDurationSec;
    }

    public void setDuration(int seconds) {
        this.mDurationSec = seconds;
    }

    public Date getLastOnTime() {
        return mLastOnTime;
    }

    public void setLastOnTime(Date lastOnTime) {
        this.mLastOnTime = lastOnTime;
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
        if(this.mLastOnTime != null) {
            long diffInSec = TimeUnit.MILLISECONDS.toSeconds(
                    Calendar.getInstance().getTime().getTime() - this.mLastOnTime.getTime());
            if (this.mDurationSec - diffInSec > 0) {
                leftDuration = this.mDurationSec - diffInSec;
            }
        }

        return  leftDuration;
    }

    public interface OnChangedListener {
        void OnStateChanged(Valve updatedValve);
    }
}
