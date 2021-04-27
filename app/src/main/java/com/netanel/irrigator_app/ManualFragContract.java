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
        int getSeekBarProgress();

        void setSeekBarMaxProgress(int maxProgress);

        void setSeekBarProgress(int progress);

        void setSeekBarEditedState(boolean edited);

        void setTimerText(String timeString);

        void setPowerButtonActiveState(boolean activated);

        void setPowerButtonEditedState(boolean edited);

        void setPowerButtonEnabled(boolean enabled);

        void setTitleText(String nameString);

        void setTimeScaleText(TimeScale timeScale, String timeString);

        void addTab(int tabId, String description, boolean showActiveBadge);

        void setTabBadge(int tabId, boolean showActiveBadge);

        void setTabDescription(Integer tabId, String description);

        void addHumiditySensorView(float currValue, float maxValue);

        void addTemperatureSensorView(float value, float maxValue);

        void addFlowSensorView(float value, float maxValue);

        void addPhSensorView(float value, float maxValue);

        void showMessage(String message);

        void switchToValveView();

        void runOnUiThread(Runnable runnable);

        void setUiEnabled(boolean focusable);
    }

    interface IPresenter {
        void onSeekBarProgressChanged(final int progress, boolean fromUser);

        void onTimeScaleClicked(TimeScale time);

        void onButtonPowerClicked();

        void onTabClicked(int tabId);

        void bindView(IView view);

        void onViewCreated();

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
