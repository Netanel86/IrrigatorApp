package com.netanel.irrigator_app.model;


import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    public static final int PROP_ID_DESCRIPTION = 0;
    public static final int PROP_ID_ON_TIME = 1;
    public static final int PROP_ID_DURATION = 2;
    public static final int PROP_ID_INDEX = 3;
    public static final int PROP_ID_MAX_DURATION = 4;
    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_ON_TIME = "time";
    public static final String PROP_DURATION = "duration";
    public static final String PROP_INDEX = "index";
    public static final String PROP_MAX_DURATION = "max_duration";

    @DocumentId
    public String mId;

    private int mIndex;
    private int mMaxDuration;
    private String mDescription;
    private Date mOnTime;
    private int mDurationInSec;
    private ArrayList<Sensor> mSensors;

    public Valve(){}

    public Valve(int index) {
        mIndex = index;
        mOnTime = new Date();
        mSensors = new ArrayList<>();
    }

    public void update(Valve updatedValve) {
        this.setOnTime(updatedValve.getOnTime());
        this.setDuration(updatedValve.getDuration());
        this.setDescription(updatedValve.getDescription());
        this.setIndex(updatedValve.getIndex());
        this.setMaxDuration(updatedValve.getMaxDuration());
    }

    public void update(Map<String, Object> propertyDict) {
        for (String propKey :
                propertyDict.keySet()) {
            switch (propKey){
                case Valve.PROP_ON_TIME:
                    this.setOnTime((Date)propertyDict.get(propKey));
                    break;
                case Valve.PROP_DURATION:
                    this.setDuration((int)propertyDict.get(propKey));
                    break;
                case Valve.PROP_DESCRIPTION:
                    this.setDescription((String)propertyDict.get(propKey));
                    break;
                case Valve.PROP_INDEX:
                    this.setIndex((int)propertyDict.get(propKey));
                    break;
                case Valve.PROP_MAX_DURATION:
                    this.setMaxDuration((int)propertyDict.get(propKey));
                    break;
            }
        }
    }

    public long getTimeLeft() {
        long leftDuration = 0;
        Date now = Calendar.getInstance().getTime();
        if (this.mOnTime != null && this.mDurationInSec > 0) {
            long diffInSec = TimeUnit.MILLISECONDS.toSeconds(
                    now.getTime() - this.mOnTime.getTime());
            if (this.mDurationInSec - diffInSec > 0) {
                leftDuration = this.mDurationInSec - diffInSec;
            }
        }
        return leftDuration;
    }

    public boolean isOn() {
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
            notifyPropertyChanged(PROP_ID_DURATION, oldDuration, seconds);
        }
    }

    public Date getOnTime() {
        return mOnTime;
    }

    public void setOnTime(Date lastOpen) {
        if(mOnTime != lastOpen) {
            if (mOnTime == null || !mOnTime.equals(lastOpen)) {
                Date oldLastOpen = mOnTime;
                mOnTime = lastOpen;
                notifyPropertyChanged(PROP_ID_ON_TIME, oldLastOpen, lastOpen);
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
            notifyPropertyChanged(PROP_ID_DESCRIPTION, oldName, description);
        }
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndex(int index) {
        if(this.mIndex != index) {
            int oldIndex = this.mIndex;
            this.mIndex = index;
            notifyPropertyChanged(PROP_ID_INDEX, oldIndex,index);
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
            notifyPropertyChanged(PROP_ID_MAX_DURATION, oldMaxDuration,maxDuration);
        }
    }

    public ArrayList<Sensor> getSensors() {
        return mSensors;
    }
}
