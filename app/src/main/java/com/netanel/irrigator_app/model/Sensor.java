package com.netanel.irrigator_app.model;


import com.netanel.irrigator_app.PropertyChangedCallback;
import com.netanel.irrigator_app.services.StringExt;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 22/04/2021
 */

public class Sensor {
    public static final int PROP_MEASURE = 0x1;
    public static final int PROP_MAX_VALUE = 0x2;
    public static final int PROP_VALUE = 0x3;
    public static final int PROP_PARENT_ID = 0x4;

    private String mParentControllerId;
    private String mId;
    private Measure mMeasure;
    private double mValue;
    private double mMaxValue;

    private PropertyChangedCallback mCallback;

    public Sensor() {}

    public Sensor(Measure measure, int maxValue) {
        mMeasure = measure;
        mMaxValue = maxValue;
    }

    public Measure getMeasures() {
        return this.mMeasure;
    }

    public void setMeasures(Measure measure) {
        if(mMeasure != measure) {
            Measure oldVal = mMeasure;
            mMeasure = measure;
            notifyPropertyChange(PROP_MEASURE, oldVal, mMeasure);
        }
    }

    public double getValue() {
        return mValue;
    }

    public void setValue(double value) {
        if(mValue != value) {
            double oldVal = mValue;
            mValue = value;
            notifyPropertyChange(PROP_VALUE, oldVal, mValue);
        }
    }

    public String getParentControllerId() {
        return mParentControllerId;
    }

    public void setParentControllerId(String parentId) {
        if(!mParentControllerId.equals(parentId)) {
            String oldVal = mParentControllerId;
            mParentControllerId = parentId;
            notifyPropertyChange(PROP_PARENT_ID, oldVal, mParentControllerId);
        }
    }

    public double getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(double maxValue) {
        if(mMaxValue != maxValue) {
            double oldVal = mMaxValue;
            mMaxValue = maxValue;
            notifyPropertyChange(PROP_MAX_VALUE, oldVal, mMaxValue);
        }
    }

    public void setOnPropertyChangedCallback(PropertyChangedCallback mCallback) {
        this.mCallback = mCallback;
    }

    private void notifyPropertyChange(int propertyId, Object oldValue,Object newValue) {
        if (mCallback != null) {
            mCallback.onPropertyChanged(this, propertyId, oldValue, newValue);
        }
    }

    public enum Measure {
        HUMIDITY("%"),
        TEMPERATURE(StringExt.SYMBOL_CELSIUS),
        PH("pH"),
        EC("EC"),
        FLOW("L/s");

        public final String symbol;
        Measure(String symbol) {this.symbol = symbol;}
    }
}
