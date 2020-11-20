package com.wfector.util;

import java.util.Date;

public class Time {

    public static long getEpochTime() {
        return new Date().getTime() / 1000;
    }

    public static String getAgo(int seconds) {
        int currentTime = (int) getEpochTime();

        int difference = (currentTime - seconds);

        StringBuilder timestampStr = new StringBuilder();

        if(Math.floor(difference / 86400) > 0) {
            int days = (int) Math.floor(difference / 86400);
            timestampStr.append(days).append("d ");
            difference = (difference - (days * 86400));
        }

        if(Math.floor(difference / 3600) > 0) {
            int hours = (int) Math.floor(difference / 3600);
            timestampStr.append(hours).append("h ");
            difference = (difference - (hours * 3600));
        }

        if(Math.floor(difference / 60) > 0) {
            int minutes = (int) Math.floor(difference / 60);
            timestampStr.append(minutes).append("m ");
            difference = (difference - (minutes * 60));
        }

        if(timestampStr.length() == 0) {
            timestampStr.append(difference).append("s ");
        }

        return timestampStr.toString();
    }
}
