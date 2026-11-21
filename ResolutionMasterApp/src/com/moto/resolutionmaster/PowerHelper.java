package com.moto.resolutionmaster;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

public class PowerHelper {

    private static final String TAG = "PowerHelper";
    private static final String PREF_NAME = "power_helper_prefs";
    private static final String KEY_BYPASS_ACTIVE = "key_bypass_charging_active";

    public static class BatteryInfo {
        public int level = 0;
        public float temperature = 0f;
        public boolean isCharging = false;
        public boolean isPlugged = false;
        public boolean isBypassActive = false;
        public String statusText = "";
    }

    public static boolean isBypassChargingActive(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_BYPASS_ACTIVE, false);
    }

    public static boolean setDirectPowerSupply(Context context, boolean enable) {
        try {
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            java.lang.reflect.Method getService = smClass.getMethod("getService", String.class);
            android.os.IBinder baseBinder = (android.os.IBinder) getService.invoke(null, "moto_extend_service");
            if (baseBinder != null) {
                android.os.IBinder binderToUse;
                if (ShellHelper.isShizukuPermissionGranted()) {
                    binderToUse = new rikka.shizuku.ShizukuBinderWrapper(baseBinder);
                } else {
                    binderToUse = baseBinder;
                }
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.motorola.misc.IMotoExtendService");
                    data.writeInt(enable ? 1 : 0);
                    boolean ok = binderToUse.transact(21, data, reply, 0);
                    reply.readException();
                    Log.i(TAG, "IMotoExtendService.setDirectPowerSupplyStatus(" + enable + ")=" + ok);
                    return ok;
                } finally {
                    data.recycle();
                    reply.recycle();
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to call setDirectPowerSupplyStatus via Binder", t);
        }
        return false;
    }

    public static boolean setBypassCharging(Context context, boolean enable) {
        boolean hwOk = setDirectPowerSupply(context, enable);
        boolean ok;
        if (enable) {
            ok = ShellHelper.runCommand(context, "dumpsys battery unplug; echo 0 > /sys/class/power_supply/battery/charging_enabled 2>/dev/null");
        } else {
            ok = ShellHelper.runCommand(context, "dumpsys battery reset; echo 1 > /sys/class/power_supply/battery/charging_enabled 2>/dev/null");
        }

        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_BYPASS_ACTIVE, enable).apply();
        return hwOk || ok;
    }

    public static BatteryInfo getBatteryInfo(Context context) {
        BatteryInfo info = new BatteryInfo();
        info.isBypassActive = isBypassChargingActive(context);

        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);

            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    info.level = Math.round((level / (float) scale) * 100);
                }

                int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                info.temperature = temp / 10.0f;

                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                info.isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                  status == BatteryManager.BATTERY_STATUS_FULL;

                int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                info.isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                                 plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                                 plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
            }
        } catch (Throwable t) {
            Log.e(TAG, "getBatteryInfo error", t);
        }

        if (info.isBypassActive) {
            info.statusText = "Сквозное питание (Bypass) • Сеть ⚡";
        } else if (info.isCharging) {
            info.statusText = "Зарядка от сети ⚡";
        } else if (info.isPlugged) {
            info.statusText = "Подключено • Батарея полная ✓";
        } else {
            info.statusText = "Питание от аккумулятора";
        }

        return info;
    }

    public static boolean isPowerSaveMode(Context context) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return pm.isPowerSaveMode();
            }
        } catch (Throwable ignored) {}
        try {
            return Settings.Global.getInt(context.getContentResolver(), "low_power", 0) == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean togglePowerSaveMode(Context context) {
        boolean cur = isPowerSaveMode(context);
        int target = cur ? 0 : 1;
        boolean ok = ShellHelper.runCommand(context, "settings put global low_power " + target);
        if (!ok) {
            try {
                ok = Settings.Global.putInt(context.getContentResolver(), "low_power", target);
            } catch (Throwable ignored) {}
        }
        return ok;
    }

    public static boolean launchBatteryCare(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.motorola.batterycare", "com.motorola.batterycare.smartbackground.activity.BackgroundSettingActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable t) {
            try {
                Intent fallback = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
                return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }
}
