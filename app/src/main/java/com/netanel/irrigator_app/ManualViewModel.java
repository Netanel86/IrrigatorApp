package com.netanel.irrigator_app;

import android.app.Application;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * <p></p>
 *{
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 23/09/2020
 */
// TODO: 12/12/2021 build a global data access class for all view models, across all fragments.
// TODO: 15/12/2021 global data access object should handle subscribing to db,
//  would have two methods for data requests, onComplete and onError.

public class ManualViewModel extends ObservableViewModel
        implements IManualViewModel,
        ConnectivityCallback.IConnectivityChangedCallback{

    private static final boolean EDITED = true;
    private static final boolean ENABLED = true;

    public static final int VIEW_EMPTY = 0;
    public static final int VIEW_VALVE = 1;

    private final ConnectivityCallback mConnectivityChangedCallback;

    private final IDataBaseConnection mDb;

    private final Handler mHandler;

    private final ProgressTimer mTimer;

    private String mMessage;

    private int mMessageResource;

    private Object[] mMessageArr;

    private int mActiveView;

    private boolean isEnabled;

    private boolean mIsScaleButtonChange;

    private List<ValveViewModel> mValves;

    private List<SensorViewModel> mSensors;

    private ValveViewModel mSelectedValve;

    public ManualViewModel(Application application) {
        super(application);
        mDb = AppServices.getInstance().getDbConnection();
        mTimer = new ProgressTimer();
        mConnectivityChangedCallback = new ConnectivityCallback(this, getApplication().getApplicationContext());
        mHandler = new Handler(Looper.getMainLooper());

        NetworkUtilities.registerConnectivityCallback(
                getApplication().getApplicationContext(), mConnectivityChangedCallback);

        if (mValves == null) {
            if (NetworkUtilities.isOnline(this.getApplication())) {
                fetchValves();
            } else {
                setMessageResource(R.string.msg_no_connection);
            }
        }

        testSensors();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        NetworkUtilities.unregisterConnectivityCallback(
                this.getApplication().getApplicationContext(), mConnectivityChangedCallback);

    }

    @Override
    public void onConnectivityChanged(final boolean isConnected) {
        runOnUiThread(() -> {
            if (isConnected) {
                if (mValves == null || mValves.isEmpty()) {
                    setMessageArray(new Object[]{R.string.msg_connection_resumed,
                            StringExt.COMMA, StringExt.SPACE,
                            R.string.msg_loading_valves});
                    fetchValves();
                } else {
                    setMessageResource(R.string.msg_connection_resumed);
                    setEnabled(ENABLED);
                }
            } else {
                setMessageResource(R.string.msg_connection_lost);
                mTimer.stopIfActive();
                setEnabled(!ENABLED);
            }
        });

    }

    @Override
    public int getMessageResource() {
        return mMessageResource;
    }

    public void setMessageResource(int resource) {
        mMessageResource = resource;
        notifyPropertyChanged(BR.messageResource);
    }

    @Override
    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
        notifyPropertyChanged(BR.message);
    }

    @Override
    public Object[] getMessageArray() {
        return mMessageArr;
    }

    public void setMessageArray(Object[] compose) {
        mMessageArr = compose;
        notifyPropertyChanged(BR.messageArray);
    }

    @Override
    public int getActiveView() {
        return mActiveView;
    }

    public void setActiveView(int viewId) {
        mActiveView = viewId;
        notifyPropertyChanged(BR.activeView);
    }

    @Override
    public List<ValveViewModel> getValves() {
        return mValves;
    }

    public void setValves(List<ValveViewModel> valves) {
        mValves = valves;
        notifyPropertyChanged(BR.valves);

        setActiveView(VIEW_VALVE);
    }

    @Override
    public List<SensorViewModel> getSensors() {
        return mSensors;
    }

    public void setSensors(List<SensorViewModel> mSensors) {
        this.mSensors = mSensors;
        notifyPropertyChanged(BR.sensors);
    }

    @Override
    public ValveViewModel getSelectedValve() {
        return mSelectedValve;
    }

    public void setSelectedValve(ValveViewModel valve) {
        mSelectedValve = valve;
        notifyPropertyChanged(BR.selectedValve);
    }

    @Override
    public boolean getEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        if (enabled) {
            setActiveView(VIEW_VALVE);
        } else {
            setActiveView(VIEW_EMPTY);
        }
        notifyPropertyChanged(BR.enabled);
    }

    @Override
    public void setRelativeProgress(double relativeProgress) {
        mSelectedValve.setProgress((int) (mSelectedValve.getMaxDuration() * relativeProgress));
        mIsScaleButtonChange = true;
    }

    public void fetchValves() {
        mDb.getValves(new IDataBaseConnection.TaskListener<List<Valve>>() {
            @Override
            public void onComplete(List<Valve> answer, Exception ex) {
                if (ex != null) {
                    setMessage(ex.getMessage());
                } else if (answer != null) {
                    if (!answer.isEmpty()) {
                        LinkedList<ValveViewModel> valves = new LinkedList<>();
                        for (Valve valve :
                                answer) {

                            initValveDbListener(valve);
                            ValveViewModel valveVm = new ValveViewModel(valve);
                            valves.add(valveVm);
                        }

                        setValves(valves);
                        setMessageResource(R.string.msg_loaded_successful);
                    } else {
                        setMessageResource(R.string.error_no_valves);
                    }
                } else {
                    setMessageResource(R.string.msg_returned_empty_result);
                }
            }
        });
    }

    @Override
    public void onTabValveSelected(ValveViewModel valveVm) {
        setSelectedValve(valveVm);
    }

    @Override
    public void onSeekBarProgressChanged(final int progress, boolean fromUser) {
        if (fromUser || mIsScaleButtonChange) {
            mIsScaleButtonChange = false;
            onProgressChangedByUser();
        } else if (!hasProgressChangedByTimer()) {
            resetTimer();
        }
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
            mSelectedValve.removeViewState(ValveViewModel.State.ENABLED);
            AppServices.getInstance().getDbConnection().addCommand(cmnd, new IDataBaseConnection.TaskListener<Command>() {
                @Override
                public void onComplete(Command answer, Exception ex) {
                    if (ex != null) {
                        setMessage(ex.getMessage());
                        mSelectedValve.addViewState(ValveViewModel.State.ENABLED);
                    } else if (answer != null) {
                        Log.println(Log.INFO, "Command", "registered with id:" + answer.getId());
                        setMessageResource(R.string.command_sent);
                    }
                }
            });
        }
    }

    private void testSensors() {
        List<SensorViewModel> sensors = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Sensor s1 = new Sensor(Sensor.Measure.HUMIDITY, 100);
            s1.setValue((float) (s1.getMaxValue() / 2) * (i + 1));
            SensorViewModel viewModel = new SensorViewModel(s1);
            sensors.add(viewModel);
        }
        for (int i = 0; i < 2; i++) {
            Sensor s1 = new Sensor(Sensor.Measure.TEMPERATURE, 99);
            s1.setValue((float) (s1.getMaxValue() / 2) * (i + 1));
            SensorViewModel viewModel = new SensorViewModel(s1);
            sensors.add(viewModel);
        }
        for (int i = 0; i < 2; i++) {
            Sensor s1 = new Sensor(Sensor.Measure.FLOW, 9);
            s1.setValue(0.5);
            SensorViewModel viewModel = new SensorViewModel(s1);
            sensors.add(viewModel);
        }
        for (int i = 0; i < 2; i++) {
            Sensor s1 = new Sensor(Sensor.Measure.PH, 15);
            s1.setValue((float) (s1.getMaxValue() / 2) * (i + 1));
            SensorViewModel viewModel = new SensorViewModel(s1);
            sensors.add(viewModel);
        }

        setSensors(sensors);
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

    private void runOnUiThread(Runnable action) {
        mHandler.post(action);
    }

    private boolean hasProgressChangedByTimer() {
        return mTimer.isActive() && mTimer.getProgress() == mSelectedValve.getProgress();
    }

    private void onProgressChangedByUser() {
        mTimer.stopIfActive();

        if (mSelectedValve.getProgress() != mSelectedValve.getTimeLeft()) {
            mSelectedValve.setEdited(EDITED);
        } else {
            mSelectedValve.resetViewStates();

            if (mSelectedValve.isOpen()) {
                if (mSelectedValve.getEditedProgress() == 0) {
                    setMessageResource(R.string.tip_use_power_button);
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
