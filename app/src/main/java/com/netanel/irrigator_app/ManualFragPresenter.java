package com.netanel.irrigator_app;

import android.os.CountDownTimer;

import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.model.Valve;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
            mView.setSeekBarMaxProgress(mSelectedValve.getMaxDuration());
            mView.setPredefinedTimeText(ManualFragContract.PredefinedTime.Zero, String.valueOf(0));
            mView.setPredefinedTimeText(ManualFragContract.PredefinedTime.Quarter, String.valueOf((int) (mSelectedValve.getMaxDuration() * 0.25 / 60)));
            mView.setPredefinedTimeText(ManualFragContract.PredefinedTime.Half, String.valueOf((int) (mSelectedValve.getMaxDuration() * 0.5 / 60)));
            mView.setPredefinedTimeText(ManualFragContract.PredefinedTime.ThreeQuarters, String.valueOf((int) (mSelectedValve.getMaxDuration() * 0.75 / 60)));
            mView.setPredefinedTimeText(ManualFragContract.PredefinedTime.Max, String.valueOf((int) (mSelectedValve.getMaxDuration() / 60)));
            mView.setTitleText(mSelectedValve.getName());
            updateFocusedValveStatusIconColor();
            updateFocusedValveProgressView();
        } else {
            mView.showMessage("Error loading valve");
        }
    }

    private void updateFocusedValveProgressView() {
       mTimer.stopIfRunning();

        if (mSelectedValve.getStatus() && mSelectedValve.getTimeLeftOn() > 0) {
            mView.setSeekBarProgress((int) mSelectedValve.getTimeLeftOn());
            mTimer.start();
        } else {
            mView.setTimerText(formatSecToTimeString(0));
            mView.setSeekBarProgress(0);
        }
    }

    private void updateFocusedValveStatusIconColor() {
        if(mSelectedValve.getStatus()) {
            mView.setImageDrawableTint(R.color.color_valve_state_on);
        } else {
            mView.setImageDrawableTint(R.color.color_valve_state_off);
        }
    }

    private String formatSecToTimeString(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d %s",
                minutes, seconds, minutes > 0 ? "Minutes" : "Seconds");
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
                            int btnId = mView.addStateRadioButton(currValve.getStatus(),
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

        valve.setOnChangedListener(new Valve.OnChangedListener() {
            @Override
            public void OnPropertyChanged(Valve updatedValve, String propertyName, Object oldValue) {
                if(propertyName.equals(Valve.PROPERTY_DURATION) ||
                        propertyName.equals(Valve.PROPERTY_LAST_ON)) {
                    if(mSelectedValve == updatedValve) {
                        updateFocusedValveProgressView();
                    }
                }
                if(propertyName.equals(Valve.PROPERTY_STATUS)) {
                    Integer btnId;
                    if ((btnId = mBtnMap.get(updatedValve.getId())) != null) {
                        mView.updateStateRadioButton(btnId, updatedValve.getStatus());
                        if(mSelectedValve == updatedValve) {
                            updateFocusedValveStatusIconColor();
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
        if(fromUser) {
            mTimer.stopIfRunning();
        }

        mView.setTimerText(formatSecToTimeString(progress));
    }

    @Override
    public void onStateRadioButtonClicked(int btnId) {
        if(mSelectedValve == null ) {
            mView.switchToValveView();
        }

        mSelectedValve = mValveMap.get(mBtnMapInverse.get(btnId));
        showSelectedValve();
    }

    @Override
    public void onPredefinedTimeClicked(ManualFragContract.PredefinedTime time) {
        mTimer.stopIfRunning();

        switch (time) {
            case Zero:
                mView.setSeekBarProgress(0);
                break;
            case Quarter:
                mView.setSeekBarProgress((int)(mSelectedValve.getMaxDuration() * 0.25));
                break;
            case Half:
                mView.setSeekBarProgress((int)(mSelectedValve.getMaxDuration() * 0.5));
                break;
            case ThreeQuarters:
                mView.setSeekBarProgress((int)(mSelectedValve.getMaxDuration() * 0.75));
                break;
            case Max:
                mView.setSeekBarProgress((int)(mSelectedValve.getMaxDuration()));
                break;
        }
    }

    @Override
    public void onButtonSetClicked() {
        Command cmnd = new Command();
        cmnd.setDuration(mView.getSeekBarProgress());
        cmnd.setValveId(mSelectedValve.getId());
        AppServices.getInstance().getDbConnection().addCommand(cmnd, new IDataBaseConnection.TaskListener<Command>() {
            @Override
            public void onComplete(Command answer, Exception ex) {
                if(ex != null) {
                    mView.showMessage(ex.getMessage());
                } else if(answer != null) {
                    mView.showMessage("Command registered with id:" + answer.getId());
                }
            }
        });
    }

    public class ValveTimer {
        private CountDownTimer mTimer;
        private boolean mIsTimerRunning = false;

        public void start() {
            mTimer = new CountDownTimer(
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

        public void stopIfRunning() {
            if(mIsTimerRunning) {
                mTimer.cancel();
                mIsTimerRunning = false;
            }
        }
    }
}
