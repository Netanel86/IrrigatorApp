package com.netanel.irrigator_app;


import java.util.List;

import androidx.databinding.Bindable;
import androidx.databinding.Observable;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 24/09/2020
 */

public interface IManualViewModel extends Observable {

    @Bindable
    int getMessageResource();

    @Bindable
    String getMessage();

    @Bindable
    Object[] getMessageArray();

    @Bindable
    List<ValveViewModel> getValves();

    @Bindable
    List<SensorViewModel> getSensors();

    @Bindable
    ValveViewModel getSelectedValve();

    @Bindable
    boolean getEnabled();

    void setRelativeProgress(double relativeProgress);

    void onTabValveSelected(ValveViewModel valveVm);

    void onSeekBarProgressChanged(final int progress, boolean fromUser);

    void onSendCommand();

    enum Scale {
        Zero(0),
        Quarter(0.25),
        Half(0.5),
        ThreeQuarters(0.75),
        Max(1);

        public final double value;
        Scale(double val) {
            value = val;
        }
    }
}
