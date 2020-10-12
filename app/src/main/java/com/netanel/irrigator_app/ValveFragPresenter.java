package com.netanel.irrigator_app;

import android.os.CountDownTimer;

import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.model.Valve;
import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;

import java.util.Locale;

import androidx.lifecycle.ViewModel;

import static com.netanel.irrigator_app.ValveFragContract.*;

public class ValveFragPresenter extends ViewModel implements IPresenter {

    private IView mView;
    private Valve mValve;
    private CountDownTimer mTimer;
    private boolean mIsTimerRunning = false;

    private ManualFragRouter mRouter;

    public ValveFragPresenter(ManualFragRouter router) {
        mRouter = router;
    }

    private void updateProgressView() {
        mView.setSeekBarProgress((int) mValve.getTimeLeftOn());
        updateViewPowerIconColor();

        if (mValve.getTimeLeftOn() > 0) {
            startTimer();
        } else {
            mView.setTimerText(formatSecToTimeString(0));
            mView.setSeekBarProgress(0);
        }
    }

    private void updateViewPowerIconColor() {
        if(mValve.getState()) {
            mView.setImageDrawableTint(R.color.color_valve_state_on);
        } else {
            mView.setImageDrawableTint(R.color.color_valve_state_off);
        }
    }

    private void startTimer() {
        mTimer = new CountDownTimer(
                mValve.getTimeLeftOn() * 1000,
                1000) {
            @Override
            public void onTick(long l) {
                mView.setSeekBarProgress((int) l / 1000);
            }

            @Override
            public void onFinish() {
                mView.setSeekBarProgress(0);
                updateViewPowerIconColor();
                if(mOnTimerFinishedListener != null) {
                    mOnTimerFinishedListener.onTimerFinished();
                }
            }
        }.start();
        mIsTimerRunning = true;
    }

    private void stopTimer() {
        mTimer.cancel();
        mIsTimerRunning = false;
    }

    private String formatSecToTimeString(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d %s",
                minutes, seconds, minutes > 0 ? "Minutes" : "Seconds");
    }

    @Override
    public void bindView(IView view) {
        mView = view;
    }

    @Override
    public void onCreateView() {
        mValve = mRouter.getFragmentArguments();

        initValveDbListener(mValve);

        mValve.setOnChangedListener(new Valve.OnChangedListener() {
            @Override
            public void OnPropertyChanged(Valve updatedValve, String propertyName, Object oldValue) {
                switch (propertyName) {
                    case Valve.PROPERTY_LAST_ON:
                    case Valve.PROPERTY_DURATION:
                        if (mIsTimerRunning) stopTimer();
                        updateProgressView();
                        break;
                    case Valve.PROPERTY_NAME:
                        mView.setTitleText(updatedValve.getName());
                        break;
                }
            }
        });
    }

    private void initValveDbListener(final Valve valve) {
        AppServices.getInstance().getDbConnection()
                .addOnValveChangedListener(valve.getId(), new IDataBaseConnection.OnDataChangedListener<Valve>() {
                    @Override
                    public void onDataChanged(Valve changedObject, Exception ex) {
                        if (ex != null) {
//                            getView().showMessage(ex.getMessage());
                        }
                        valve.update(changedObject);
                    }
                });
    }

    @Override
    public void onViewCreated() {
        if (mValve != null) {
            mView.setSeekBarMaxProgress(mValve.getMaxDuration());
            mView.setPredefinedTimeText(PredefinedTime.Zero, String.valueOf(0));
            mView.setPredefinedTimeText(PredefinedTime.Quarter, String.valueOf((int) (mValve.getMaxDuration() * 0.25 / 60)));
            mView.setPredefinedTimeText(PredefinedTime.Half, String.valueOf((int) (mValve.getMaxDuration() * 0.5 / 60)));
            mView.setPredefinedTimeText(PredefinedTime.ThreeQuarters, String.valueOf((int) (mValve.getMaxDuration() * 0.75 / 60)));
            mView.setPredefinedTimeText(PredefinedTime.Max, String.valueOf((int) (mValve.getMaxDuration() / 60)));
            mView.setTitleText(mValve.getName());
            updateProgressView();
        }
        else mView.showMessage("Error loading valve");
    }

    public void onSeekBarProgressChanged(final int progress, boolean fromUser) {

        if(mIsTimerRunning && fromUser) {
            stopTimer();
        }

        mView.setTimerText(formatSecToTimeString(progress));
    }

    @Override
    public void onPredefinedTimeClicked(PredefinedTime time) {

        if(mIsTimerRunning) {
            stopTimer();
        }

        switch (time) {
            case Zero:
                mView.setSeekBarProgress(0);
                break;
            case Quarter:
                mView.setSeekBarProgress((int)(mValve.getMaxDuration() * 0.25));
            break;
                case Half:
                mView.setSeekBarProgress((int)(mValve.getMaxDuration() * 0.5));
                break;
            case ThreeQuarters:
                mView.setSeekBarProgress((int)(mValve.getMaxDuration() * 0.75));
                break;
            case Max:
                mView.setSeekBarProgress((int)(mValve.getMaxDuration()));
                break;
        }
    }

    @Override
    public void onButtonSetClicked() {
        Command cmnd = new Command();
        cmnd.setDuration(mView.getSeekBarProgress());
        cmnd.setValveId(mValve.getId());
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

    public void setOnTimerFinished(IOnTimerFinishedListener onTimerFinishedListener) {
        mOnTimerFinishedListener = onTimerFinishedListener;
    }
    private IOnTimerFinishedListener mOnTimerFinishedListener;
    public interface IOnTimerFinishedListener {
        void onTimerFinished();
    }
}