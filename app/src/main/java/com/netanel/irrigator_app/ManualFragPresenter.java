package com.netanel.irrigator_app;

import android.app.Application;
import android.content.res.Resources;
import android.os.CountDownTimer;
import android.util.Log;

import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.services.StringExt;
import com.netanel.irrigator_app.services.connection.ConnectivityCallback;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.model.Valve;
import com.netanel.irrigator_app.services.connection.NetworkUtilities;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
// TODO: 13/03/2021 build a joined parent for presenters to handle global values for all fragments, start with valves.

public class ManualFragPresenter extends AndroidViewModel
        implements ManualFragContract.IPresenter,
        ConnectivityCallback.IConnectivityChangedCallback {
    private static final boolean EDITED = true;
    private static final boolean ENABLED = true;

    private final String[] mTimeNames;

    private final Resources mResources;
    private final ConnectivityCallback mConnectivityChangedCallback;
    private final IDataBaseConnection mDb;
    private final ValveTimer mTimer;

    private ManualFragContract.IView mView;

    private Map<String, Valve> mValveMap;
    private Map<String, Integer> mTabMap;
    private Map<Integer, String> mTabMapInverse;

    private Valve mSelectedValve;

    public ManualFragPresenter(Application app) {
        super(app);
        mDb = AppServices.getInstance().getDbConnection();
        mTimer = new ValveTimer();
        mResources = getApplication().getResources();
        mConnectivityChangedCallback = new ConnectivityCallback(this, app.getApplicationContext());
        NetworkUtilities.registerConnectivityCallback(
                app.getApplicationContext(), mConnectivityChangedCallback);

        mTimeNames = new String[]{
                mResources.getString(R.string.time_counter_seconds),
                mResources.getString(R.string.time_counter_minutes),
                mResources.getString(R.string.time_counter_hours),
                mResources.getString(R.string.time_counter_days)};
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
            mView.showMessage(mResources.getString(R.string.msg_no_connection));
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
                            mView.addTab(i, tabText, currValve.isOpen());
                        }
                    } else {
                        mView.showMessage(mResources.getString(R.string.error_no_valves));
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

        valve.setOnPropertyChangedCallback(new Valve.OnPropertyChangedCallback() {
            @Override
            public void OnPropertyChanged(Valve updatedValve, String propertyName, Object oldValue) {
                Integer tabId = mTabMap.get(updatedValve.getId());

                switch (propertyName) {
                    case Valve.PROPERTY_DURATION:
                    case Valve.PROPERTY_LAST_ON_TIME:
                    case Valve.PROPERTY_OPEN:
                        if (tabId != null) {
                            mView.setTabBadge(tabId, updatedValve.isOpen());
                        }
                        if (mSelectedValve == updatedValve) {
                            mView.setPowerButtonActiveState(mSelectedValve.isOpen());
                            mView.setPowerButtonEditedState(!EDITED);
                            mView.setSeekBarEditedState(!EDITED);
                            mView.setPowerButtonEnabled(mSelectedValve.isOpen());
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
                        mView.showMessage(mResources.getString(R.string.msg_connection_resumed)
                                + StringExt.COMMA + StringExt.SPACE
                                + mResources.getString(R.string.msg_loading_valves));
                        loadValves();
                    } else {
                        mView.showMessage(mResources.getString(R.string.msg_connection_resumed));
                        mView.setUiEnabled(true);
                    }
                } else {
                    mView.showMessage(mResources.getString(R.string.msg_connection_lost));
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

        mView.setTimerText(StringExt.formatSecToTimeString(progress, mTimeNames));
    }

    private void onUserSeekBarProgressChanged() {
        mTimer.stopIfRunning();
        mView.setPowerButtonEditedState(EDITED);
        mView.setSeekBarEditedState(EDITED);
        if(mView.getSeekBarProgress() != 0) {
            mView.setPowerButtonEnabled(ENABLED);
        } else if (!mSelectedValve.isOpen()) {
            mView.setPowerButtonEnabled(!ENABLED);
        }
    }

    private boolean isUserSeekBarProgressChanged() {
        return mSelectedValve.timeLeftOpen() != mView.getSeekBarProgress();
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
            mView.setPowerButtonActiveState(mSelectedValve.isOpen());
            mView.setPowerButtonEnabled(mSelectedValve.isOpen());
            mView.setPowerButtonEditedState(!EDITED);
            mView.setSeekBarEditedState(!EDITED);
            updateFocusedValveProgressView();
        } else {
            mView.showMessage(mResources.getString(R.string.error_loading_valve));
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

        if (mSelectedValve.isOpen()
                && mSelectedValve.getLastOnTime().before(Calendar.getInstance().getTime())) {

            if (mSelectedValve.timeLeftOpen() > 0) {
                mView.setSeekBarProgress((int) mSelectedValve.timeLeftOpen());
                mTimer.startCountDown();
            }
        } else {
            mView.setTimerText(StringExt.formatSecToTimeString(0, mTimeNames));
            mView.setSeekBarProgress(0);
        }
    }

    @Override
    public void onTimeScaleClicked(ManualFragContract.TimeScale time) {
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
                mView.setSeekBarProgress((mSelectedValve.getMaxDuration()));
                break;
        }

        onUserSeekBarProgressChanged();
    }

    @Override
    public void onButtonPowerClicked() {
        Command cmnd = null;

        if (isUserSeekBarProgressChanged()) {
            if(mView.getSeekBarProgress() != 0) {
                cmnd = new Command(mSelectedValve.getIndex(), mView.getSeekBarProgress(), Valve.OPEN);
            } else {
                cmnd = new Command(mSelectedValve.getIndex(), !Valve.OPEN);
            }
        } else if (mSelectedValve.isOpen()) {
            cmnd = new Command(mSelectedValve.getIndex(), !Valve.OPEN);
        }

        if (cmnd != null) {
            AppServices.getInstance().getDbConnection().addCommand(cmnd, new IDataBaseConnection.TaskListener<Command>() {
                @Override
                public void onComplete(Command answer, Exception ex) {
                    if (ex != null) {
                        mView.showMessage(ex.getMessage());
                    } else if (answer != null) {
                        Log.println(Log.INFO, "Command", "registered with id:" + answer.getId());
                    }
                }
            });
        }
    }

    public class ValveTimer {
        private CountDownTimer mCountDownTimer;
        private Timer mElapsedTimer;
        private boolean mIsTimerRunning = false;

        public void startCountDown() {
            mCountDownTimer = new CountDownTimer(
                    mSelectedValve.timeLeftOpen() * 1000,
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
                            mView.setTimerText(
                                    StringExt.formatSecToTimeString((int) diffInSec, mTimeNames));
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
