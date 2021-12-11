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
    interface IView {
//        int getSelectedValveProgress();

//        void setSelectedValveProgress(int progress);

//        void setSeekBarEditedState(boolean edited);

//        void setPowerButtonEditedState(boolean edited);

        void setSendCommandEnabledState(boolean enabled);

        void addValve(String valveId, String description, boolean isOpen);

//        void setValveDescription(String valveId, String description);

        void addHumiditySensorView(float currValue, float maxValue);

        void addTemperatureSensorView(float value, float maxValue);

        void addFlowSensorView(float value, float maxValue);

        void addPhSensorView(float value, float maxValue);

        void showMessage(String message);

//        void switchToValveView();

        void runOnUiThread(Runnable runnable);

        void setUiEnabled(boolean focusable);

//        void showValve(String description, boolean isOpen, int maxDuration);

//        void setSelectedValveMaxProgress(int maxProgress);

//        void setValveOpen(String valveId, boolean isOpen);

//        void setSelectedValveEdited();
    }

    interface IPresenter {
        void onSeekBarProgressChanged(final int progress, boolean fromUser);

        void onValveSelected(String valveId);

        void onTimeScaleClicked(Scale time);

        void onSendCommand();

//        void onValveSelected(int tabId);

        void bindView(IView view);

        void onViewCreated();

        void onDestroy();
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
