package com.netanel.irrigator_app.services.connection;


import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.model.Valve;

import java.util.List;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 21/09/2020
 */

public interface IDataBaseConnection {
    void getValves(TaskListener<List<Valve>> result);

    void addValve(final Valve valve, final TaskListener<String> onComplete);

    void addOnValveChangedListener(String valveId, OnDataChangedListener<Valve> dataChangedListener);

    void addCommand(Command command, TaskListener<Command> onComplete);

    interface TaskListener<T> {
        void onComplete(T answer, Exception ex);
    }

    interface OnDataChangedListener<T> {
        void onDataChanged(T changedObject, Exception ex);
    }


}
