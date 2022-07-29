package com.netanel.irrigator_app.model;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 22/03/2021
 */
// TODO: 01/04/2021 make commands immutable
// TODO: 27/07/2022 maybe implement builder class for commands
public class ValveCommand extends Command {

    private static final String COMMAND_ON = "on";
    private static final String COMMAND_OFF = "off";
    private static final String COMMAND_EDIT_DESCRIPTION = "edit_description";

    private final int mIndex;
    private int mDuration;
    private boolean mState;
    private String mDescription;

    public ValveCommand(int valveIndex, int duration) {
        this(valveIndex, Valve.ON);
        setDuration(duration);
    }

    public ValveCommand(int valveIndex, String description) {
        super();
        mIndex = valveIndex;
        setDescription(description);
    }

    public ValveCommand(int valveIndex, boolean state) {
        super();
        mIndex = valveIndex;
        setState(state);
    }

    public int getIndex() {
        return mIndex;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
        this.mCommandLog.remove(COMMAND_OFF);
        this.addUniqueCommandLog(COMMAND_ON);
    }

    public int getDuration() {
        return mDuration;
    }

    public void setState(boolean state) {
        if(!state) {
            this.mCommandLog.remove(COMMAND_ON);
            this.addUniqueCommandLog(COMMAND_OFF);
        }
        this.mState = state;
    }

    public boolean getState() {
        return mState;
    }

    public void setDescription(String description) {
        this.mDescription = description;
        this.addUniqueCommandLog(COMMAND_EDIT_DESCRIPTION);
    }

    public String getDescription() {
        return mDescription;
    }
}
