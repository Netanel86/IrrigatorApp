package com.netanel.irrigator_app.model;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 04/11/2021
 */

public interface PropertyChangedListener {
    void onPropertyChanged(Object sender, int propertyId, Object oldValue, Object newValue);
}
