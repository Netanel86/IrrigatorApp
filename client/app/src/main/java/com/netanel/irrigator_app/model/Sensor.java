package com.netanel.irrigator_app.model;


import com.google.firebase.firestore.DocumentId;
import com.netanel.irrigator_app.services.StringExt;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 22/04/2021
 */

public class Sensor extends Observable{
    public static final int PROP_MEASURE = 0;
    public static final int PROP_MAX_VALUE = 1;
    public static final int PROP_MIN_VALUE = 3;
    public static final int PROP_VALUE = 2;
    public static final int PROP_PARENT_ID = 3;

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    @DocumentId
    public String mId;

    private String mControllerId;
    private SensorType mType;
    private double mCurrVal;
    private double mMaxVal;
    private double mMinVal;

    public Sensor() {
    }

    public Sensor(SensorType type, int maxValue) {
        mType = type;
        mMaxVal = maxValue;
    }

    public SensorType getType() {
        return this.mType;
    }

    public void setType(SensorType type) {
        if (mType != type) {
            SensorType oldVal = mType;
            mType = type;
            notifyPropertyChanged(PROP_MEASURE, oldVal, mType);
        }
    }

    public double getCurrVal() {
        return mCurrVal;
    }

    public void setCurrVal(double value) {
        if (mCurrVal != value) {
            double oldVal = mCurrVal;
            mCurrVal = value;
            notifyPropertyChanged(PROP_VALUE, oldVal, mCurrVal);
        }
    }

    public String getControllerId() {
        return mControllerId;
    }

    public void setControllerId(String parentId) {
        if (mControllerId == null || !mControllerId.equals(parentId)) {
            String oldVal = mControllerId;
            mControllerId = parentId;
            notifyPropertyChanged(PROP_PARENT_ID, oldVal, mControllerId);
        }
    }

    public double getMaxVal() {
        return mMaxVal;
    }

    public void setMaxVal(double maxValue) {
        if (mMaxVal != maxValue) {
            double oldVal = mMaxVal;
            mMaxVal = maxValue;
            notifyPropertyChanged(PROP_MAX_VALUE, oldVal, mMaxVal);
        }
    }

    public double getMinVal() {
        return mMaxVal;
    }

    public void setMinVal(double minValue) {
        if (mMinVal != minValue) {
            double oldVal = mMinVal;
            mMinVal = minValue;
            notifyPropertyChanged(PROP_MIN_VALUE, oldVal, mMinVal);
        }
    }

    public void update(Sensor updated) {
        this.setControllerId(updated.getControllerId());
        this.setMaxVal(updated.getMaxVal());
        this.setType(updated.getType());
        this.setCurrVal(updated.getCurrVal());
    }

    public enum SensorType {
        HUMIDITY("%"),
        TEMPERATURE(StringExt.SYMBOL_CELSIUS),
        PH("pH"),
        EC("EC"),
        FLOW("L/s");

        public final String symbol;

        SensorType(String symbol) {
            this.symbol = symbol;
        }
    }
}
