package com.application.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {
    public static String getNowTime(){
        Date date = new Date();
        String stime = "";
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        stime = df.format(date);
        return stime;
    }
}
