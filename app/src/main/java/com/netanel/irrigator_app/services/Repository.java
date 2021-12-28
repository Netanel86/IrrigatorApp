package com.netanel.irrigator_app.services;


import android.util.Log;

import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.model.Sensor;
import com.netanel.irrigator_app.model.Valve;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection.Direction;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection.Path;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection.TaskListener;

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
    private static final String PROP_INDEX = "index";

    private final IDataBaseConnection mConnection;

    private List<Valve> mValves;
    private List<Sensor> mSensors;

    public Repository(IDataBaseConnection connection) {
        mConnection = connection;
    }

    public void getValves(@NonNull TaskListener<List<Valve>> taskCompletedListener) {
        if (mValves == null) {
            mConnection.getCollection(Path.VALVES, Valve.class)
                    .orderBy(PROP_INDEX, Direction.ASCENDING)
                    .get(new IDataBaseConnection.TaskListener<List<Valve>>() {
                        @Override
                        public void onComplete(List<Valve> result) {
                            mValves = result;
                            for (Valve valve :
                                    mValves) {
                                initValveDbListener(valve);
                            }
                            taskCompletedListener.onComplete(result);
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
                Path.COMMANDS, Command.class, new IDataBaseConnection.TaskListener<Command>() {
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

    private void initValveDbListener(@NonNull final Valve valve) {
        mConnection.addDocumentChangedListener(Path.VALVES, valve.getId(), Valve.class,
                new IDataBaseConnection.TaskListener<Valve>() {
                    @Override
                    public void onComplete(Valve updatedObject) {
                        valve.update(updatedObject);
                    }

                    @Override
                    public void onFailure(Exception ex) {
                        Log.w(TAG, ex.getMessage() != null ?
                                ex.getMessage() : "failed to initialize data base listener");
                    }
                });
    }
}
