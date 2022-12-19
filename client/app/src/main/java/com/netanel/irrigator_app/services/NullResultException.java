package com.netanel.irrigator_app.services;


/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 27/12/2021
 */

public class NullResultException extends NullPointerException {
    public NullResultException(String targetPath) {
        super("Task: get " + targetPath + " returned an null result");
    }
}
