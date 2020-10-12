package com.netanel.irrigator_app;

import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.model.Valve;

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

    private IDataBaseConnection mDb;
    private Map<String, Valve> mValveMap;

    private Map<String, Integer> mBtnMap;
    private Map<Integer, String> mBtnMapInverse;

    private ManualFragRouter mRouter;

    public ManualFragPresenter(ManualFragRouter router) {
        mDb = AppServices.getInstance().getDbConnection();
        mRouter = router;
    }

    public ManualFragContract.IView getView() {
        return this.mView;
    }

    public void populateValves() {
        mDb.getValves(new IDataBaseConnection.TaskListener<Map<String, Valve>>() {
            @Override
            public void onComplete(Map<String, Valve> answer, Exception ex) {
                if (ex != null) {
                    getView().showMessage(ex.getMessage());
                } else if (answer != null) {
                    mValveMap = answer;
                    if (!mValveMap.isEmpty()) {
                        ArrayList<Valve> valves = new ArrayList<>(mValveMap.values());
                        for (int i = 0; i < valves.size(); i++) {
                            Valve currValve = valves.get(i);
                            initValveListeners(currValve);
                            int btnId = getView().addStateRadioButton(currValve.getState(),
                                    String.format(Locale.ENGLISH, "#%d", currValve.getIndex()));
                            addToBtnMaps(btnId, currValve.getId());
                        }
                    } else {
                        getView().showMessage("No Valves Found");
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
                if(propertyName.equals(Valve.PROPERTY_DURATION) || propertyName.equals(Valve.PROPERTY_LAST_ON)) {
                    Integer btnId;
                    if ((btnId = mBtnMap.get(updatedValve.getId())) != null) {
                        mView.updateStateRadioButton(btnId, updatedValve.getState());
                    }
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
                    getView().showMessage(ex.getMessage());
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
        mRouter.showEmptyFragment();

        if (mBtnMap == null) {
            mBtnMap = new HashMap<>();
        }
        if (mBtnMapInverse == null) {
            mBtnMapInverse = new HashMap<>();
        }

        populateValves();
    }

    @Override
    public void onButtonCommandClicked() {

    }

    @Override
    public void onButtonAddClicked() {
        final Valve valve = new Valve();
//        valve.setLastOnTime(Calendar.getInstance().getTime());
//        valve.setDuration(1800);
//        valve.setName("Fruits Valve");
//        valve.setState(Valve.STATE_ON);
        valve.setIndex(mValveMap.size() + 1);

        mDb.addValve(valve, new IDataBaseConnection.TaskListener<String>() {
            @Override
            public void onComplete(String answer, Exception ex) {
                valve.setId(answer);
                mValveMap.put(valve.getId(), valve);
                initValveDbListener(valve);
                int btnId = mView.addStateRadioButton(valve.getState(),
                        String.format(Locale.ENGLISH, "#%d", mValveMap.size()));
                addToBtnMaps(btnId, valve.getId());
            }
        });
    }

    @Override
    public void onStateRadioButtonClicked(int btnId) {
        Valve selectedValve;
        if ((selectedValve = mValveMap.get(mBtnMapInverse.get(btnId))) != null) {
           mRouter.showValveFragment(selectedValve);
        } else {
            getView().showMessage("This shouldn't happen!");
        }
    }
}
