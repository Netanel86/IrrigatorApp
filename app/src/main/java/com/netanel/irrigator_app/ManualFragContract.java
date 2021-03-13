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

        void setPowerIconActiveState(boolean isActive);

        void setPowerIconEditedState(boolean isEdited);

        void setTitleText(String nameString);

        void setTimeScaleText(TimeScale timeScale, String timeString);

        void addTab(int tabId, String description, boolean showActiveBadge);

        void setTabBadge(int tabId, boolean showActiveBadge);

        void setTabDescription(Integer tabId, String description);

        void showMessage(String message);

        void switchToValveView();

        void runOnUiThread(Runnable runnable);

        void setUiEnabled(boolean focusable);
    }

    interface IPresenter {
        void onSeekBarProgressChanged(final int progress, boolean fromUser);

        void onTimeScaleClicked(TimeScale time);

        void onButtonPowerClicked();

        void bindView(IView view);

        void onViewCreated();

        void onTabClicked(int tabId);

        void onDestroy();
    }

    enum TimeScale {
        Zero(0),
        Quarter(0.25),
        Half(0.5),
        ThreeQuarters(0.75),
        Max(1);

        public final double value;
        TimeScale(double val) {
            value = val;
        }
    }
}
