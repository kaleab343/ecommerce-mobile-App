package com.example.econ2;

import android.app.ActivityManager;
import android.content.Context;
import android.widget.Toast;

import java.util.List;

public class SafeToast {

    public static void show(Context context, String message, int duration) {
        if (isAppInForeground(context)) {
            Toast.makeText(context, message, duration).show();
        } else {
            // App is not open — do nothing
        }
    }

    private static boolean isAppInForeground(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();

        if (runningProcesses == null) return false;

        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && processInfo.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
