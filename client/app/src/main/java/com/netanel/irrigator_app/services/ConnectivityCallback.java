package com.netanel.irrigator_app.services;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 06/03/2021
 */

public class ConnectivityCallback extends ConnectivityManager.NetworkCallback {

    private static final boolean CONNECTED = true;

    private final IConnectivityChangedCallback mCallback;
    private boolean mIsConnected;

    public ConnectivityCallback(@Nonnull IConnectivityChangedCallback callback, Context context){
        super();
        mCallback = callback;
        mIsConnected = NetworkUtilities.isOnline(context) == CONNECTED;
    }
    @Override
    public void onAvailable(@NonNull Network network) {
        if(!mIsConnected) {
            mCallback.onConnectivityChanged(CONNECTED);
        }
        mIsConnected = CONNECTED;
    }

    @Override
    public void onLost(@NonNull Network network) {
        if(mIsConnected) {
            mCallback.onConnectivityChanged(!CONNECTED);
        }
        mIsConnected = !CONNECTED;
    }

    public interface IConnectivityChangedCallback {
        void onConnectivityChanged(final boolean isConnected);
    }
}
