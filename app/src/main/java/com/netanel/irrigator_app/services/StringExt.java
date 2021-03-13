package com.netanel.irrigator_app.services;


import android.content.Context;
import android.text.format.DateFormat;

import java.util.Locale;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 13/03/2021
 */

public class StringExt {
    public static final String EMPTY_LINE = "/n";
    public static final String SPACE = " ";
    public static final String COMMA = ",";

    private static final int HOUR_IN_SEC = 3600;
    private static final int DAY_IN_SEC = 3600 * 24;

    public static String formatSecToTimeString(int totalSeconds, String[] timeNames) {
        String sec = timeNames[0];
        String min = timeNames[1];
        String hr = timeNames[2];
        String day = timeNames[3];

        String timeFormat;
        if (totalSeconds < HOUR_IN_SEC) {
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            timeFormat = String.format(Locale.getDefault(),
                    "%02d:%02d %s", minutes, seconds, minutes > 0 ? min : sec);
        } else if (totalSeconds < DAY_IN_SEC) {
            int hours = totalSeconds / HOUR_IN_SEC;
            int minutes = (totalSeconds % HOUR_IN_SEC) / 60;
            int seconds = (totalSeconds % HOUR_IN_SEC) % 60;
            timeFormat = String.format(Locale.getDefault(),
                    "%02d:%02d:%02d %s", hours, minutes, seconds, hr);
        } else {
            int days = totalSeconds / DAY_IN_SEC;
            int hours = (totalSeconds % DAY_IN_SEC) / HOUR_IN_SEC;
            int minutes = ((totalSeconds % DAY_IN_SEC) % HOUR_IN_SEC) / 60;
            int seconds = ((totalSeconds % DAY_IN_SEC) % HOUR_IN_SEC) % 60;
            timeFormat = String.format(Locale.getDefault(),
                    "%d %s %02d:%02d:%02d %s",days ,day, hours, minutes, seconds, hr);
        }
        return timeFormat;
    }
}
