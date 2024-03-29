package com.netanel.irrigator_app;


import com.netanel.irrigator_app.model.Sensor;

import java.lang.reflect.Type;
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

public class SensorViewModel extends ObservableViewModel 
        implements PropertyChangedCallback{

    private final Sensor mSensor;

    private int mDrawable;

    private Resolution mResolution;

    public SensorViewModel(Sensor sensor) {
        mSensor = sensor;

        mSensor.setOnPropertyChangedCallback(this);
        assignResources();
    }

    @Bindable
    public String getTextValue() {
        String value;
        if (mResolution.type == Double.TYPE) {
            value = String.format(Locale.getDefault(), mResolution.format, mSensor.getValue());
        } else {
            value = String.format(Locale.getDefault(), mResolution.format, (int)mSensor.getValue());
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
        return (int)(mSensor.getMaxValue() * mResolution.resolution);
    }

    @Bindable
    public int getProgress() {
        return (int)(mSensor.getValue() * mResolution.resolution);
    }

    @Override
    protected void onCleared() {
        mSensor.clearOnPropertyChangedCallback();
        super.onCleared();
    }

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

    private void assignResources() {
        switch (mSensor.getMeasures()) {
            case EC:
                // TODO: 19/12/2021 add drawable to EC: Electrical Conductivity
                mResolution = Resolution.Double0x1;
                break;
            case PH:
                setDrawable(R.drawable.ic_ph_meter);
                mResolution = Resolution.Double0x1;
                break;
            case HUMIDITY:
                setDrawable(R.drawable.ic_humidity_filled);
                mResolution = Resolution.Integer;
                break;
            case TEMPERATURE:
                setDrawable(R.drawable.ic_thermometer);
                mResolution = Resolution.Integer;
                break;
            case FLOW:
                setDrawable(R.drawable.ic_flow_meter);
                mResolution = Resolution.Double0x1;
                break;
        }
    }

    public enum Resolution {
        Integer("%d", 1, java.lang.Integer.TYPE),
        Double0x1("%.1f", 10, Double.TYPE),
        Double0x2("%.2f", 100, Double.TYPE);

        public final String format;
        public final int resolution;
        public final Type type;

        Resolution(String format, int resolution, Type type) {
            this.format = format;
            this.resolution = resolution;
            this.type = type;
        }
    }
}
