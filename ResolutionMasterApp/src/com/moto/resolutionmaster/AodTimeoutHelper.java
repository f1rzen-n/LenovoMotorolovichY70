package com.moto.resolutionmaster;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

public class AodTimeoutHelper {

    private static final String PREF_NAME = "aod_timeout_prefs";
    public static final String KEY_ENABLED = "key_aod_timeout_enabled";
    public static final String KEY_TIMEOUT_SEC = "key_aod_timeout_seconds";
    public static final String KEY_KEEP_CHARGING = "key_aod_keep_on_charging";

    public static final int DEFAULT_TIMEOUT_SEC = 15;

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply();

        Intent intent = new Intent(context, AodTimeoutService.class);
        if (enabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }
            } catch (Throwable t) {
                try { context.startService(intent); } catch (Throwable ignored) {}
            }
        } else {
            try { context.stopService(intent); } catch (Throwable ignored) {}
            // Restore AOD to always on when feature is turned off
            restoreAodOn(context);
        }
    }

    public static int getTimeoutSeconds(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_TIMEOUT_SEC, DEFAULT_TIMEOUT_SEC);
    }

    public static void setTimeoutSeconds(Context context, int sec) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_TIMEOUT_SEC, sec).apply();
    }

    public static boolean isKeepOnCharging(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_KEEP_CHARGING, true);
    }

    public static boolean isKeepOnWhileCharging(Context context) {
        return isKeepOnCharging(context);
    }

    public static void setKeepOnCharging(Context context, boolean keep) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_KEEP_CHARGING, keep).apply();
    }

    public static void setKeepOnWhileCharging(Context context, boolean keep) {
        setKeepOnCharging(context, keep);
    }

    public static void restoreAodOn(Context context) {
        try {
            Settings.Secure.putInt(context.getContentResolver(), "doze_always_on", 1);
        } catch (Throwable ignored) {}
        ShellHelper.runCommand(context, "settings put secure doze_always_on 1");
    }

    public static void setDozeAlwaysOn(Context context, boolean on) {
        if (on) {
            restoreAodOn(context);
        } else {
            setAodOff(context);
        }
    }

    public static void setAodOff(Context context) {
        try {
            Settings.Secure.putInt(context.getContentResolver(), "doze_always_on", 0);
        } catch (Throwable ignored) {}
        ShellHelper.runCommand(context, "settings put secure doze_always_on 0");
    }
}
