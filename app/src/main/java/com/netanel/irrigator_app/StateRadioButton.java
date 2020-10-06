package com.netanel.irrigator_app;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 16/09/2020
 */

public class StateRadioButton extends AppCompatRadioButton {

    private static final int[] STATE_ON = {R.attr.state_toggle_on};

    private boolean mCurrentState = false;


    public StateRadioButton(Context context) {
        super(context);
    }

    public StateRadioButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StateRadioButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        return mCurrentState ?
                mergeDrawableStates(super.onCreateDrawableState(extraSpace + 1), STATE_ON)
                : super.onCreateDrawableState(extraSpace);
    }

    public void setState(boolean state) {
        this.mCurrentState = state;
        this.refreshDrawableState();
    }

    public Boolean getState() {
        return mCurrentState;
    }
}
