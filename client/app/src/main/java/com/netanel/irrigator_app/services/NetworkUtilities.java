package com.netanel.irrigator_app.services;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 01/03/2021
 */

public class NetworkUtilities {

    public static boolean isOnline(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnected();
    }

    public static void registerConnectivityCallback(Context context, ConnectivityManager.NetworkCallback callback) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connMgr.registerDefaultNetworkCallback(callback);
        } else {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
            connMgr.registerNetworkCallback(request, callback);
        }
    }

    public static void unregisterConnectivityCallback(Context context, ConnectivityManager.NetworkCallback callback) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        connMgr.unregisterNetworkCallback(callback);
    }

}
