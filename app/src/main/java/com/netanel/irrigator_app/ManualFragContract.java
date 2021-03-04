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
        void setSeekBarMaxProgress(int maxProgress);

        void setSeekBarProgress(int progress);

        int getSeekBarProgress();

        void setTimerText(String timeString);

        void setPowerIconActivatedState(boolean state);

        void setPowerIconEditedState(boolean isEdited);

        void setTitleText(String nameString);

        void setPredefinedTimeText(PredefinedTime predefinedTime, String timeString);

        int addStateRadioButton(boolean valveState, String viewString);

        void setRadioButtonState(int btnId, boolean newState);

        void showMessage(String message);

        void switchToValveView();

        void runOnUiThread(Runnable runnable);

        void setUiEnabled(boolean focusable);
    }

    interface IPresenter {
        void onSeekBarProgressChanged(final int progress, boolean fromUser);

        void onPredefinedTimeClicked(PredefinedTime time);

        void onButtonPowerClicked();

        void bindView(IView view);

        void onViewCreated();

        void onStateRadioButtonClicked(int btnId);

        void onConnectionChanged(boolean isConnected);
    }

    enum PredefinedTime {
        Zero(0),
        Quarter(0.25),
        Half(0.5),
        ThreeQuarters(0.75),
        Max(1);

        public final double value;
        PredefinedTime(double val) {
            value = val;
        }
    }
}
