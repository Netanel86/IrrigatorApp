package com.netanel.irrigator_app;

import android.app.Application;
import android.content.res.Resources;
import android.os.CountDownTimer;
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
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 23/09/2020
 */
// TODO: 13/03/2021 build a joined parent for presenters to handle global values for all fragments.
// TODO: 27/04/2021 remove all resource handling from presenter and implement them in the view.

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

    private int mActiveView;

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

    private final Resources mResources;

    private ManualFragContract.IView mView;

    private final ProgressTimer mTimer;

    public ManualFragPresenter(Application app) {
        super(app);
        mDb = AppServices.getInstance().getDbConnection();
        mTimer = new ProgressTimer();
        mResources = getApplication().getResources();
        mConnectivityChangedCallback = new ConnectivityCallback(this, app.getApplicationContext());
        NetworkUtilities.registerConnectivityCallback(
                app.getApplicationContext(), mConnectivityChangedCallback);
    }

    @Override
    public void bindView(ManualFragContract.IView view) {
        this.mView = view;
    }

    @Override
    public void onViewCreated() {
        if (mValves == null) {
            if (NetworkUtilities.isOnline(this.getApplication())) {
                fetchValves();

            } else {
                mView.showMessage(mResources.getString(R.string.msg_no_connection));
            }
        }
//        else {
//            addValvesToView();
//        }

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

        for (Sensor s :
                sensors) {
            switch (s.getMeasureType()) {
                case HUMIDITY:
                    mView.addHumiditySensorView(s.getValue(), s.getMaxValue());
                    break;
                case TEMPERATURE:
                    mView.addTemperatureSensorView(s.getValue(), s.getMaxValue());
                    break;
                case FLOW:
                    mView.addFlowSensorView(s.getValue(), s.getMaxValue());
                    break;
                case PH:
                    mView.addPhSensorView(s.getValue(), s.getMaxValue());
                    break;
            }
        }

    }

    @Override
    public void onDestroy() {
        NetworkUtilities.unregisterConnectivityCallback(
                this.getApplication().getApplicationContext(), mConnectivityChangedCallback);
    }

    public void fetchValves() {
        mDb.getValves(new IDataBaseConnection.TaskListener<Map<String, Valve>>() {
            @Override
            public void onComplete(Map<String, Valve> answer, Exception ex) {
                if (ex != null) {
                    mView.showMessage(ex.getMessage());
                } else if (answer != null) {
//                    setValveMap(answer);
                    if (!answer.isEmpty()) {
                        LinkedHashMap<String, ValveViewModel> valvesMap = new LinkedHashMap<>();
                        ArrayList<Valve> valves = new ArrayList<>(answer.values());
                        for (int i = 0; i < valves.size(); i++) {
                            initValveListeners(valves.get(i));

                            ValveViewModel valveVm = new ValveViewModel(valves.get(i));
                            valveVm.addOnPropertyChangedCallback(new OnPropertyChangedCallback() {
                                @Override
                                public void onPropertyChanged(Observable sender, int propertyId) {
                                    if(propertyId == BR.progress) {
                                        if(sender.equals(mSelectedValve) &&
                                                !isTimerProgressChanged()) {
                                            resetTimer();
                                        }
                                    }
                                }
                            });
                            valvesMap.put(valveVm.getId(), valveVm);
                        }
                        setValveMap(valvesMap);
//                        addValvesToView();
                    } else {
                        mView.showMessage(mResources.getString(R.string.error_no_valves));
                    }
                } else {
                    mView.showMessage("Something went wrong, db returned empty result");
                }
            }
        });
    }

    private void initValveListeners(final Valve valve) {
        initValveDbListener(valve);
//        initValveViewListener(valve);
    }

    private void initValveDbListener(@NotNull final Valve valve) {
        mDb.addOnValveChangedListener(valve.getId(), new IDataBaseConnection.OnDataChangedListener<Valve>() {
            @Override
            public void onDataChanged(Valve changedObject, Exception ex) {
                if (ex != null) {
                    mView.showMessage(ex.getMessage());
                }
                valve.update(changedObject);
            }
        });
    }

    private void initValveViewListener(Valve valve) {
        valve.setOnPropertyChangedCallback(new Valve.OnPropertyChangedCallback() {
            @Override
            public void OnPropertyChanged(Valve updatedValve, String propertyName, Object oldValue) {
//                Integer tabId = mTabMap.get(updatedValve.getId());

                switch (propertyName) {
                    case Valve.PROPERTY_DURATION:
                    case Valve.PROPERTY_LAST_ON_TIME:
                    case Valve.PROPERTY_OPEN:

//                        mView.setValveOpen(updatedValve.getId(),updatedValve.isOpen());

//                        if (mSelectedValve == updatedValve.getId()) {
//                            updateSelectedValveProgressView();
//                        }
                        break;

                    case Valve.PROPERTY_MAX_DURATION:
//                        if (mSelectedValve == updatedValve) {
//                            mView.setSelectedValveMaxProgress(mSelectedValve.getMaxDuration());
//                            updateSelectedValveProgressView();
//                        }
                        break;

                    case Valve.PROPERTY_DESCRIPTION:
                    case Valve.PROPERTY_INDEX:
//                            mView.setValveDescription(updatedValve.getId(),formatValveDescription(updatedValve));

//                        if (tabId != null) {
//                            mView.setTabDescription(tabId, formatValveDescription(updatedValve));
//                        }
                        break;
                }
            }
        });
    }

    @Override
    public void onConnectivityChanged(final boolean isConnected) {
        mView.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    if (mValves == null || mValves.isEmpty()) {
                        mView.showMessage(mResources.getString(R.string.msg_connection_resumed)
                                + StringExt.COMMA + StringExt.SPACE
                                + mResources.getString(R.string.msg_loading_valves));
                        fetchValves();
                    } else {
                        mView.showMessage(mResources.getString(R.string.msg_connection_resumed));
                        mView.setUiEnabled(true);
                    }
                } else {
                    mView.showMessage(mResources.getString(R.string.msg_connection_lost));
//                    mTimer.stopIfRunning();
                    mView.setUiEnabled(false);
                }
            }
        });

    }

    @Override
    public void onSeekBarProgressChanged(final int progress, boolean fromUser) {
        if (fromUser) {
            onUserProgressChange();
        }
    }

    private void onUserProgressChange() {
        mTimer.stopIfRunning();

        if (mSelectedValve.getProgress() != mSelectedValve.getTimeLeft()) {
            mSelectedValve.setEdited(EDITED);
        } else {
            mSelectedValve.resetViewStates();

            if (mSelectedValve.isOpen()) {
                if (mSelectedValve.getEditedProgress() == 0) {
                    mView.showMessage("Tip: use the power button to turn off a valve");
                }

                mTimer.startCountDown();
            }
        }
    }

    private void resetTimer() {
        mTimer.stopIfRunning();

        if (mSelectedValve.isOpen() && !mSelectedValve.isEdited()) {
            mTimer.startCountDown();
        }
    }

    private boolean isTimerProgressChanged() {
        long time = mSelectedValve.getTimeLeft();
        int progress = mSelectedValve.getEditedProgress();
        int diff = (int) time - progress;
        return diff == 1;
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

        if (mSelectedValve != null) {
            resetTimer();
        } else {
            mView.showMessage(mResources.getString(R.string.error_loading_valve));
        }
    }


    @Override
    public void onTimeScaleClicked(ManualFragContract.TimeScale time) {
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
//                mView.setSelectedValveProgress((mSelectedValve.getMaxDuration()));
                break;
        }

        onUserProgressChange();
    }

    @Override
    public void onSendCommand() {
        Command cmnd = null;

        if (mSelectedValve.isEdited()) {
            if (mSelectedValve.getEditedProgress() != 0) {
                cmnd = new ValveCommand(mSelectedValve.getIndex(), mSelectedValve.getEditedProgress());
            } else {
                cmnd = new ValveCommand(mSelectedValve.getIndex(), !Valve.OPEN);
            }
        } else if (mSelectedValve.isOpen()) {
            cmnd = new ValveCommand(mSelectedValve.getIndex(), !Valve.OPEN);
        }

        if (cmnd != null) {
            AppServices.getInstance().getDbConnection().addCommand(cmnd, new IDataBaseConnection.TaskListener<Command>() {
                @Override
                public void onComplete(Command answer, Exception ex) {
                    if (ex != null) {
                        mView.showMessage(ex.getMessage());
                    } else if (answer != null) {
                        Log.println(Log.INFO, "Command", "registered with id:" + answer.getId());
                        mView.showMessage(mResources.getString(R.string.command_sent));
                        mView.setSendCommandEnabledState(!ENABLED);
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
        private boolean mIsTimerRunning = false;

        public void startCountDown() {
            mCountDownTimer = new CountDownTimer(
                    (long) mSelectedValve.getTimeLeft() * 1000,
                    1000) {
                @Override
                public void onTick(long l) {
                    mSelectedValve.setProgress((int) l / 1000);
                }

                @Override
                public void onFinish() {
                    mSelectedValve.setProgress(0);
                    mIsTimerRunning = false;
                }
            }.start();
            mIsTimerRunning = true;
        }

        public void startElapsedTimer() {
            mElapsedTimer = new Timer();
            mElapsedTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    mView.runOnUiThread(new Runnable() {
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
            mIsTimerRunning = true;
        }

        public void stopIfRunning() {
            if (mIsTimerRunning) {
                if (mCountDownTimer != null) mCountDownTimer.cancel();
                if (mElapsedTimer != null) mElapsedTimer.cancel();
                mIsTimerRunning = false;
            }
        }
    }
}
