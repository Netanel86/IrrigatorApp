package com.netanel.irrigator_app.services;

import com.netanel.irrigator_app.services.connection.FirebaseConnection;
import com.netanel.irrigator_app.services.connection.IDataBaseConnection;
import com.netanel.irrigator_app.services.json_parser.GsonParser;
import com.netanel.irrigator_app.services.json_parser.IJsonParser;

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

}
