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
public class Module extends Observable {
    public static final int PROP_ID_DESCRIPTION = 0;
    public static final int PROP_ID_ON_TIME = 1;
    public static final int PROP_ID_DURATION = 2;
    public static final int PROP_ID_INDEX = 3;
    public static final int PROP_ID_MAX_DURATION = 4;
    public static final int PROP_ID_SENSORS = 5;
    public static final String PROP_DESCRIPTION = "description";
    public static final String PROP_ON_TIME = "onTime";
    public static final String PROP_DURATION = "duration";
    public static final String PROP_IP = "ip";
    public static final String PROP_MAX_DURATION = "maxDuration";

    @DocumentId
    public String mId;

    private String mIp;
    private int mMaxDuration;
    private String mDescription;
    private Date mOnTime;
    private int mDurationInSec;
    private ArrayList<Sensor> mSensors;

    public Module(){}

    public Module(String ip) {
        mIp = ip;
        mOnTime = new Date();
        mSensors = new ArrayList<>();
    }

    public void update(Module updatedModule) {
        this.setOnTime(updatedModule.getOnTime());
        this.setDuration(updatedModule.getDuration());
        this.setDescription(updatedModule.getDescription());
        this.setIp(updatedModule.getIp());
        this.setMaxDuration(updatedModule.getMaxDuration());
    }

    public void update(Map<String, Object> propertyDict) {
        for (String propKey :
                propertyDict.keySet()) {
            switch (propKey){
                case Module.PROP_ON_TIME:
                    this.setOnTime((Date)propertyDict.get(propKey));
                    break;
                case Module.PROP_DURATION:
                    this.setDuration((int)propertyDict.get(propKey));
                    break;
                case Module.PROP_DESCRIPTION:
                    this.setDescription((String)propertyDict.get(propKey));
                    break;
                case Module.PROP_IP:
                    this.setIp((String)propertyDict.get(propKey));
                    break;
                case Module.PROP_MAX_DURATION:
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

    public String getIp() {
        return mIp;
    }

    public void setIp(String ip) {
        if(this.mIp != ip) {
            String oldIndex = this.mIp;
            this.mIp = ip;
            notifyPropertyChanged(PROP_ID_INDEX, oldIndex, ip);
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
    public void setSensors(List<Sensor> sensors){
        if(this.mSensors != sensors) {
            List<Sensor> oldVal = this.mSensors;
            this.mSensors = new ArrayList<>(sensors);
            notifyPropertyChanged(PROP_ID_SENSORS, oldVal, sensors);
        }
    }
}
