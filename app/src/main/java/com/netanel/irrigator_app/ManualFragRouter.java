package com.netanel.irrigator_app;


import android.os.Bundle;

import com.netanel.irrigator_app.model.Valve;
import com.netanel.irrigator_app.services.AppServices;

import java.lang.ref.WeakReference;
import java.util.Objects;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 12/10/2020
 */

public class ManualFragRouter {
    private WeakReference<MainActivity> contextRef;

    private String lastShownValveId = null;

    public ManualFragRouter(MainActivity context){
        contextRef = new WeakReference<>(context);
    }

    public void showEmptyFragment() {
        contextRef.get().getSupportFragmentManager().beginTransaction()
                .add(
                        R.id.fragment_container_view,
                        new Fragment(R.layout.valve_empty_layout),
                        "Empty").commit();
    }

    public void showValveFragment(Valve valve) {
        FragmentManager fragmentManager = contextRef.get().getSupportFragmentManager();
        Fragment lastShownFragment = fragmentManager.findFragmentByTag(lastShownValveId);
        Fragment currFragment;
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if(lastShownFragment != null) {
            transaction.hide(lastShownFragment);
        }else {
            transaction.remove(Objects.requireNonNull(fragmentManager.findFragmentByTag("Empty")));
        }

        lastShownValveId = valve.getId();

        if ((currFragment = fragmentManager.findFragmentByTag(valve.getId())) != null) {
            transaction.show(currFragment).commit();
        } else {
            currFragment = initNewValveFragment(valve);
            transaction.add(R.id.fragment_container_view, currFragment, valve.getId()).commit();
        }
    }

    public Valve getFragmentArguments() {
        FragmentManager fragmentManager = contextRef.get().getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(lastShownValveId);
        Valve valve = null;
        if(fragment != null && fragment.getArguments() != null) {
            valve = AppServices.getInstance()
                    .getJsonParser().fromJson(
                            fragment.getArguments().getString(ValveFragment.BUNDLE_VALVE),
                            Valve.class);
        }
        return valve;
    }

    private Fragment initNewValveFragment(final Valve valve) {
        ValveFragment newFragment = new ValveFragment(new ValveFragPresenter(this));
        String valveJson = AppServices.getInstance().getJsonParser().toJson(valve);

        Bundle bundle = new Bundle();
        bundle.putString(ValveFragment.BUNDLE_VALVE, valveJson);
        newFragment.setArguments(bundle);
        return newFragment;
    }
}
