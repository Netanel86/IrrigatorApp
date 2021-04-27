package com.netanel.irrigator_app.model;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 22/04/2021
 */

public class Sensor {
    private String mParentControllerId;
    private String mId;
    private Measure mMeasure;
    private float mValue;
    private float mMaxValue;

    public Sensor() {}

    public Sensor(Measure measure, int maxValue) {
        mMeasure = measure;
        mMaxValue = maxValue;
    }

    public Measure getMeasureType() {
        return this.mMeasure;
    }

    public void setMeasureType(Measure measure) {
        this.mMeasure = measure;
    }

    public float getValue() {
        return mValue;
    }

    public void setValue(float value) {
        mValue = value;
    }

    public String getParentControllerId() {
        return mParentControllerId;
    }

    public void setParentControllerId(String parentId) {
        mParentControllerId = parentId;
    }

    public float getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(int mMaxValue) {
        this.mMaxValue = mMaxValue;
    }

    public enum Measure {
        HUMIDITY,
        TEMPERATURE,
        PH,
        EC,
        FLOW
    }
}
