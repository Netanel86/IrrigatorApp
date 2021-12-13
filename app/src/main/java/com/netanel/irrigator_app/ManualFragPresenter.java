package com.netanel.irrigator_app;

import android.app.Application;
import android.content.res.Resources;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.model.Sensor;
import com.netanel.irrigator_app.model.ValveCommand;
import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.services.StringExt;
import com.netanel.irrigator_app.services.connection.ConnectivityCallback;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.model.Valve;
import com.netanel.irrigator_app.services.connection.NetworkUtilities;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import androidx.databinding.Bindable;
import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.AndroidViewModel;

/**
 * <p></p>
 *{
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 23/09/2020
 */
// TODO: 12/12/2021 build a global data access class for all view models, across all fragments.

public class ManualFragPresenter extends AndroidViewModel
        implements ManualFragContract.IPresenter,
        ConnectivityCallback.IConnectivityChangedCallback,
        Observable {
    public static final int VIEW_EMPTY = 0;
    public static final int VIEW_VALVE = 1;
    private static final boolean EDITED = true;
    private static final boolean ENABLED = true;

    private final ConnectivityCallback mConnectivityChangedCallback;

    private final IDataBaseConnection mDb;


    @Override
    protected void onCleared() {
        super.onCleared();

        NetworkUtilities.unregisterConnectivityCallback(
                this.getApplication().getApplicationContext(), mConnectivityChangedCallback);

    }

    private int mActiveView;

    private int mMessageRes;
    private String mMessage;
    private Object[] mMessageArr;
    @Bindable
    public int getMessageRes() {
        return mMessageRes;
    }

    @Bindable
    public String getMessage() {
        return mMessage;
    }
    @Bindable
    public Object[] getMessageArr() {
        return mMessageArr;
    }

    public void setMessage(String message) {
        mMessage = message;
        notifyPropertyChanged(BR.message);
    }
    public void setMessage(int messageRes) {
        mMessageRes = messageRes;
        notifyPropertyChanged(BR.messageRes);
    }

    public void setMessage(Object[] compose) {
        mMessageArr = compose;
        notifyPropertyChanged(BR.messageArr);
    }

    public void setActiveView(int viewId) {
        mActiveView = viewId;
        notifyPropertyChanged(BR.activeView);
    }

    @Bindable
    public int getActiveView() {
        return mActiveView;
    }

    private Map<String, ValveViewModel> mValves;

    @Bindable
    public Map<String, ValveViewModel> getValveMap() {
        return mValves;
    }

    public void setValveMap(Map<String, ValveViewModel> valveMap) {
        mValves = valveMap;
        notifyPropertyChanged(BR.valveMap);

        setActiveView(VIEW_VALVE);
    }

    private ValveViewModel mSelectedValve;

    @Bindable
    public ValveViewModel getSelectedValve() {
        return mSelectedValve;
    }

    public void setSelectedValve(ValveViewModel valve) {
        mSelectedValve = valve;
        notifyPropertyChanged(BR.selectedValve);
    }


    private final ProgressTimer mTimer;

    public ManualFragPresenter(Application app) {
        super(app);
        mDb = AppServices.getInstance().getDbConnection();
        mTimer = new ProgressTimer();
        mConnectivityChangedCallback = new ConnectivityCallback(this, app.getApplicationContext());
        mHandler = new Handler(Looper.getMainLooper());

        NetworkUtilities.registerConnectivityCallback(
                app.getApplicationContext(), mConnectivityChangedCallback);

        if (mValves == null) {
            if (NetworkUtilities.isOnline(this.getApplication())) {
                fetchValves();
            } else {
                setMessage(R.string.msg_no_connection);
            }
        }

        testSensors();
    }


    private void testSensors() {
        List<Sensor> sensors = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Sensor s1 = new Sensor(Sensor.Measure.HUMIDITY, 100);
            s1.setValue((float) (s1.getMaxValue() / 2) * (i + 1));
            sensors.add(s1);
        }
        for (int i = 0; i < 2; i++) {
            Sensor s1 = new Sensor(Sensor.Measure.TEMPERATURE, 99);
            s1.setValue((float) (s1.getMaxValue() / 2) * (i + 1));
            sensors.add(s1);
        }
        for (int i = 0; i < 2; i++) {
            Sensor s1 = new Sensor(Sensor.Measure.FLOW, 9);
            s1.setValue((float) (s1.getMaxValue() / 2) * (i + 1));
            sensors.add(s1);
        }
        for (int i = 0; i < 2; i++) {
            Sensor s1 = new Sensor(Sensor.Measure.PH, 15);
            s1.setValue((float) (s1.getMaxValue() / 2) * (i + 1));
            sensors.add(s1);
        }

//        for (Sensor s :
//                sensors) {
//            switch (s.getMeasureType()) {
//                case HUMIDITY:
//                    mView.addHumiditySensorView(s.getValue(), s.getMaxValue());
//                    break;
//                case TEMPERATURE:
//                    mView.addTemperatureSensorView(s.getValue(), s.getMaxValue());
//                    break;
//                case FLOW:
//                    mView.addFlowSensorView(s.getValue(), s.getMaxValue());
//                    break;
//                case PH:
//                    mView.addPhSensorView(s.getValue(), s.getMaxValue());
//                    break;
//            }
//        }

    }



    public void fetchValves() {
        mDb.getValves(new IDataBaseConnection.TaskListener<Map<String, Valve>>() {
            @Override
            public void onComplete(Map<String, Valve> answer, Exception ex) {
                if (ex != null) {
                    setMessage(ex.getMessage());
                } else if (answer != null) {
                    if (!answer.isEmpty()) {
                        LinkedHashMap<String, ValveViewModel> valvesMap = new LinkedHashMap<>();
                        ArrayList<Valve> valves = new ArrayList<>(answer.values());
                        for (int i = 0; i < valves.size(); i++) {
                            initValveDbListener(valves.get(i));

                            ValveViewModel valveVm = new ValveViewModel(valves.get(i));

                            valvesMap.put(valveVm.getId(), valveVm);
                        }
                        setValveMap(valvesMap);
                        setMessage(R.string.msg_loaded_successful);
                    } else {
                        setMessage(R.string.error_no_valves);
                    }
                } else {
                    setMessage(R.string.msg_returned_empty_result);
                }
            }
        });
    }

    private void initValveDbListener(@NotNull final Valve valve) {
        mDb.addOnValveChangedListener(valve.getId(), new IDataBaseConnection.OnDataChangedListener<Valve>() {
            @Override
            public void onDataChanged(Valve changedObject, Exception ex) {
                if (ex != null) {
                    setMessage(ex.getMessage());
                }
                valve.update(changedObject);
            }
        });
    }

    @Override
    public void onConnectivityChanged(final boolean isConnected) {
        runOnUiThread(() -> {
            if (isConnected) {
                if (mValves == null || mValves.isEmpty()) {
                    setMessage(new Object[]{R.string.msg_connection_resumed,
                            StringExt.COMMA, StringExt.SPACE,
                            R.string.msg_loading_valves});
                    fetchValves();
                } else {
                    setMessage(R.string.msg_connection_resumed);
                    setUiEnabled(ENABLED);
                }
            } else {
                setMessage(R.string.msg_connection_lost);
                mTimer.stopIfActive();
                setUiEnabled(!ENABLED);
            }
        });

    }

    private boolean isUiEnabled;

    @Bindable
    public boolean getIsUiEnabled() {
        return isUiEnabled;
    }
    public void setUiEnabled(boolean enabled) {
        isUiEnabled = enabled;
        if (enabled) {
            setActiveView(VIEW_VALVE);
        } else {
            setActiveView(VIEW_EMPTY);
        }
        notifyPropertyChanged(BR.isUiEnabled);
    }
    private final Handler mHandler;
    private void runOnUiThread(Runnable action) {
        mHandler.post(action);
    }
    @Override
    public void onSeekBarProgressChanged(final int progress, boolean fromUser) {
        if (fromUser || mIsFromScaleButton) {
            mIsFromScaleButton = false;
            onProgressChangedByUser();
        } else if (!hasProgressChangedByTimer()) {
            resetTimer();
        }
    }

    private boolean hasProgressChangedByTimer() {
        return mTimer.isActive() && mTimer.getProgress() == mSelectedValve.getProgress();
    }

    private boolean mIsFromScaleButton;

    public void setRelativeProgress(double relativeProgress) {
        mSelectedValve.setProgress((int) (mSelectedValve.getMaxDuration() * relativeProgress));
        mIsFromScaleButton = true;
    }

    private void onProgressChangedByUser() {
        mTimer.stopIfActive();

        if (mSelectedValve.getProgress() != mSelectedValve.getTimeLeft()) {
            mSelectedValve.setEdited(EDITED);
        } else {
            mSelectedValve.resetViewStates();

            if (mSelectedValve.isOpen()) {
                if (mSelectedValve.getEditedProgress() == 0) {
                    setMessage(R.string.tip_use_power_button);
                }

                mTimer.startCountDown();
            }
        }
    }

    private void resetTimer() {
        mTimer.stopIfActive();

        if (mSelectedValve.isOpen() && !mSelectedValve.isEdited()) {
            mTimer.startCountDown();
        }
    }



    @Override
    public void onValveSelected(String valveId) {
//        mSelectedValve = mValves.get(valveId);
//        if (mSelectedValve != null) {
//            mView.showValve(mSelectedValve.getDescription(),
//                    mSelectedValve.isOpen(), mSelectedValve.getMaxDuration());
//            updateSelectedValveProgressView();
//        } else {
//            mView.showMessage(mResources.getString(R.string.error_loading_valve));
//        }
    }

    public void onTabValveSelected(ValveViewModel valveVm) {
        setSelectedValve(valveVm);
    }


    @Override
    public void onTimeScaleClicked(ManualFragContract.Scale time) {
        switch (time) {
            case Zero:
//                mView.setSelectedValveProgress(0);
                break;
            case Quarter:
//                mView.setSelectedValveProgress((int) (mSelectedValve.getMaxDuration() * 0.25));
                break;
            case Half:
//                mView.setSelectedValveProgress((int) (mSelectedValve.getMaxDuration() * 0.5));
                break;
            case ThreeQuarters:
//                mView.setSelectedValveProgress((int) (mSelectedValve.getMaxDuration() * 0.75));
                break;
            case Max:
                setRelativeProgress(ManualFragContract.Scale.Max.value);
//                mView.setSelectedValveProgress((mSelectedValve.getMaxDuration()));
                break;
        }

        onProgressChangedByUser();
    }

    @Override
    public void onSendCommand() {
        Command cmnd = null;

        if (mSelectedValve.isEdited()) {
            if (mSelectedValve.getEditedProgress() != 0) {
                // TODO: 12/12/2021 implement Command with builder pattern
                cmnd = new ValveCommand(mSelectedValve.getIndex(), mSelectedValve.getEditedProgress());
            } else {
                cmnd = new ValveCommand(mSelectedValve.getIndex(), !Valve.OPEN);
            }
        } else if (mSelectedValve.isOpen()) {
            cmnd = new ValveCommand(mSelectedValve.getIndex(), !Valve.OPEN);
        }

        if (cmnd != null) {
            mSelectedValve.removeViewState(ValveViewModel.StateFlag.ENABLED);
            AppServices.getInstance().getDbConnection().addCommand(cmnd, new IDataBaseConnection.TaskListener<Command>() {
                @Override
                public void onComplete(Command answer, Exception ex) {
                    if (ex != null) {
                        setMessage(ex.getMessage());
                        mSelectedValve.addViewState(ValveViewModel.StateFlag.ENABLED);
                    } else if (answer != null) {
                        Log.println(Log.INFO, "Command", "registered with id:" + answer.getId());
                        setMessage(R.string.command_sent);
                    }
                }
            });
        }
    }

    private PropertyChangeRegistry mCallBacks;

    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        if (mCallBacks == null) {
            mCallBacks = new PropertyChangeRegistry();
        }

        mCallBacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        if (mCallBacks != null) {
            mCallBacks.remove(callback);
        }
    }

    private void notifyPropertyChanged(int fieldId) {
        if (mCallBacks != null) {
            mCallBacks.notifyCallbacks(this, fieldId, null);
        }
    }

    public class ProgressTimer {
        private CountDownTimer mCountDownTimer;
        private Timer mElapsedTimer;
        private boolean mIsActive = false;
        public boolean isActive() {
            return mIsActive;
        }
        public int getProgress() {
            return mProgress;
        }
        private int mProgress;

        public void startCountDown() {
            mCountDownTimer = new CountDownTimer(
                    (long) mSelectedValve.getTimeLeft() * 1000,
                    1000) {
                @Override
                public void onTick(long l) {
                    mSelectedValve.setProgress(mProgress = (int) l / 1000);
                }

                @Override
                public void onFinish() {
                    mProgress = 0;
                    mSelectedValve.setProgress(0);
                    mIsActive = false;
                }
            }.start();
            mIsActive = true;
        }

        public void startElapsedTimer() {
            mElapsedTimer = new Timer();
            mElapsedTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            long diffInSec = TimeUnit.MILLISECONDS.toSeconds(
                                    Calendar.getInstance().getTime().getTime() -
                                            mSelectedValve.getLastOpen().getTime());
                            mSelectedValve.setProgress((int) diffInSec);
                        }
                    });
                }
            }, 0, 1000);
            mIsActive = true;
        }

        public void stopIfActive() {
            if (mIsActive) {
                if (mCountDownTimer != null) mCountDownTimer.cancel();
                if (mElapsedTimer != null) mElapsedTimer.cancel();
                mIsActive = false;
            }
        }
    }
}
