package com.netanel.irrigator_app;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


import java.util.ArrayList;
import java.util.List;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 22/04/2021
 */

public class SensorsAdapter extends BaseAdapter {

    private final Context mContext;
    private final List<SensorViewHolder> mSensorViewHolders;

    public SensorsAdapter(Context context, List<SensorViewHolder> sensorViewHolders) {
        super();
        mSensorViewHolders = sensorViewHolders != null ?
                sensorViewHolders : new ArrayList<SensorViewHolder>();
        mContext = context;
    }

    public SensorsAdapter(Context context) {
        this(context,null);
    }

    public void addItem(float value, String formattedValue, int iconResId) {
        SensorViewHolder viewHolder = new SensorViewHolder(value, formattedValue, iconResId);
        mSensorViewHolders.add(viewHolder);
//        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mSensorViewHolders.size();
    }

    @Override
    public Object getItem(int i) {
        return mSensorViewHolders.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = new SensorView(mContext, null);
        }
        if (view instanceof SensorView) {
            SensorView sensorView = (SensorView) view;
            sensorView.setValueText(mSensorViewHolders.get(i).getFormattedValue());
            sensorView.setProgress(mSensorViewHolders.get(i).getIntegerValue());
            sensorView.setIcon(mSensorViewHolders.get(i).getIconResourceId());
        }
        return view;
    }

    public class SensorViewHolder {
        private final int mIconResourceId;
        private String mFormattedValue;
        private float mValue;

        public SensorViewHolder(float value, String formattedValue, int iconResourceId) {
            mIconResourceId = iconResourceId;
            mFormattedValue = formattedValue;
            mValue = value;
        }

        public int getIconResourceId() {
            return mIconResourceId;
        }

        public float getValue() {
            return mValue;
        }

        public void setValue(float value) {
            mValue = value;
        }

        public String getFormattedValue() {
            return mFormattedValue;
        }

        public void setFormattedValue(String formattedValue) {
            mFormattedValue =formattedValue;
        }

        public int getIntegerValue() {
            return (int)mValue;
        }
    }
}
