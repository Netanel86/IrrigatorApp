package com.netanel.irrigator_app.services;


import android.util.Log;

import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.model.Module;
import com.netanel.irrigator_app.model.Sensor;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection.Direction;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection.TaskListener;

import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 26/12/2021
 */

public class Repository {
    private static final String TAG = Repository.class.getSimpleName();

    private static final String COLL_MODULES = "modules";
    private static final String COLL_SENSORS = "sensors";
    private static final String COLL_SYSTEMS = "systems";
    private static final String COLL_COMMANDS = "commands";

    private String mPathModules;
    private String mPathSystem;
    private String mPathCommands;
    private final HashMap<String, String> mPathSensors;

    private final IDataBaseConnection mConnection;
    private final String mSystemId = "a4MgpJK45g5l9lMEErhS";

    private List<Module> mValves;

    public Repository(IDataBaseConnection connection) {
        mConnection = connection;
        mPathSensors = new HashMap<>();
        initPaths();
    }

    public void getValves(@NonNull TaskListener<List<Module>> taskCompletedListener) {
        if (mValves == null) {
            mConnection.getCollection(mPathModules, Module.class)
                    .orderBy(Module.PROP_IP, Direction.ASCENDING)
                    .get(new IDataBaseConnection.TaskListener<List<Module>>() {
                        @Override
                        public void onComplete(List<Module> result) {
                            mValves = result;
                            for (int i = 0; i < mValves.size(); i++) {
                                Module module = mValves.get(i);
                                int idx = i;
                                initSensorsPath(module);
                                initValveDbListener(module);
                                mConnection.getCollection(mPathSensors.get(module.getId()), Sensor.class).get(
                                        new IDataBaseConnection.TaskListener<List<Sensor>>(){
                                            @Override
                                            public void onComplete(List<Sensor> sensors) {
                                                if(!sensors.isEmpty()) {
                                                    module.setSensors(sensors);
                                                    initSensorDbListeners(module);
                                                }

                                                if(idx == mValves.size()-1)
                                                    taskCompletedListener.onComplete(result);
                                            }

                                            @Override
                                            public void onFailure(Exception exception) {
                                                taskCompletedListener.onFailure(exception);
                                            }
                                        }
                                );
                            }
                        }

                        @Override
                        public void onFailure(Exception exception) {
                            taskCompletedListener.onFailure(exception);
                        }
                    });
        } else {
            taskCompletedListener.onComplete(mValves);
        }
    }

    public void addCommand(@NonNull Command command, TaskListener<Command> taskCompletedListener) {
        mConnection.addDocument(command,
                mPathCommands, Command.class, new IDataBaseConnection.TaskListener<Command>() {
                    @Override
                    public void onComplete(Command result) {
                        if (taskCompletedListener != null) {
                            taskCompletedListener.onComplete(result);
                        }
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        taskCompletedListener.onFailure(exception);
                    }
                });
    }

    private void initValveDbListener(@NonNull final Module module) {
        mConnection.addDocumentChangedListener(mPathModules, module.getId(), Module.class,
                new IDataBaseConnection.TaskListener<Module>() {
                    @Override
                    public void onComplete(Module updatedObject) {
                        module.update(updatedObject);
                    }

                    @Override
                    public void onFailure(Exception ex) {
                        logInitListenerEx(ex);
                    }
                });
    }

    private void initSensorDbListeners(@NonNull final Module module) {
        for (Sensor sensor :
                module.getSensors()) {
            mConnection.addDocumentChangedListener(mPathSensors.get(module.getId()), sensor.getId(), Sensor.class, new TaskListener<Sensor>() {
                @Override
                public void onComplete(Sensor result) {
                    sensor.update(result);
                }

                @Override
                public void onFailure(Exception exception) {
                    logInitListenerEx(exception);
                }
            });
        }
    }

    private void logInitListenerEx(Exception ex) {
        Log.w(TAG, ex.getMessage() != null ?
                ex.getMessage() : "failed to initialize data base listener");
    }

    private void initSensorsPath(Module module) {
        String path = String.format("%s/%s/%s",mPathModules, module.getId(),COLL_SENSORS);
        mPathSensors.put(module.getId(), path);
    }

    private void initPaths() {
        mPathSystem = String.format("%s/%s", COLL_SYSTEMS, mSystemId);
        mPathModules = String.format("%s/%s", mPathSystem, COLL_MODULES);
        mPathCommands = String.format("%s/%s", mPathSystem, COLL_COMMANDS);
    }
}
