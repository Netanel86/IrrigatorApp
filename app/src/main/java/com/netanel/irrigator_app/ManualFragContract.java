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
        int addStateRadioButton(boolean valveState, String viewString);
        void updateStateRadioButton(int btnId, boolean newState);
        void showMessage(String message);
        void showValvePage(String name, boolean state, int duration, Valve valve);
    }

    interface IViewModel {
        void bindView(IView view);
        void onCreate();
        void onButtonCommandClicked();
        void onButtonAddClicked();
        void onStateRadioButtonClicked(int btnId);
    }
}
