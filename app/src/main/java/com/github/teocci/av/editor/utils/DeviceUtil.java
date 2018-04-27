package com.github.teocci.av.editor.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Apr-24
 */
public class DeviceUtil
{
    public static int getDeviceWidth()
    {
        return ContextUtils.getContext().getResources().getDisplayMetrics().widthPixels;
    }

    public static int getDeviceHeight()
    {
        return ContextUtils.getContext().getResources().getDisplayMetrics().heightPixels;
    }

    public static boolean hasAppInstalled(String pkgName)
    {
        try {
            ContextUtils.getContext().getPackageManager().getPackageInfo(pkgName, PERMISSION_GRANTED);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isAppRunInBackground()
    {
        ActivityManager activityManager = (ActivityManager) ContextUtils.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return false;

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(ContextUtils.getContext().getPackageName())) {
                // return true -> Run in background
                // return false -> Run in foreground
                return appProcess.importance != IMPORTANCE_FOREGROUND;
            }
        }
        return false;
    }
}
