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

public class StateImageButton extends AppCompatImageButton {

    private static final int[] STATE_ACTIVATED = {R.attr.state_activated};
    private static final int[] STATE_EDITED = {R.attr.state_edited};
    private boolean mIsActivated = false;
    private boolean mIsEdited = false;


    public StateImageButton(Context context) {
        super(context);
    }

    public StateImageButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StateImageButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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

    public void setActivatedState(boolean state) {
        this.mIsActivated = state;
        this.refreshDrawableState();
    }


    public Boolean getActivatedState() {
        return mIsActivated;
    }

    public void setEdited(boolean state) {
        this.mIsEdited = state;
        this.refreshDrawableState();
    }

    public Boolean getEdited() {
        return mIsEdited;
    }
}
