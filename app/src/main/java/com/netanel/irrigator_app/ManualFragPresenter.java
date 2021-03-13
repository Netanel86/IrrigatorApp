package com.netanel.irrigator_app;

import android.app.Application;
import android.os.CountDownTimer;

import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.services.connection.ConnectivityCallback;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.model.Valve;
import com.netanel.irrigator_app.services.connection.NetworkUtilities;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.AndroidViewModel;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 23/09/2020
 */

// TODO: 13/03/2021 handle power button functionality, add isEdited value to track changes.
// TODO: 13/03/2021 build a joined parent for presenters to handle global values for all fragments, start with valves.
// TODO: 13/03/2021 remove all hardcoded strings
public class ManualFragPresenter extends AndroidViewModel
        implements ManualFragContract.IPresenter,
        ConnectivityCallback.IConnectivityChangedCallback {
    private static final int HOUR_IN_SEC = 3600;
    private static final int DAY_IN_SEC = 3600 * 24;

    private ManualFragContract.IView mView;
    private final ConnectivityCallback mConnectivityChangedCallback;
    private final IDataBaseConnection mDb;
    private Map<String, Valve> mValveMap;

    private Map<String, Integer> mTabMap;
    private Map<Integer, String> mTabMapInverse;

    private Valve mSelectedValve;
    private final ValveTimer mTimer;

//    private ManualFragRouter mRouter;

    public ManualFragPresenter(Application app) {
        super(app);
        mDb = AppServices.getInstance().getDbConnection();
        mTimer = new ValveTimer();
        mConnectivityChangedCallback = new ConnectivityCallback(this, app.getApplicationContext());
        NetworkUtilities.registerConnectivityCallback(
                app.getApplicationContext(), mConnectivityChangedCallback);
//        mRouter = router;
    }

    @Override
    public void bindView(ManualFragContract.IView view) {
        this.mView = view;
    }

    @Override
    public void onViewCreated() {
        if (mTabMap == null) {
            mTabMap = new HashMap<>();
        }
        if (mTabMapInverse == null) {
            mTabMapInverse = new HashMap<>();
        }

        if(NetworkUtilities.isOnline(this.getApplication())) {
            loadValves();
        } else {
            mView.showMessage("internet connection is not available");
        }

    }

    @Override
    public void onDestroy() {
        NetworkUtilities.unregisterConnectivityCallback(
                this.getApplication().getApplicationContext(), mConnectivityChangedCallback);
    }

    public void loadValves() {
        mDb.getValves(new IDataBaseConnection.TaskListener<Map<String, Valve>>() {
            @Override
            public void onComplete(Map<String, Valve> answer, Exception ex) {
                if (ex != null) {
                    mView.showMessage(ex.getMessage());
                } else if (answer != null) {
                    mValveMap = answer;
                    if (!mValveMap.isEmpty()) {
                        ArrayList<Valve> valves = new ArrayList<>(mValveMap.values());
                        for (int i = 0; i < valves.size(); i++) {
                            Valve currValve = valves.get(i);
                            initValveListeners(currValve);
                            String tabText = getValveViewDescription(currValve);
                            addToTabMaps(i, currValve.getId());
                            mView.addTab(i, tabText, currValve.isActive());
                        }
                    } else {
                        mView.showMessage("No Valves Found");
                    }
                }
            }
        });
    }

    private String getValveViewDescription(Valve valve) {
        return valve.getDescription() == null || valve.getDescription().isEmpty() ?
                "#" + valve.getIndex() : valve.getDescription();
    }

    private void initValveListeners(final Valve valve) {
        initValveDbListener(valve);

        valve.setOnPropertyChangedListener(new Valve.OnPropertyChangedListener() {
            @Override
            public void OnPropertyChanged(Valve updatedValve, String propertyName, Object oldValue) {
                Integer tabId = mTabMap.get(updatedValve.getId());

                switch (propertyName) {
                    case Valve.PROPERTY_DURATION:
                    case Valve.PROPERTY_LAST_ON_TIME:
                    case Valve.PROPERTY_ACTIVE:
                        if (tabId != null) {
                            mView.setTabBadge(tabId, updatedValve.isActive());
                        }
                        if (mSelectedValve == updatedValve) {
                            mView.setPowerIconActiveState(mSelectedValve.isActive());
                            updateFocusedValveProgressView();
                        }
                        break;

                    case Valve.PROPERTY_MAX_DURATION:
                        if (mSelectedValve == updatedValve) {
                            mView.setSeekBarMaxProgress(mSelectedValve.getMaxDuration());
                            setTimeScaleMarks();
                            updateFocusedValveProgressView();
                        }
                        break;

                    case Valve.PROPERTY_DESCRIPTION:
                        if (mSelectedValve == updatedValve) {
                            mView.setTitleText(mSelectedValve.getDescription());
                        }
                    case Valve.PROPERTY_INDEX:
                        if (tabId != null) {
                            mView.setTabDescription(tabId, getValveViewDescription(updatedValve));
                        }
                        break;
                }
//                if (propertyName.equals(Valve.PROPERTY_DURATION) ||
//                        propertyName.equals(Valve.PROPERTY_LAST_ON_TIME) ||
//                        propertyName.equals(Valve.PROPERTY_ACTIVE) ||
//                        propertyName.equals(Valve.PROPERTY_MAX_DURATION)) {
//                    if (tabId != null) {
//                        mView.setTabValveActiveState(tabId, updatedValve.isActive());
//                    }
//
//                    if (mSelectedValve == updatedValve) {
//                        updateFocusedValveProgressView();
//                    }
//                }else if (propertyName.equals(Valve.PROPERTY_DESCRIPTION) ||
//                        propertyName.equals(Valve.PROPERTY_INDEX)) {
//                    if (tabId != null) {
//                        mView.setTabValveDescription(tabId, getValveViewDescription(updatedValve));
//                    }
//
//                    if (mSelectedValve == updatedValve) {
//                        mView.setTitleText(mSelectedValve.getDescription());
//                    }
//                }
            }
        });
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

    private void addToTabMaps(int btnId, String valveId) {
        mTabMap.put(valveId, btnId);
        mTabMapInverse.put(btnId, valveId);
    }

    @Override
    public void onConnectivityChanged(final boolean isConnected) {
        mView.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(isConnected) {
                    if(mValveMap == null || mValveMap.isEmpty()) {
                        mView.showMessage("connection resumed! loading valves!");
                        loadValves();
                    } else {
                        mView.showMessage("connection resumed!");
                        mView.setUiEnabled(true);
                    }
                } else {
                    mView.showMessage("connection lost!");
                    mTimer.stopIfRunning();
                    mView.setUiEnabled(false);
                }
            }
        });

    }

    @Override
    public void onSeekBarProgressChanged(final int progress, boolean fromUser) {
        if (fromUser) {
            onUserSeekBarProgressChanged();
        }

        mView.setTimerText(formatSecToTimeString(progress));
    }

    private void onUserSeekBarProgressChanged() {
        mTimer.stopIfRunning();
        mView.setPowerIconEditedState(true);
    }

    private String formatSecToTimeString(int totalSeconds) {
        String timeFormat;
        if (totalSeconds < HOUR_IN_SEC) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            timeFormat = String.format(Locale.getDefault(),
                    "%02d:%02d%s", minutes, seconds, minutes > 0 ? "min" : "sec");
        } else if (totalSeconds < DAY_IN_SEC) {
            int hours = totalSeconds / HOUR_IN_SEC;
            int minutes = (totalSeconds % HOUR_IN_SEC) / 60;
            int seconds = (totalSeconds % HOUR_IN_SEC) % 60;
            timeFormat = String.format(Locale.getDefault(),
                    "%02d:%02d:%02dhrs", hours, minutes, seconds);
        } else {
            int days = totalSeconds / DAY_IN_SEC;
            int hours = (totalSeconds % DAY_IN_SEC) / HOUR_IN_SEC;
            int minutes = ((totalSeconds % DAY_IN_SEC) % HOUR_IN_SEC) / 60;
            int seconds = ((totalSeconds % DAY_IN_SEC) % HOUR_IN_SEC) % 60;
            timeFormat = String.format(Locale.getDefault(),
                    "%dd %02d:%02d:%02dhrs", days, hours, minutes, seconds);
        }
        return timeFormat;
    }

    @Override
    public void onTabClicked(int tabId) {
        if (mSelectedValve == null) {
            mView.switchToValveView();
        }

        mSelectedValve = mValveMap.get(mTabMapInverse.get(tabId));
        showSelectedValve();
    }

    private void showSelectedValve() {
        if (mSelectedValve != null) {
            setTimeScaleMarks();
            mView.setSeekBarMaxProgress(mSelectedValve.getMaxDuration());
            mView.setTitleText(mSelectedValve.getDescription());
            mView.setPowerIconActiveState(mSelectedValve.isActive());
            mView.setPowerIconEditedState(false);
            updateFocusedValveProgressView();
        } else {
            mView.showMessage("Error loading valve");
        }
    }

    private void setTimeScaleMarks() {
        for (ManualFragContract.TimeScale timeScale :
                ManualFragContract.TimeScale.values()) {
            mView.setTimeScaleText(timeScale,
                    String.valueOf((int) (
                            mSelectedValve.getMaxDuration() * timeScale.value / 60)));
        }
    }

    private void updateFocusedValveProgressView() {
        mTimer.stopIfRunning();

        if (mSelectedValve.isActive()
                && mSelectedValve.getLastOnTime().before(Calendar.getInstance().getTime())) {

            if (mSelectedValve.getTimeLeftOn() > 0) {
                mView.setSeekBarProgress((int) mSelectedValve.getTimeLeftOn());
                mTimer.startCountDown();
            }
        } else {
            mView.setTimerText(formatSecToTimeString(0));
            mView.setSeekBarProgress(0);
        }
    }

    @Override
    public void onTimeScaleClicked(ManualFragContract.TimeScale time) {
        onUserSeekBarProgressChanged();

        switch (time) {
            case Zero:
                mView.setSeekBarProgress(0);
                break;
            case Quarter:
                mView.setSeekBarProgress((int) (mSelectedValve.getMaxDuration() * 0.25));
                break;
            case Half:
                mView.setSeekBarProgress((int) (mSelectedValve.getMaxDuration() * 0.5));
                break;
            case ThreeQuarters:
                mView.setSeekBarProgress((int) (mSelectedValve.getMaxDuration() * 0.75));
                break;
            case Max:
                mView.setSeekBarProgress((int) (mSelectedValve.getMaxDuration()));
                break;
        }
    }

    @Override
    public void onButtonPowerClicked() {
        Command cmnd = new Command();
        cmnd.setDuration(mView.getSeekBarProgress());
        cmnd.setIndex(mSelectedValve.getIndex());
        cmnd.setState(mView.getSeekBarProgress() > 0);
        AppServices.getInstance().getDbConnection().addCommand(cmnd, new IDataBaseConnection.TaskListener<Command>() {
            @Override
            public void onComplete(Command answer, Exception ex) {
                if (ex != null) {
                    mView.showMessage(ex.getMessage());
                } else if (answer != null) {
                    mView.showMessage("Command registered with id:" + answer.getId());
                }
            }
        });
    }

    public class ValveTimer {
        private CountDownTimer mCountDownTimer;
        private Timer mElapsedTimer;
        private boolean mIsTimerRunning = false;

        public void startCountDown() {
            mCountDownTimer = new CountDownTimer(
                    mSelectedValve.getTimeLeftOn() * 1000,
                    1000) {
                @Override
                public void onTick(long l) {
                    mView.setSeekBarProgress((int) l / 1000);
                }

                @Override
                public void onFinish() {
                    mView.setSeekBarProgress(0);
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
                                            mSelectedValve.getLastOnTime().getTime());
                            mView.setTimerText(formatSecToTimeString((int) diffInSec));
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
