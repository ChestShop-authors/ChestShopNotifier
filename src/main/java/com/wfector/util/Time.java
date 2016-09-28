package com.wfector.util;

import java.util.Date;

public class Time {

    public static long GetEpochTime() {
        Date dt = new Date();

        return (dt.getTime() / 1000);
    }

    public static String GetAgo(Integer seconds) {

        Date dt = new Date();
        Integer CurrentTime = (int) (dt.getTime() / 1000);

        Integer Difference = (CurrentTime - seconds);

        String Timestamp = "";

        if(Math.floor(Difference / 86400) > 0) {
            Integer Days = (int) Math.floor(Difference / 86400);
            Timestamp += Days.toString() + "d";
            Difference = (Difference - (Days * 86400));
        }

        if(Math.floor(Difference / 3600) > 0) {
            Integer Hours = (int) Math.floor(Difference / 3600);
            Timestamp += Hours.toString() + "h";
            Difference = (Difference - (Hours * 3600));
        }

        if(Math.floor(Difference / 60) > 0) {
            Integer Minutes = (int) Math.floor(Difference / 60);
            Timestamp += Minutes.toString() + "m";
            Difference = (Difference - (Minutes * 60));
        }

        if(Timestamp == "") {
            Timestamp = Difference.toString() + "s";
        }

        return Timestamp;
    }
}
