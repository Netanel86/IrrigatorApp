package com.netanel.irrigator_app;


import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.devadvance.circularseekbar.CircularSeekBar;
import com.google.android.material.tabs.TabLayout;

import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingConversion;
import androidx.transition.Visibility;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 15/12/2021
 */

public class GeneralBindingAdapters {

    @BindingAdapter("enabled")
    public static void setCircularSeekbarEnabled(CircularSeekBar seekBar, boolean enabled){
        seekBar.setIsTouchEnabled(enabled);
    }

    @BindingAdapter("enabled")
    public static void setTabLayoutEnabled(TabLayout tabView, boolean enabled) {
        ViewGroup tabLayout = (ViewGroup) tabView.getChildAt(0);
        tabLayout.setEnabled(enabled);
        for (int i = 0; i < tabLayout.getChildCount(); i++) {
            tabLayout.getChildAt(i).setEnabled(enabled);
        }
    }

    @BindingAdapter("src")
    public static void setImageResource(ImageView imageView, int resource) {
        imageView.setImageResource(resource);
    }

    @BindingAdapter("paddingInDp")
    public static void setPaddingInDp(View view, int paddingDp) {
        float scale = view.getResources().getDisplayMetrics().density;
        int paddingInPx = (int) (paddingDp * scale + 0.5f);
        view.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);
    }

    @BindingConversion
    public static int convertBooleanToVisibility(boolean visible) {
        return visible ? View.VISIBLE : View.GONE;
    }
}
