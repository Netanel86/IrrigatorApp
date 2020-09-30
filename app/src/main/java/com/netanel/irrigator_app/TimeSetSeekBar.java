package com.netanel.irrigator_app;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.devadvance.circularseekbar.CircularSeekBar;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 25/09/2020
 */

public class TimeSetSeekBar extends ConstraintLayout
        implements View.OnClickListener, CircularSeekBar.OnCircularSeekBarChangeListener {
    private CircularSeekBar.OnCircularSeekBarChangeListener seekBarChangeListener;

    private CircularSeekBar mSeekBar;
    private TextView mTvOnTime;

    // TODO: 25/09/2020 add zero textview under max
    private TextView mTextViewMax;
    private TextView mTextViewOneQuarter;
    private TextView mTextViewHalf;
    private TextView mTextViewThreeQuarter;

    public TimeSetSeekBar(@NonNull Context context) {
        super(context);
    }

    public TimeSetSeekBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.circular_seekbar_time_view, this, true);

        mSeekBar = findViewById(R.id.circular_seekbar);
        mTvOnTime = findViewById(R.id.tv_elapsed_time);
        mTextViewMax = findViewById(R.id.tv_time_max);
        mTextViewOneQuarter = findViewById(R.id.tv_time_quarter);
        mTextViewHalf = findViewById(R.id.tv_time_half);
        mTextViewThreeQuarter = findViewById(R.id.tv_time_three_quarter);

        mSeekBar.setOnSeekBarChangeListener(this);
        mTextViewThreeQuarter.setOnClickListener(this);
        mTextViewMax.setOnClickListener(this);
        mTextViewHalf.setOnClickListener(this);
        mTextViewOneQuarter.setOnClickListener(this);


        TypedArray array = getContext()
                .obtainStyledAttributes(attrs, R.styleable.TimeSetSeekBar, 0, 0);
        float textSize;


        try {
            mSeekBar.setMax(array.getInt(R.styleable.TimeSetSeekBar_max_value, 100));
            textSize = array.getDimension(R.styleable.TimeSetSeekBar_textSize, 14);
        } finally {
            array.recycle();
        }

        mTvOnTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        mTextViewOneQuarter.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        mTextViewHalf.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        mTextViewThreeQuarter.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        mTextViewMax.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        int maxSeconds = mSeekBar.getMax();
        mTextViewOneQuarter.setText(String.valueOf(getRelative(RelativeValue.ONE_QUARTER, maxSeconds)));
        mTextViewHalf.setText(String.valueOf(getRelative(RelativeValue.HALF, maxSeconds)));
        mTextViewThreeQuarter.setText(String.valueOf(getRelative(RelativeValue.THREE_QUARTER, maxSeconds)));
        mTextViewMax.setText(String.valueOf(getRelative(RelativeValue.MAX, maxSeconds)));

        mTvOnTime.setText(getCurrentProgressText(mSeekBar.getProgress()));
    }

    private int getRelative(RelativeValue relative, int value) {
        int rel;
        switch (relative) {
            case ONE_QUARTER:
                rel = (int)(value * 0.25 / 60);
                break;
            case HALF:
                rel = (int)(value * 0.5 / 60);
                break;
            case THREE_QUARTER:
                rel = (int)(value * 0.75 / 60);
                break;
            case MAX:
                rel = value  / 60;
                break;
            default: rel = 0;
        }
        return rel;
    }

    private enum RelativeValue {
        NONE,
        ONE_QUARTER,
        HALF,
        THREE_QUARTER,
        MAX
    }

    public void setOnChangedListener(CircularSeekBar.OnCircularSeekBarChangeListener listener) {
        this.seekBarChangeListener = listener;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_time_max:
                mSeekBar.setProgress(mSeekBar.getMax());
                break;
            case R.id.tv_time_three_quarter:
                mSeekBar.setProgress((int) (mSeekBar.getMax() * 0.75));
                break;
            case R.id.tv_time_half:
                mSeekBar.setProgress((int) (mSeekBar.getMax() * 0.5));
                break;
            case R.id.tv_time_quarter:
                mSeekBar.setProgress((int) (mSeekBar.getMax() * 0.25));
                break;
        }
    }

    @Override
    public void onProgressChanged(CircularSeekBar circularSeekBar, final int progress, boolean fromUser) {

        mTvOnTime.setText(getCurrentProgressText(progress));

        if (seekBarChangeListener != null) {
            seekBarChangeListener.onProgressChanged(circularSeekBar, progress, fromUser);
        }
    }

    public void setProgress(int progress) {
        mSeekBar.setProgress(progress);
    }

    private String getCurrentProgressText(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d %s",
                minutes, seconds, minutes > 0 ? "Minutes" : "Seconds");
    }

    @Override
    public void onStopTrackingTouch(CircularSeekBar seekBar) {
        if (seekBarChangeListener != null) {
            seekBarChangeListener.onStopTrackingTouch(seekBar);
        }
    }

    @Override
    public void onStartTrackingTouch(CircularSeekBar seekBar) {
        if (seekBarChangeListener != null) {
            seekBarChangeListener.onStartTrackingTouch(seekBar);
        }
    }
}
