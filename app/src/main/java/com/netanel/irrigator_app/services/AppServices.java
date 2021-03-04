package com.netanel.irrigator_app.services;

import com.netanel.irrigator_app.ViewModelFactory;
import com.netanel.irrigator_app.services.connection.FirebaseConnection;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.services.json_parser.GsonParser;
import com.netanel.irrigator_app.services.json_parser.IJsonParser;

import java.io.IOException;

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

    private static AppServices sInstance;

    public static synchronized AppServices getInstance() {
        if(sInstance == null) {
            sInstance = new AppServices();
        }

        return sInstance;
    }

    private AppServices(){}

    private IJsonParser mJsonParser;
    private IDataBaseConnection mDatabaseConnection;
    private ViewModelProvider.AndroidViewModelFactory mViewModelFactory;

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

    public ViewModelProvider.AndroidViewModelFactory getViewModelFactory(){
        if(mViewModelFactory == null) {
            throw new NullPointerException("ViewModelFactory has not been instantiated");
        }

        return mViewModelFactory;
    }

    public void setViewModelFactory(ViewModelProvider.AndroidViewModelFactory factory){
        if(mViewModelFactory != null) {
            throw new RuntimeException("ViewModelFactory has already been instantiated");
        }
        mViewModelFactory = factory;
    }

}
