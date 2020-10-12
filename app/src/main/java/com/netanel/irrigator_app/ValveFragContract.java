package com.netanel.irrigator_app;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 12/10/2020
 */

class ValveFragContract {
    interface  IView {
        void setSeekBarMaxProgress(int maxProgress);

        void setSeekBarProgress(int progress);

        int getSeekBarProgress();

        void setTimerText(String timeString);

        void setImageDrawableTint(int colorResource);

        void setTitleText(String nameString);

        void showMessage(String message);

        void setPredefinedTimeText(PredefinedTime predefinedTime, String timeString);
    }
    interface IPresenter {
        void bindView(IView view);

        void onCreateView();

        void onViewCreated();

        void onSeekBarProgressChanged(final int progress, boolean fromUser);

        void onPredefinedTimeClicked(PredefinedTime time);

        void onButtonSetClicked();
    }

    enum PredefinedTime {
        Zero,
        Quarter,
        Half,
        ThreeQuarters,
        Max
    }
}
