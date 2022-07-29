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
    public static final int PROP_VALUE = 2;
    public static final int PROP_PARENT_ID = 3;

    @DocumentId
    public String mId;

    private String mControllerId;
    private Measure mMeasure;
    private double mValue;
    private double mMaxValue;

    public Sensor() {
    }

    public Sensor(Measure measure, int maxValue) {
        mMeasure = measure;
        mMaxValue = maxValue;
    }

    public Measure getMeasures() {
        return this.mMeasure;
    }

    public void setMeasures(Measure measure) {
        if (mMeasure != measure) {
            Measure oldVal = mMeasure;
            mMeasure = measure;
            notifyPropertyChanged(PROP_MEASURE, oldVal, mMeasure);
        }
    }

    public double getValue() {
        return mValue;
    }

    public void setValue(double value) {
        if (mValue != value) {
            double oldVal = mValue;
            mValue = value;
            notifyPropertyChanged(PROP_VALUE, oldVal, mValue);
        }
    }

    public String getControllerId() {
        return mControllerId;
    }

    public void setControllerId(String parentId) {
        if (!mControllerId.equals(parentId)) {
            String oldVal = mControllerId;
            mControllerId = parentId;
            notifyPropertyChanged(PROP_PARENT_ID, oldVal, mControllerId);
        }
    }

    public double getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(double maxValue) {
        if (mMaxValue != maxValue) {
            double oldVal = mMaxValue;
            mMaxValue = maxValue;
            notifyPropertyChanged(PROP_MAX_VALUE, oldVal, mMaxValue);
        }
    }

    public enum Measure {
        HUMIDITY("%"),
        TEMPERATURE(StringExt.SYMBOL_CELSIUS),
        PH("pH"),
        EC("EC"),
        FLOW("L/s");

        public final String symbol;

        Measure(String symbol) {
            this.symbol = symbol;
        }
    }
}
