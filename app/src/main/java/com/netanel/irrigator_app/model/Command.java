package com.netanel.irrigator_app.model;


import com.google.firebase.firestore.DocumentId;
import com.netanel.irrigator_app.services.connection.IMappable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 15/09/2020
 */

public abstract class Command implements IMappable {

    @DocumentId
    private String uID;

    private final Date mTime;
    protected final List<String> mCommandLog;

    public Command() {
        mTime = Calendar.getInstance().getTime();
        mCommandLog = new ArrayList<>();
    }

    public String getId() {
        return uID;
    }

    public void setId(String uID) {
        this.uID = uID;
    }

    public Date getTime() {
        return mTime;
    }

    public List<String> getCommandLog() {
        return mCommandLog;
    }

    protected void addUniqueCommandLog(String registry) {
        if(!mCommandLog.contains(registry)) {
            mCommandLog.add(registry);
        }
    }
}

