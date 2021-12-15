package com.netanel.irrigator_app;


import com.netanel.irrigator_app.model.Sensor;

import java.util.Locale;

import androidx.databinding.Bindable;



/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 14/12/2021
 */

public class SensorViewModel extends ObservableViewModel {
    private static final int INTEGER = 0;
    private static final int DOUBLE = 1;

    private final Sensor mSensor;

    private String mTextFormat;

    private int mDrawable;

    private int mResolution;

    private int mValueType;

    public SensorViewModel(Sensor sensor) {
        mSensor = sensor;

        initListener();
        assignResources();
    }

    private void initListener() {
        mSensor.setOnPropertyChangedCallback(new PropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Object sender, int propertyId, Object oldValue, Object newValue) {
                switch (propertyId) {
                    case Sensor.PROP_MAX_VALUE:
                        notifyPropertyChanged(BR.maxProgress);
                    case Sensor.PROP_VALUE:
                        notifyPropertyChanged(BR.progress);
                        notifyPropertyChanged(BR.textValue);
                        break;

                    case Sensor.PROP_MEASURE:
                        notifyPropertyChanged(BR.textValue);
                        notifyPropertyChanged(BR.drawable);
                        break;
                }
            }
        });
    }

    private void assignResources() {
        switch (mSensor.getMeasures()) {
            case EC:
                mTextFormat = "%.1f";
                mResolution = 10;
                mValueType = DOUBLE;
                break;
            case PH:
                setDrawable(R.drawable.ic_ph_meter);
                mTextFormat = "%.1f";
                mResolution = 10;
                mValueType = DOUBLE;
                break;
            case HUMIDITY:
                setDrawable(R.drawable.ic_humidity_filled);
                mTextFormat = "%d";
                mResolution = 1;
                mValueType = INTEGER;
                break;
            case TEMPERATURE:
                setDrawable(R.drawable.ic_thermometer);
                mTextFormat = "%d";
                mResolution = 1;
                mValueType = INTEGER;
                break;
            case FLOW:
                setDrawable(R.drawable.ic_flow_meter);
                mTextFormat = "%.1f";
                mResolution = 10;
                mValueType = DOUBLE;
                break;
        }
    }

    @Bindable
    public String getTextValue() {
        String value;
        if (mValueType == DOUBLE) {
            value = String.format(Locale.getDefault(), mTextFormat, mSensor.getValue());
        } else {
            value = String.format(Locale.getDefault(), mTextFormat, (int)mSensor.getValue());
        }
        return value + mSensor.getMeasures().symbol;
    }

    @Bindable
    public int getDrawable() {
        return mDrawable;
    }

    public void setDrawable(int resource) {
        if(mDrawable != resource) {
            mDrawable = resource;
            notifyPropertyChanged(BR.drawable);
        }
    }

    @Bindable
    public int getMaxProgress() {
        return (int)(mSensor.getMaxValue() * mResolution);
    }

    @Bindable
    public int getProgress() {
        return (int)(mSensor.getValue() * mResolution);
    }
}
