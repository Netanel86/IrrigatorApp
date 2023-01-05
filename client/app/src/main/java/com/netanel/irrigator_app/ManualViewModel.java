package com.netanel.irrigator_app;

import android.app.Application;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.netanel.irrigator_app.model.Actions;
import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.model.Module;
import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.connection.Repository;
import com.netanel.irrigator_app.services.StringExt;
import com.netanel.irrigator_app.services.ConnectivityCallback;
import com.netanel.irrigator_app.connection.IDataBaseConnection;
import com.netanel.irrigator_app.services.NullResultException;
import com.netanel.irrigator_app.services.NetworkUtilities;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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


public class ManualViewModel extends ObservableViewModel
        implements IManualViewModel,
        ConnectivityCallback.IConnectivityChangedCallback{

    private static final boolean EDITED = true;
    private static final boolean ENABLED = true;

    private final ConnectivityCallback mConnectivityChangedCallback;

    private final Repository mRepository;

    private final Handler mHandler;

    private final ProgressTimer mTimer;

    private String mMessage;

    private int mMessageResource;

    private Object[] mMessageArr;

    private boolean mIsEnabled;

    private boolean mIsScaleButtonChange;

    private List<ModuleViewModel> mValves;

    private List<SensorViewModel> mSensors;

    private ModuleViewModel mSelectedValve;

    public ManualViewModel(Application application) {
        super(application);
        mRepository = AppServices.getInstance().getRepository();
        mTimer = new ProgressTimer();
        mConnectivityChangedCallback = new ConnectivityCallback(this, getApplication().getApplicationContext());
        mHandler = new Handler(Looper.getMainLooper());

        NetworkUtilities.registerConnectivityCallback(
                getApplication().getApplicationContext(), mConnectivityChangedCallback);

        if (mValves == null) {
            if (NetworkUtilities.isOnline(this.getApplication())) {
                initValveViewModels();
            } else {
                setMessageResource(R.string.msg_no_connection);
            }
        }
    }

    @Override
    protected void onCleared() {
        NetworkUtilities.unregisterConnectivityCallback(
                this.getApplication().getApplicationContext(), mConnectivityChangedCallback);

        for (ModuleViewModel viewModel:
             mValves) {
            viewModel.onCleared();
        }
        super.onCleared();
    }

    @Override
    public void onConnectivityChanged(final boolean isConnected) {
        runOnUiThread(() -> {
            if (isConnected) {
                if (mValves == null || mValves.isEmpty()) {
                    setMessageArray(new Object[]{R.string.msg_connection_resumed,
                            StringExt.COMMA, StringExt.SPACE,
                            R.string.msg_loading_valves});
                    initValveViewModels();
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
    public List<ModuleViewModel> getValves() {
        return mValves;
    }

    public void setValves(List<ModuleViewModel> valves) {
        mValves = valves;
        notifyPropertyChanged(BR.valves);
    }

    @Override
    public List<SensorViewModel> getSensors() {
        return mSensors;
    }

    public void setSensors(List<SensorViewModel> sensorsVms) {
        this.mSensors = sensorsVms;
        notifyPropertyChanged(BR.sensors);
    }

    @Override
    public ModuleViewModel getSelectedValve() {
        return mSelectedValve;
    }

    public void setSelectedValve(ModuleViewModel valve) {
        mSelectedValve = valve;
        notifyPropertyChanged(BR.selectedValve);
        setSensors(valve.getSensorsViewModels());
    }

    @Override
    public boolean getEnabled() {
        return mIsEnabled;
    }

    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
        notifyPropertyChanged(BR.enabled);
    }

    @Override
    public void setRelativeProgress(double relativeProgress) {
        mIsScaleButtonChange = true;
        mSelectedValve.setProgress((int) (mSelectedValve.getMaxDuration() * relativeProgress));
    }

    public void initValveViewModels() {
        mRepository.getModules(new IDataBaseConnection.TaskListener<List<Module>>() {
            @Override
            public void onComplete(List<Module> result) {
                    if (!result.isEmpty()) {
                        LinkedList<ModuleViewModel> valves = new LinkedList<>();
                        for (Module module :
                                result) {
                            ModuleViewModel valveVm = new ModuleViewModel(module);
                            valves.add(valveVm);
                        }

                        setValves(valves);
                        setEnabled(ENABLED);
                        setMessageResource(R.string.msg_loaded_successful);
                    } else {
                        setEnabled(!ENABLED);
                        setMessageResource(R.string.error_no_valves);
                    }
                }

            @Override
            public void onFailure(Exception exception) {
                setEnabled(!ENABLED);

                if(exception instanceof NullResultException) {
                    setMessageResource(R.string.msg_returned_null_result);
                }else {
                    setMessage(exception.getMessage());
                }
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void onTabValveSelected(ModuleViewModel valveVm) {
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
        Map<String, Object> attr = new HashMap<>();
        attr.put("index", mSelectedValve.getIp());

        if (mSelectedValve.isEdited()) {
            if (mSelectedValve.getEditedProgress() != 0) {
                attr.put("duration", mSelectedValve.getEditedProgress());
                cmnd = new Command(Actions.OPEN, attr);
            } else {
                cmnd = new Command(Actions.CLOSE, attr);
            }
        } else if (mSelectedValve.isOpen()) {
            cmnd = new Command(Actions.CLOSE, attr);
        }

        if (cmnd != null) {
            mSelectedValve.removeViewState(ModuleViewModel.State.ENABLED);
            mRepository.addCommand(cmnd, new IDataBaseConnection.TaskListener<Command>() {
                @Override
                public void onComplete(Command result) {
                    if (result != null) {
                        Log.println(Log.INFO, "Command", "registered with id:" + result.getId());
                        setMessageResource(R.string.command_sent);
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    setMessage(exception.getMessage());
                    mSelectedValve.addViewState(ModuleViewModel.State.ENABLED);
                }
            });
        }
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
                    mProgress = (int) l / 1000;
                    if(mProgress != 0)
                        mSelectedValve.setProgress(mProgress);
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
