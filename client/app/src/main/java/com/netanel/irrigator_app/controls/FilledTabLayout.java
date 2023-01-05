package com.netanel.irrigator_app.controls;


import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 06/03/2021
 */

public class FilledTabLayout extends TabLayout {
    public FilledTabLayout(@NonNull Context context) {
        super(context);
    }
    public FilledTabLayout(@NonNull Context context, AttributeSet attrs) {
        super(context,attrs);
    }
    public FilledTabLayout(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context,attrs,defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        ViewGroup tabLayout = (ViewGroup)getChildAt(0);
        int tabCount = tabLayout.getChildCount();
        if(tabCount != 0) {
            int parentWidth = getContext().getResources().getDisplayMetrics().widthPixels;
            int tabMinWidth = parentWidth / tabCount;
            for(int i = 0; i < tabCount; i++) {
                tabLayout.getChildAt(i).setMinimumWidth(tabMinWidth);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
