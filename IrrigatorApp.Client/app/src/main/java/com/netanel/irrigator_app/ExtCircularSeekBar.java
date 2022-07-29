package com.netanel.irrigator_app;


import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;

import com.devadvance.circularseekbar.CircularSeekBar;

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
            mergeDrawableStates(baseState,STATE_ACTIVATED);
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
