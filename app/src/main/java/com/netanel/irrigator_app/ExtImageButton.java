package com.netanel.irrigator_app;


import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 22/02/2021
 */

public class ExtImageButton extends AppCompatImageButton
        implements IMultiStateView {

    private static final int[] STATE_ACTIVATED = {R.attr.state_activated};
    private static final int[] STATE_EDITED = {R.attr.state_edited};
    private boolean mIsActivated = false;
    private boolean mIsEdited = false;


    public ExtImageButton(Context context) {
        super(context);
    }

    public ExtImageButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtImageButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        int[] baseState = super.onCreateDrawableState(extraSpace + 2);

        if (mIsActivated) {
            mergeDrawableStates(baseState, STATE_ACTIVATED);
        }
        if (mIsEdited) {
            mergeDrawableStates(baseState, STATE_EDITED);
        }
        return baseState;
    }

    @Override
    public void setActivated(boolean activated) {
        this.mIsActivated = activated;
        this.refreshDrawableState();
    }

    public Boolean isStateActivated() {
        return mIsActivated;
    }

    @Override
    public void setEdited(boolean edited) {
        this.mIsEdited = edited;
        this.refreshDrawableState();
    }

    public Boolean isStateEdited() {
        return mIsEdited;
    }
}
