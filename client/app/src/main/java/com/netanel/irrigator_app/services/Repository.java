package com.netanel.irrigator_app.services;


import android.util.Log;

import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.model.Module;
import com.netanel.irrigator_app.model.Sensor;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection.Direction;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection.TaskListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<String, String> mPathSensors;

    private final IDataBaseConnection mConnection;
    private final String mSystemId = "a4MgpJK45g5l9lMEErhS";

    private Map<String, Module> mValves;

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
                            mValves = new LinkedHashMap<>();
                            initModulesDbListener();
                            for (int i = 0; i < result.size(); i++) {
                                Module module = result.get(i);
                                mValves.put(module.getId(),module);
                                int idx = i;
                                initSensorsPath(module);
                                mConnection.getCollection(mPathSensors.get(module.getId()), Sensor.class).get(
                                        new IDataBaseConnection.TaskListener<List<Sensor>>(){
                                            @Override
                                            public void onComplete(List<Sensor> sensors) {
                                                if (!sensors.isEmpty()) {
                                                    module.setSensors(sensors);
                                                    initSensorsDbListener(module);
                                                }

                                                if (idx == mValves.size() - 1) {
                                                    taskCompletedListener.onComplete(new ArrayList<>(mValves.values()));
                                                }
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
            taskCompletedListener.onComplete(new ArrayList<>(mValves.values()));
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

    private void initModulesDbListener() {
        mConnection.addCollectionListener(mPathModules, Module.class,
                new IDataBaseConnection.TaskListener<List<Module>>() {
                    @Override
                    public void onComplete(List<Module> updatedModules) {
                        for (Module updated :
                                updatedModules) {
                            Module module = mValves.get(updated.getId());
                            module.update(updated);
                        }
                    }

                    @Override
                    public void onFailure(Exception ex) {
                        logInitListenerEx(ex);
                    }
                });
    }

    private void initSensorsDbListener(@NonNull final Module module) {
            mConnection.addCollectionListener(mPathSensors.get(module.getId()), Sensor.class,
                    new TaskListener<List<Sensor>>() {
                @Override
                public void onComplete(List<Sensor> updatedSensors) {
                    for (Sensor updated :
                            updatedSensors) {
                        Sensor sensor = module.getSensor(updated.getId());
                        sensor.update(updated);
                    }
                }

                @Override
                public void onFailure(Exception exception) {
                    logInitListenerEx(exception);
                }
            });
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
