package com.netanel.irrigator_app;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 24/10/2021
 */

interface IMultiStateView {
    void setActivated(boolean activated);
    void setEdited(boolean edited);
    void setEnabled(boolean enabled);
}
