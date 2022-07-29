package com.netanel.irrigator_app.model;


import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 30/12/2021
 */

public class PropertyChangedRegistry {

    private final List<PropertyChangedListener> mPropertyChangedListeners;

    public PropertyChangedRegistry() {
        mPropertyChangedListeners = new ArrayList<>();
    }

    public ListenerRegistration add(PropertyChangedListener propertyChangedListener) {
        mPropertyChangedListeners.add(propertyChangedListener);
        return () -> remove(propertyChangedListener);
    }

    public void remove(PropertyChangedListener propertyChangedListener) {
        mPropertyChangedListeners.remove(propertyChangedListener);
    }

    public void clear() {
        mPropertyChangedListeners.clear();
    }

    protected void notifyListeners(Object sender, int propertyId, Object oldValue, Object newValue) {
        for (PropertyChangedListener listener :
                mPropertyChangedListeners) {
            listener.onPropertyChanged(sender, propertyId, oldValue, newValue);
        }
    }
}