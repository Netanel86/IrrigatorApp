package com.netanel.irrigator_app;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 24/09/2020
 */

public interface ManualFragContract {

    interface IPresenter {
        void onSeekBarProgressChanged(final int progress, boolean fromUser);

//        void onValveSelected(String valveId);

//        void onTimeScaleClicked(Scale time);

        void onSendCommand();
    }

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
