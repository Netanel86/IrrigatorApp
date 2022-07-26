package com.netanel.irrigator_app.model;


import androidx.annotation.NonNull;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 30/12/2021
 */

public abstract class Observable {
    private PropertyChangedRegistry mPropertyChangedRegistry;

    public ListenerRegistration addPropertyChangedListener(@NonNull PropertyChangedListener propertyChangedListener) {
        if (mPropertyChangedRegistry == null) {
            mPropertyChangedRegistry = new PropertyChangedRegistry();
        }
        return mPropertyChangedRegistry.add(propertyChangedListener);
    }

    public void removePropertyChangedListener(@NonNull PropertyChangedListener propertyChangedListener) {
        if(mPropertyChangedRegistry != null) {
            mPropertyChangedRegistry.remove(propertyChangedListener);
        }
    }

    public void removeListenerRegistration(@NonNull ListenerRegistration listenerRegistration) {
            listenerRegistration.remove();
    }

    public void clearPropertyChangedRegistry() {
        if(mPropertyChangedRegistry != null) {
            mPropertyChangedRegistry.clear();
        }
    }

    protected void notifyPropertyChanged(int propertyId, Object oldValue, Object newValue) {
        if (mPropertyChangedRegistry != null) {
            mPropertyChangedRegistry.notifyListeners(this, propertyId, oldValue, newValue);
        }
    }
}
