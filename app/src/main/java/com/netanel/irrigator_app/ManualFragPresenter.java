package com.netanel.irrigator_app;

import android.os.CountDownTimer;

import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.model.Valve;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.ViewModel;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 23/09/2020
 */

public class ManualFragPresenter extends ViewModel
        implements ManualFragContract.IPresenter {

    private ManualFragContract.IView mView;

    private final IDataBaseConnection mDb;
    private Map<String, Valve> mValveMap;

    private Map<String, Integer> mBtnMap;
    private Map<Integer, String> mBtnMapInverse;

    private Valve mSelectedValve;
    private final ValveTimer mTimer;

//    private ManualFragRouter mRouter;

    public ManualFragPresenter() {
        mDb = AppServices.getInstance().getDbConnection();
        mTimer = new ValveTimer();
//        mRouter = router;
    }

    private void showSelectedValve() {
        if (mSelectedValve != null) {
            for (ManualFragContract.PredefinedTime predefinedTime :
                    ManualFragContract.PredefinedTime.values()) {
                mView.setPredefinedTimeText(predefinedTime,
                        String.valueOf((int) (
                                mSelectedValve.getMaxDuration() * predefinedTime.value / 60)));
            }

            mView.setSeekBarMaxProgress(mSelectedValve.getMaxDuration());
            mView.setTitleText(mSelectedValve.getName());
            mView.setPowerIconActivatedState(mSelectedValve.getState());
            mView.setPowerIconEditedState(false);
            updateFocusedValveProgressView();
        } else {
            mView.showMessage("Error loading valve");
        }
    }

    private void updateFocusedValveProgressView() {
        mTimer.stopIfRunning();

        if (mSelectedValve.getState() &&
                mSelectedValve.getLastOnTime().before(Calendar.getInstance().getTime())) {

            if (mSelectedValve.getTimeLeftOn() > 0) {
                mView.setSeekBarProgress((int) mSelectedValve.getTimeLeftOn());
                mTimer.startCountDown();
            } else {
                mTimer.startElapsedTimer();
                mView.setSeekBarProgress(mSelectedValve.getMaxDuration());
            }
        } else {
            mView.setTimerText(formatSecToTimeString(0));
            mView.setSeekBarProgress(0);
        }
    }

    private String formatSecToTimeString(int totalSeconds) {
        if (totalSeconds < 3600) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format(Locale.getDefault(), "%02d:%02d %s",
                    minutes, seconds, minutes > 0 ? "Minutes" : "Seconds");
        } else {
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            int seconds = (totalSeconds % 3600) % 60;
            return String.format(Locale.getDefault(), "%02d:%02d:%02d %s",
                    hours, minutes, seconds, "Hours");
        }

    }

    public void populateValves() {
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
                            int btnId = mView.addStateRadioButton(currValve.getState(),
                                    String.format(Locale.ENGLISH, "#%d", currValve.getIndex()));
                            addToBtnMaps(btnId, currValve.getId());
                        }
                    } else {
                        mView.showMessage("No Valves Found");
                    }
                }
            }
        });
    }

    private void addToBtnMaps(int btnId, String valveId) {
        mBtnMap.put(valveId, btnId);
        mBtnMapInverse.put(btnId, valveId);
    }

    private void initValveListeners(final Valve valve) {
        initValveDbListener(valve);

        valve.setOnPropertyChangedListener(new Valve.OnPropertyChangedListener() {
            @Override
            public void OnPropertyChanged(Valve updatedValve, String propertyName, Object oldValue) {
                if (propertyName.equals(Valve.PROPERTY_DURATION) ||
                        propertyName.equals(Valve.PROPERTY_LAST_ON)) {
                    if (mSelectedValve == updatedValve) {
                        updateFocusedValveProgressView();
                    }
                }
                if (propertyName.equals(Valve.PROPERTY_STATE)) {
                    Integer btnId;
                    if ((btnId = mBtnMap.get(updatedValve.getId())) != null) {
                        mView.setRadioButtonState(btnId, updatedValve.getState());
                        if (mSelectedValve == updatedValve) {
                            mView.setPowerIconActivatedState(mSelectedValve.getState());
                        }
                    }
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

    @Override
    public void bindView(ManualFragContract.IView view) {
        this.mView = view;
    }

    @Override
    public void onCreate() {
        if (mBtnMap == null) {
            mBtnMap = new HashMap<>();
        }
        if (mBtnMapInverse == null) {
            mBtnMapInverse = new HashMap<>();
        }

        populateValves();
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
    @Override
    public void onStateRadioButtonClicked(int btnId) {
        if (mSelectedValve == null) {
            mView.switchToValveView();
        }

        mSelectedValve = mValveMap.get(mBtnMapInverse.get(btnId));
        showSelectedValve();
    }

    @Override
    public void onPredefinedTimeClicked(ManualFragContract.PredefinedTime time) {
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
//        Command cmnd = new Command();
//        cmnd.setDuration(mView.getSeekBarProgress());
//        cmnd.setValveId(mSelectedValve.getId());
//        AppServices.getInstance().getDbConnection().addCommand(cmnd, new IDataBaseConnection.TaskListener<Command>() {
//            @Override
//            public void onComplete(Command answer, Exception ex) {
//                if (ex != null) {
//                    mView.showMessage(ex.getMessage());
//                } else if (answer != null) {
//                    mView.showMessage("Command registered with id:" + answer.getId());
//                }
//            }
//        });
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
