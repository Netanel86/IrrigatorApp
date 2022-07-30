package com.netanel.irrigator_app.services;

import android.app.Application;
import android.util.Log;

import com.netanel.irrigator_app.services.connection.FirebaseConnection;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.services.json_parser.GsonParser;
import com.netanel.irrigator_app.services.json_parser.IJsonParser;

import androidx.lifecycle.ViewModelProvider;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 01/10/2020
 */

public class AppServices {
    private final static String TAG = AppServices.class.getSimpleName();

    private static AppServices sInstance;

    public static synchronized AppServices getInstance() {
        if(sInstance == null) {
            sInstance = new AppServices();
        }

        return sInstance;
    }

    private AppServices(){}

    private Repository mRepository;
    private IJsonParser mJsonParser;
    private IDataBaseConnection mDatabaseConnection;
    private ViewModelProvider.AndroidViewModelFactory mViewModelFactory;
    private Application mApplication;

    public IJsonParser getJsonParser() {
        if(mJsonParser == null) {
            mJsonParser = new GsonParser();
        }

        return mJsonParser;
    }

    public IDataBaseConnection getDbConnection() {
        if( mDatabaseConnection == null) {
            mDatabaseConnection = new FirebaseConnection();
        }

        return mDatabaseConnection;
    }

    public Application getApplication() {
        return mApplication;
    }

    public void setApplication(Application application) {
        mApplication = application;
    }

    public ViewModelProvider.AndroidViewModelFactory getViewModelFactory(){
        return mViewModelFactory;
    }

    public void setViewModelFactory(ViewModelProvider.AndroidViewModelFactory factory) {
        if (mViewModelFactory != null) {
            Log.w(TAG, "ViewModelFactory has already been instantiated");
        } else {
            mViewModelFactory = factory;
        }
    }

    public Repository getRepository() {
        if(mRepository == null) {
            mRepository = new Repository(getDbConnection());
        }
        return mRepository;
    }

}
