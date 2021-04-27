package com.netanel.irrigator_app;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

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

    void setPaddingInDp(int dpLeft, int dpTop, int dpRight, int dpBottom) {
        float scale = getResources().getDisplayMetrics().density;
        int leftInPx = (int) (dpLeft * scale + 0.5f);
        int topInPx = (int) (dpTop * scale + 0.5f);
        int rightInPx =(int) (dpRight * scale + 0.5f);
        int bottomInPx = (int) (dpBottom * scale + 0.5f);
        this.setPadding(leftInPx, topInPx, rightInPx, bottomInPx);
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

    public void setMaxProgress(int maxProgress) {
        mSeekBarSensor.setMax(maxProgress);
    }

    public void setIcon(int resourceId) {
        mIvIcon.setImageResource(resourceId);
    }

    public void setViewDimensions(int widthAndHeight) {
        ViewGroup.LayoutParams params = mSeekBarSensor.getLayoutParams();
        params.width = widthAndHeight;
        params.height = widthAndHeight;
        mSeekBarSensor.setLayoutParams(params);
        mSeekBarSensor.setCircleRadius((float) widthAndHeight / 2);
    }
}
