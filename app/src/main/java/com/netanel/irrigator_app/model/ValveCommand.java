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
public class ValveCommand extends Command {

    private static final String COMMAND_OPEN = "open";
    private static final String COMMAND_CLOSE = "close";
    private static final String COMMAND_EDIT_DESCRIPTION = "edit_description";

    private final int mIndex;
    private int mDuration;
    private boolean mIsOpen;
    private String mDescription;

    public ValveCommand(int valveIndex, int duration) {
        this(valveIndex, Valve.OPEN);
        setDuration(duration);
    }

    public ValveCommand(int valveIndex, String description) {
        super();
        mIndex = valveIndex;
        setDescription(description);
    }

    public ValveCommand(int valveIndex, boolean isOpen) {
        super();
        mIndex = valveIndex;
        setOpen(isOpen);
    }

    public int getIndex() {
        return mIndex;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
        this.mCommandLog.remove(COMMAND_CLOSE);
        this.addUniqueCommandLog(COMMAND_OPEN);
    }

    public int getDuration() {
        return mDuration;
    }

    public void setOpen(boolean isOpen) {
        if(!isOpen) {
            this.mCommandLog.remove(COMMAND_OPEN);
            this.addUniqueCommandLog(COMMAND_CLOSE);
        }
        this.mIsOpen = isOpen;
    }

    public boolean isOpen() {
        return mIsOpen;
    }

    public void setDescription(String description) {
        this.mDescription = description;
        this.addUniqueCommandLog(COMMAND_EDIT_DESCRIPTION);
    }

    public String getDescription() {
        return mDescription;
    }
}
