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

    private String mValueFormat;

    private int mIconRes;

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
                        notifyPropertyChanged(BR.valueText);
                        break;

                    case Sensor.PROP_MEASURE:
                        notifyPropertyChanged(BR.valueText);
                        notifyPropertyChanged(BR.iconRes);
                        break;
                }
            }
        });
    }

    private void assignResources() {
        switch (mSensor.getMeasures()) {
            case EC:
                mValueFormat = "%.1f";
                mResolution = 10;
                mValueType = DOUBLE;
                break;
            case PH:
                setIconRes(R.drawable.ic_ph_meter);
                mValueFormat = "%.1f";
                mResolution = 10;
                mValueType = DOUBLE;
                break;
            case HUMIDITY:
                setIconRes(R.drawable.ic_humidity_filled);
                mValueFormat = "%d";
                mResolution = 1;
                mValueType = INTEGER;
                break;
            case TEMPERATURE:
                setIconRes(R.drawable.ic_thermometer);
                mValueFormat = "%d";
                mResolution = 1;
                mValueType = INTEGER;
                break;
            case FLOW:
                setIconRes(R.drawable.ic_flow_meter);
                mValueFormat = "%.1f";
                mResolution = 10;
                mValueType = DOUBLE;
                break;
        }
    }

    @Bindable
    public String getValueText() {
        String value;
        if (mValueType == DOUBLE) {
            value = String.format(Locale.getDefault(), mValueFormat, mSensor.getValue());
        } else {
            value = String.format(Locale.getDefault(), mValueFormat, (int)mSensor.getValue());
        }
        return value + mSensor.getMeasures().symbol;
    }

    @Bindable
    public int getIconRes() {
        return mIconRes;
    }

    public void setIconRes(int resId) {
        if(mIconRes != resId) {
            mIconRes = resId;
            notifyPropertyChanged(BR.iconRes);
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
