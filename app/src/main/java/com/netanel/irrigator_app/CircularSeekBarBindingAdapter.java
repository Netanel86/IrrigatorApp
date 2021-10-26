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

public class CircularSeekBarBindingAdapter {
//    @BindingAdapter("progress")
//    public static void setProgress(StateCircularSeekBar seekBar, int progress) {
//        seekBar.setProgress(progress);
//    }
//
    @InverseBindingAdapter(attribute = "progress")
    public static int getProgress(StateCircularSeekBar seekBar) {
        return seekBar.getProgress();
    }

    @BindingAdapter(value = {"onProgressChanged", "progressAttrChanged"}, requireAll = false)
    public static void setProgressChangedListener(StateCircularSeekBar seekBar,
                                                  final OnProgressChanged onProgressChanged,
                                                  final InverseBindingListener progressAttrChanged) {
        seekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
                if (onProgressChanged != null) {
                    onProgressChanged.onProgressChanged(progress, fromUser);
                }

                //view notifies property had changed only if change was prompted by the user
                if (fromUser && progressAttrChanged != null) {
                        progressAttrChanged.onChange();
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
