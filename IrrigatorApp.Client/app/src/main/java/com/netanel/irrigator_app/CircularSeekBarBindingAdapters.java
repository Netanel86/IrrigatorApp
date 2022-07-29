package com.netanel.irrigator_app;


import com.devadvance.circularseekbar.CircularSeekBar;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.databinding.adapters.ListenerUtil;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 26/10/2021
 */

public class CircularSeekBarBindingAdapters {
    @InverseBindingAdapter(attribute = "progress")
    public static int getProgress(CircularSeekBar seekBar) {
        return seekBar.getProgress();
    }

    @BindingAdapter(value = {"onProgressChanged", "progressAttrChanged"}, requireAll = false)
    public static void setProgressChangedListener(CircularSeekBar seekBar,
                                                  final OnProgressChanged onProgressChanged,
                                                  final InverseBindingListener progressAttrChanged) {
        seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {

                //InverseListener: notify only when change was prompted by the user(UI), to avoid an infinite loop
                if (fromUser && progressAttrChanged != null) {
                    progressAttrChanged.onChange();
                }

                if (onProgressChanged != null) {
                    onProgressChanged.onProgressChanged(progress, fromUser);
                }
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });
    }

    public interface OnProgressChanged {
        void onProgressChanged(int progress, boolean fromUser);
    }

}
