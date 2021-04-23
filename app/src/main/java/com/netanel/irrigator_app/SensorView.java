package com.netanel.irrigator_app;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.google.android.material.textview.MaterialTextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 21/09/2020
 */
//"added title to manual page, refactored valve CardView layout, added sensors CardView, implemented basic sensor model for testing."
public class SensorView extends ConstraintLayout {

    private ImageView mIvIcon;
    private MaterialTextView mTvValue;
    private CircularSeekBar mSeekBarSensor;

    public SensorView(Context context) {
        super(context);
    }

    public SensorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sensor_view, this, true);

        mIvIcon = findViewById(R.id.iv_sensor);
        mTvValue = findViewById(R.id.tv_sensor_value);
        mSeekBarSensor = findViewById(R.id.seekbar_sensor);
        mSeekBarSensor.setIsTouchEnabled(false);

        if (attrs != null) {
            initAttr(attrs);
        }
    }

    private void initAttr(@Nullable AttributeSet attrs) {
        TypedArray array= getContext().obtainStyledAttributes(attrs,R.styleable.SensorView,0,0);
        try {
            mIvIcon.setImageResource(array.getResourceId(R.styleable.SensorView_sensor_drawable,0));
        }finally {
            array.recycle();
        }
    }

    public void setValueText(String valueText) {
        mTvValue.setText(valueText);
    }

    public void setProgress(int progress) {
        mSeekBarSensor.setProgress(progress);
    }

    public void setIcon(int resourceId) {
        mIvIcon.setImageResource(resourceId);
    }
}
