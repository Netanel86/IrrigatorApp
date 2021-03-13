package com.netanel.irrigator_app.model;


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

    private int mIndex;
    private int mDuration;
    private boolean mState;
    private Timestamp mTime;

    public Command() {
        mTime = Timestamp.now();
    }
    public String getId() {
        return uID;
    }

    public void setId(String uID) {
        this.uID = uID;
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndex(int valveId) {
        mIndex = valveId;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }


    public Timestamp getTime() {
        return mTime;
    }

    public void setTime(Timestamp timeCreated) {
        this.mTime = timeCreated;
    }

    public boolean isState() {
        return mState;
    }

    public void setState(boolean mState) {
        this.mState = mState;
    }
}
