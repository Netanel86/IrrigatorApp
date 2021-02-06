package com.netanel.irrigator_app;


import com.netanel.irrigator_app.model.Valve;

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

        void setImageDrawableTint(int colorResource);

        void setTitleText(String nameString);

        void setPredefinedTimeText(PredefinedTime predefinedTime, String timeString);

        int addStateRadioButton(boolean valveState, String viewString);

        void updateStateRadioButton(int btnId, boolean newState);

        void showMessage(String message);

        void switchToValveView();
    }

    interface IPresenter {
        void onSeekBarProgressChanged(final int progress, boolean fromUser);

        void onPredefinedTimeClicked(PredefinedTime time);

        void onButtonSetClicked();

        void bindView(IView view);

        void onCreate();

        void onStateRadioButtonClicked(int btnId);
    }

    enum PredefinedTime {
        Zero,
        Quarter,
        Half,
        ThreeQuarters,
        Max
    }
}
