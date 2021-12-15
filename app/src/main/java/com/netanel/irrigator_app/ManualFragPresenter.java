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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import androidx.databinding.Bindable;

/**
 * <p></p>
 *{
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 23/09/2020
 */
// TODO: 12/12/2021 build a global data access class for all view models, across all fragments.

public class ManualFragPresenter extends ObservableViewModel
        implements ManualFragContract.IPresenter,
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

    private int mMessageRes;

    private Object[] mMessageArr;

    private int mActiveView;

    private boolean isEnabled;

    private boolean mIsFromScaleButton;

    private List<ValveViewModel> mValves;

    private List<SensorViewModel> mSensors;

    private ValveViewModel mSelectedValve;

    public ManualFragPresenter(Application application) {
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
                setMessage(R.string.msg_no_connection);
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

    @Bindable
    public int getMessageRes() {
        return mMessageRes;
    }

    public void setMessage(int messageRes) {
        mMessageRes = messageRes;
        notifyPropertyChanged(BR.messageRes);
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

    @Bindable
    public List<ValveViewModel> getValveMap() {
        return mValves;
    }


    @Bindable
    public List<SensorViewModel> getSensors() {
        return mSensors;
    }

    public void setSensors(List<SensorViewModel> mSensors) {
        this.mSensors = mSensors;
        notifyPropertyChanged(BR.sensors);
    }

    public void setValves(List<ValveViewModel> valves) {
        mValves = valves;
        notifyPropertyChanged(BR.valveMap);

        setActiveView(VIEW_VALVE);
    }


    @Bindable
    public ValveViewModel getSelectedValve() {
        return mSelectedValve;
    }

    public void setSelectedValve(ValveViewModel valve) {
        mSelectedValve = valve;
        notifyPropertyChanged(BR.selectedValve);
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
                    setEnabled(ENABLED);
                }
            } else {
                setMessage(R.string.msg_connection_lost);
                mTimer.stopIfActive();
                setEnabled(!ENABLED);
            }
        });

    }

    @Bindable
    public boolean getIsEnabled() {
        return isEnabled;
    }
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        if (enabled) {
            setActiveView(VIEW_VALVE);
        } else {
            setActiveView(VIEW_EMPTY);
        }
        notifyPropertyChanged(BR.isEnabled);
    }
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

    public void onTabValveSelected(ValveViewModel valveVm) {
        setSelectedValve(valveVm);
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
