package com.netanel.irrigator_app.model;


import com.google.firebase.firestore.DocumentId;
import com.netanel.irrigator_app.connection.IMappable;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 15/09/2020
 */

public class Command implements IMappable {

    @DocumentId
    private String uID;

    private final Date mTimestamp;
    protected final Actions mAction;
    private final Map<String,Object> mAttributes;

    public Command(Actions action, Map<String,Object> attributes) {
        mTimestamp = Calendar.getInstance().getTime();
        mAction = action;
        mAttributes = attributes;
    }

    public String getId() {
        return uID;
    }

    public void setId(String uID) {
        this.uID = uID;
    }

    public Date getTimestamp() {
        return mTimestamp;
    }

    public Actions getAction() {
        return mAction;
    }

    public Map<String, Object> getAttributes() {
        return  mAttributes;
    }
}

