package com.netanel.irrigator_app.model;


import android.renderscript.ScriptIntrinsicYuvToRGB;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 15/09/2020
 */

public class Command {

    @DocumentId
    private String uID;

    private String mValveId;
    private int mDuration;
    private Timestamp mTimeCreated;

    public Command() {
        mTimeCreated = Timestamp.now();
    }
    public String getId() {
        return uID;
    }

    public void setId(String uID) {
        this.uID = uID;
    }

    public String getValveId() {
        return mValveId;
    }

    public void setValveId(String valveId) {
        mValveId = valveId;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }


    public Timestamp getTimeCreated() {
        return mTimeCreated;
    }

    public void setTimeCreated(Timestamp timeCreated) {
        this.mTimeCreated = timeCreated;
    }
}
