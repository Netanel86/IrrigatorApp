package com.netanel.irrigator_app;


import android.app.Application;

import com.netanel.irrigator_app.services.AppServices;
import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.AndroidViewModel;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 13/10/2021
 */
public abstract class ObservableViewModel extends AndroidViewModel
        implements Observable {

    private PropertyChangeRegistry mCallBacks;

    /**
     * Empty constructor, with no application context.
     * No need to pass Application context as a parameter as it is called at this constructor
     * through AppServices. Use this constructor only when instantiation of the view model is called
     * outside of a view model factory, e.g in another view model.
     */
    public ObservableViewModel() {
        super(AppServices.getInstance().getApplication());
    }

    /**
     * A standard view model constructor.
     * This constructor should be used when instantiating the view model from a view model factory.
     *
     * @param application The application context
     */
    public ObservableViewModel(Application application) {
        super(application);
    }

    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        if (mCallBacks == null) {
            mCallBacks = new PropertyChangeRegistry();
        }

        mCallBacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        if (mCallBacks != null) {
            mCallBacks.remove(callback);
        }
    }

    protected void notifyPropertyChanged(int fieldId) {
        if (mCallBacks != null) {
            mCallBacks.notifyCallbacks(this, fieldId, null);
        }
    }

    @Override
    protected void onCleared() {
        mCallBacks.clear();
        super.onCleared();
    }
}
