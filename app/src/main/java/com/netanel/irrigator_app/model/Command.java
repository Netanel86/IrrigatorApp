package com.netanel.irrigator_app.model;


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

    private String ValveID;
    private int Duration;

    public String getId() {
        return uID;
    }

    public void setId(String uID) {
        this.uID = uID;
    }

    public String getValveID() {
        return ValveID;
    }

    public void setValveID(String valveID) {
        ValveID = valveID;
    }

    public int getDuration() {
        return Duration;
    }

    public void setDuration(int duration) {
        Duration = duration;
    }
}
