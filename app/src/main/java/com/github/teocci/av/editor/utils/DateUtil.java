package com.github.teocci.av.editor.utils;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Apr-24
 */
public class DateUtil
{
    public static String convertSecondsToFormat(long seconds, String format)
    {
        if (TextUtils.isEmpty(format))
            return "";

        Date date = new Date(seconds);
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * second to HH:MM:ss
     *
     * @param seconds
     * @return
     */
    public static String convertSecondsToTime(long seconds)
    {
        String timeStr;
        int hour, minute, second;

        if (seconds <= 0)
            return "00:00";
        else {
            minute = (int) seconds / 60;
            if (minute < 60) {
                second = (int) seconds % 60;
                timeStr = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = (int) (seconds - hour * 3600 - minute * 60);
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    private static String unitFormat(int i)
    {
        return (i >= 0 && i < 10) ? "0" + Integer.toString(i) : "" + i;
    }
}
