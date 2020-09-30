package com.netanel.irrigator_app;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 21/09/2020
 */

public class SensorView extends LinearLayout {

    private ImageView mIcon;
    private TextView mValue;

    public SensorView(Context context) {
        super(context);
    }

    public SensorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sensor_view, this, true);

        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER);

        mIcon = findViewById(R.id.sensor_tab_icon);
        mValue = findViewById(R.id.sensor_tab_value);

        initAttr(attrs);

    }

    private void initAttr(@Nullable AttributeSet attrs) {
        TypedArray array= getContext().obtainStyledAttributes(attrs,R.styleable.SensorView,0,0);
        try {
            mIcon.setImageResource(array.getResourceId(R.styleable.SensorView_sensor_drawable,0));
            mIcon.setScaleType(ImageView.ScaleType.FIT_XY);
        }finally {
            array.recycle();
        }
    }

    public void setValue(String value) {
        mValue.setText(value);
    }
}
