package com.netanel.irrigator_app.controls;


import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.netanel.irrigator_app.R;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 20/03/2021
 */

public class ExtCircularSeekBar extends CircularSeekBar implements IMultiStateView {
    private static final int[] STATE_EDITED = {R.attr.state_edited};
    private static final int[] STATE_ACTIVATED = {R.attr.state_activated};
    private boolean mIsEdited = false;
    private boolean mIsActivated = false;

    private ColorStateList mCircleProgressColorStates;

    public ExtCircularSeekBar(Context context) {
        super(context);
    }

    public ExtCircularSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtCircularSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init(AttributeSet attrs, int defStyle) {
        super.init(attrs, defStyle);

        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ExtCircularSeekBar, defStyle, 0);

        initExtAttributes(typedArray);

        typedArray.recycle();
    }

    protected void initExtAttributes(TypedArray attrArray) {

        boolean glowEnabled = attrArray.getBoolean(R.styleable.ExtCircularSeekBar_glow_enabled, true);

        if (!glowEnabled) {
            mCircleProgressGlowPaint = new Paint(mCircleProgressPaint);
        }
    }

    @Override
    protected void initAttributes(TypedArray attrArray) {
        super.initAttributes(attrArray);

        mCircleProgressColorStates = attrArray.getColorStateList(R.styleable.CircularSeekBar_circle_progress_color);
        mCircleProgressColor = getCurrentCircleProgressStateColor();
    }

    @Override
    protected void initPaints() {
        super.initPaints();
        mCircleProgressPaint.setColor(getCurrentCircleProgressStateColor());
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        mCircleProgressPaint.setColor(getCurrentCircleProgressStateColor());
    }

    private int getCurrentCircleProgressStateColor() {
        return mCircleProgressColorStates.getColorForState(
                getDrawableState(), mCircleProgressColorStates.getDefaultColor());
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        int[] baseState = super.onCreateDrawableState(extraSpace + 2);

        if (mIsEdited) {
            mergeDrawableStates(baseState, STATE_EDITED);
        }
        if (mIsActivated) {
            mergeDrawableStates(baseState, STATE_ACTIVATED);
        }

        return baseState;
    }

    @Override
    public void setActivated(boolean activated) {
        mIsActivated = activated;
    }

    @Override
    public void setEdited(boolean edited) {
        this.mIsEdited = edited;
    }

    public Boolean isStateEdited() {
        return mIsEdited;
    }
}