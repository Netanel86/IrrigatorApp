package com.netanel.irrigator_app;

import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.services.AppServices;
import com.netanel.irrigator_app.services.connection.FirebaseConnection;
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

public class ManualFragViewModel extends ViewModel
        implements ManualFragContract.IViewModel {

    private ManualFragContract.IView mView;

    private IDataBaseConnection mDb;
    private Map<String, Valve> mValveMap;

    private Map<String, Integer> mBtnMap;
    private Map<Integer, String> mBtnMapInverse;

    private Valve mSelectedValve;

    public ManualFragViewModel() {
        mDb = new FirebaseConnection();
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
            public void OnStateChanged(Valve updatedValve) {
                Integer btnId;
                if ((btnId = mBtnMap.get(updatedValve.getId())) != null) {
                    mView.updateStateRadioButton(btnId, updatedValve.getState());
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

    // ManualFragContract.IViewModel implementation
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
    public void onButtonCommandClicked() {
        final Command cmnd = new Command();
        cmnd.setValveID(mValveMap.get(0).getId());
        cmnd.setDuration(30);
        mDb.addCommand(cmnd, new IDataBaseConnection.TaskListener<Command>() {
            @Override
            public void onComplete(Command answer, Exception ex) {
                // TODO: 24/09/2020 handle successful request registration
//                String.format(
//                        "Request to turn ON valve #%d, for %d seconds has registered successfully",
//                        mValvesMap.ge);
            }
        });
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
        if ((mSelectedValve = mValveMap.get(mBtnMapInverse.get(btnId))) != null) {
            getView().showValvePage(mSelectedValve.getName(), mSelectedValve.getState(), (int) mSelectedValve.getTimeLeftOn(),mSelectedValve);
        } else {
            getView().showMessage("This shouldn't happen!");
        }
    }
}
