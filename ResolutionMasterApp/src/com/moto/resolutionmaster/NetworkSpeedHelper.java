package com.moto.resolutionmaster;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.util.Locale;

public class NetworkSpeedHelper {

    private static final String TAG = "NetworkSpeedHelper";
    private static final String PREF_NAME = "net_speed_prefs";

    private static final String KEY_ENABLED = "key_net_speed_enabled";
    private static final String KEY_HIDE_IDLE = "key_net_speed_hide_idle";
    private static final String KEY_AUTOSTART = "key_net_speed_autostart";
    private static final String KEY_BITS_MODE = "key_net_speed_bits_mode";
    private static final String KEY_ICON_MODE = "key_net_speed_icon_mode_v2";
    private static final String KEY_OVERLAY_OFFSET_DP = "key_net_speed_offset_dp_v2";
    private static final String KEY_NATIVE_ROOT = "key_net_speed_native_root";

    // Resolution Profiles for Per-Resolution Offset Calibration
    public static final int RES_PROFILE_QHD = 0; // >= 1400 px width
    public static final int RES_PROFILE_FHD = 1; // 900..1200 px width
    public static final int RES_PROFILE_HD = 2;  // <= 800 px width

    public static final int DEFAULT_OFFSET_QHD = 180;
    public static final int DEFAULT_OFFSET_FHD = 140;
    public static final int DEFAULT_OFFSET_HD = 172;

    private static final String KEY_OFFSET_QHD = "key_net_speed_offset_qhd_v3";
    private static final String KEY_OFFSET_FHD = "key_net_speed_offset_fhd_v3";
    private static final String KEY_OFFSET_HD = "key_net_speed_offset_hd_v3";

    public static final int ICON_MODE_RIGHT_OVERLAY = 0; // Fixed right side near battery (never shifts!)
    public static final int ICON_MODE_TWO = 1;           // Left: 2 icons (↓ Down + ↑ Up)
    public static final int ICON_MODE_SINGLE_SPLIT = 2;  // Left: 1 icon (↓ / ↑ stacked)

    public static final int RESULT_DISABLED = 0;
    public static final int RESULT_OK_SERVICE = 1;
    public static final int RESULT_OK_NATIVE_ROOT = 2;
    public static final int RESULT_NEED_NOTIF_PERM = -1;
    public static final int RESULT_NEED_OVERLAY_PERM = -2;

    public static boolean isEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (sp.getBoolean(KEY_NATIVE_ROOT, false) && sp.getBoolean(KEY_ENABLED, false)) {
            return true;
        }
        return NetworkSpeedService.isRunning() || sp.getBoolean(KEY_ENABLED, false);
    }

    public static boolean isActiveNow(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (sp.getBoolean(KEY_NATIVE_ROOT, false) && sp.getBoolean(KEY_ENABLED, false)) {
            return true;
        }
        return NetworkSpeedService.isRunning();
    }

    public static boolean isNativeRootActive(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_NATIVE_ROOT, false) && sp.getBoolean(KEY_ENABLED, false);
    }

    public static boolean isHideOnIdleEnabled(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HIDE_IDLE, false);
    }

    public static void setHideOnIdleEnabled(Context context, boolean hide) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HIDE_IDLE, hide).apply();
        if (NetworkSpeedService.isRunning()) {
            refreshService(context);
        }
    }

    public static boolean isAutoStartEnabled(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTOSTART, true);
    }

    public static void setAutoStartEnabled(Context context, boolean autoStart) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTOSTART, autoStart).apply();
    }

    public static boolean isBitsMode(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BITS_MODE, false);
    }

    public static void setBitsMode(Context context, boolean bits) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_BITS_MODE, bits).apply();
        if (NetworkSpeedService.isRunning()) {
            refreshService(context);
        }
    }

    public static int getIconMode(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_ICON_MODE, ICON_MODE_RIGHT_OVERLAY);
    }

    public static int cycleIconMode(Context context) {
        int next = (getIconMode(context) + 1) % 3;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ICON_MODE, next).apply();
        if (next == ICON_MODE_RIGHT_OVERLAY) {
            tryGrantOverlayPermission(context);
        }
        if (NetworkSpeedService.isRunning()) {
            refreshService(context);
        }
        return next;
    }

    public static String getIconModeLabel(int mode) {
        if (mode == ICON_MODE_RIGHT_OVERLAY) {
            return "Справа у батареи (Фикс. ↓/↑)";
        } else if (mode == ICON_MODE_TWO) {
            return "Слева: 2 иконки (↓ Вход + ↑ Исход)";
        } else {
            return "Слева: 1 иконка (↓ и ↑ вместе)";
        }
    }

    public static int getResProfile(Context context) {
        if (context == null) return RES_PROFILE_QHD;
        android.graphics.Point p = DisplayManagerHelper.getCurrentResolution(context);
        int minRes = (p != null) ? Math.min(p.x, p.y) : 1440;
        if (minRes > 1080) {
            return RES_PROFILE_QHD;
        } else if (minRes > 720) {
            return RES_PROFILE_FHD;
        } else {
            return RES_PROFILE_HD;
        }
    }

    public static String getResProfileLabel(int profile) {
        if (profile == RES_PROFILE_QHD) return "QHD+ (1440p)";
        if (profile == RES_PROFILE_FHD) return "FHD+ (1080p)";
        return "HD+ (720p)";
    }

    public static int getOverlayOffsetDp(Context context) {
        return getOverlayOffsetDpForProfile(context, getResProfile(context));
    }

    public static int getOverlayOffsetDpForProfile(Context context, int profile) {
        if (context == null) return DEFAULT_OFFSET_QHD;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (profile == RES_PROFILE_QHD) {
            return sp.getInt(KEY_OFFSET_QHD, DEFAULT_OFFSET_QHD);
        } else if (profile == RES_PROFILE_FHD) {
            return sp.getInt(KEY_OFFSET_FHD, DEFAULT_OFFSET_FHD);
        } else {
            return sp.getInt(KEY_OFFSET_HD, DEFAULT_OFFSET_HD);
        }
    }

    public static void setOverlayOffsetDp(Context context, int dp) {
        setOverlayOffsetDpForProfile(context, getResProfile(context), dp);
    }

    public static void setOverlayOffsetDpForProfile(Context context, int profile, int dp) {
        if (context == null) return;
        if (dp < 15) dp = 15;
        if (dp > 240) dp = 240;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (profile == RES_PROFILE_QHD) {
            sp.edit().putInt(KEY_OFFSET_QHD, dp).apply();
        } else if (profile == RES_PROFILE_FHD) {
            sp.edit().putInt(KEY_OFFSET_FHD, dp).apply();
        } else {
            sp.edit().putInt(KEY_OFFSET_HD, dp).apply();
        }
        if (NetworkSpeedService.isRunning()) {
            refreshService(context);
        }
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < 33) {
            return true;
        }
        return context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
            == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean tryGrantNotificationPermission(Context context) {
        if (hasNotificationPermission(context)) return true;
        if (ShellHelper.isShizukuPermissionGranted() || ShellHelper.isRootAvailable()) {
            ShellHelper.runCommand(context,
                "pm grant " + context.getPackageName() + " android.permission.POST_NOTIFICATIONS");
        }
        return hasNotificationPermission(context);
    }

    public static boolean tryGrantOverlayPermission(Context context) {
        if (Settings.canDrawOverlays(context)) return true;
        if (ShellHelper.isShizukuPermissionGranted() || ShellHelper.isRootAvailable()) {
            ShellHelper.runCommand(context,
                "appops set " + context.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
        }
        return Settings.canDrawOverlays(context);
    }

    public static int setEnabled(Context context, boolean enable) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        if (!enable) {
            try {
                Intent stopIntent = new Intent(context, NetworkSpeedService.class);
                stopIntent.setAction(NetworkSpeedService.ACTION_STOP);
                context.startService(stopIntent);
            } catch (Throwable ignored) {}

            if (sp.getBoolean(KEY_NATIVE_ROOT, false) && ShellHelper.isRootAvailable()) {
                ShellHelper.runCommand(context,
                    "content insert --uri content://com.motorola.android.providers.settings/global " +
                    "--bind name:s:internet_speed_switch --bind value:s:0");
            }

            sp.edit()
                .putBoolean(KEY_ENABLED, false)
                .putBoolean(KEY_NATIVE_ROOT, false)
                .apply();
            return RESULT_DISABLED;
        }

        // If Root is available, try Motorola's native right-side SystemUI network_speed switch first
        if (ShellHelper.isRootAvailable()) {
            String out = ShellHelper.runCommandWithOutput(context,
                "content insert --uri content://com.motorola.android.providers.settings/global " +
                "--bind name:s:internet_speed_switch --bind value:s:1");
            if (out != null && !out.contains("SecurityException") && !out.contains("Error")) {
                sp.edit()
                    .putBoolean(KEY_ENABLED, true)
                    .putBoolean(KEY_NATIVE_ROOT, true)
                    .apply();
                return RESULT_OK_NATIVE_ROOT;
            }
        }

        int mode = getIconMode(context);
        if (mode == ICON_MODE_RIGHT_OVERLAY) {
            if (!tryGrantOverlayPermission(context)) {
                return RESULT_NEED_OVERLAY_PERM;
            }
        } else {
            if (!tryGrantNotificationPermission(context)) {
                return RESULT_NEED_NOTIF_PERM;
            }
        }

        // Also grant notification perm silently if possible so foreground stats show in shade
        tryGrantNotificationPermission(context);

        try {
            Intent startIntent = new Intent(context, NetworkSpeedService.class);
            startIntent.setAction(NetworkSpeedService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            } else {
                context.startService(startIntent);
            }
            sp.edit()
                .putBoolean(KEY_ENABLED, true)
                .putBoolean(KEY_NATIVE_ROOT, false)
                .apply();
            return RESULT_OK_SERVICE;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start NetworkSpeedService", t);
            return mode == ICON_MODE_RIGHT_OVERLAY ? RESULT_NEED_OVERLAY_PERM : RESULT_NEED_NOTIF_PERM;
        }
    }

    public static void refreshService(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(context, NetworkSpeedService.class);
            intent.setAction(NetworkSpeedService.ACTION_REFRESH);
            context.startService(intent);
        } catch (Throwable ignored) {}
    }

    public static void refreshServiceDelayed(final Context context, long delayMs) {
        if (context == null) return;
        final Context appCtx = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshService(appCtx != null ? appCtx : context);
            }
        }, delayMs);
    }

    public static String formatSpeedInline(long bytesPerSec, boolean bitsMode) {
        if (bitsMode) {
            double bits = bytesPerSec * 8.0;
            if (bits < 1000.0) {
                return String.format(Locale.US, "%d б/с", Math.round(bits));
            } else if (bits < 1_000_000.0) {
                double kb = bits / 1000.0;
                return kb >= 100.0
                    ? String.format(Locale.US, "%d Кбит/с", Math.round(kb))
                    : String.format(Locale.US, "%.1f Кбит/с", kb);
            } else {
                double mb = bits / 1_000_000.0;
                return mb >= 100.0
                    ? String.format(Locale.US, "%d Мбит/с", Math.round(mb))
                    : String.format(Locale.US, "%.1f Мбит/с", mb);
            }
        } else {
            if (bytesPerSec < 1024L) {
                return bytesPerSec + " Б/с";
            } else if (bytesPerSec < 1024L * 1024L) {
                double kb = bytesPerSec / 1024.0;
                return kb >= 100.0
                    ? String.format(Locale.US, "%d КБ/с", Math.round(kb))
                    : String.format(Locale.US, "%.1f КБ/с", kb);
            } else {
                double mb = bytesPerSec / (1024.0 * 1024.0);
                return mb >= 100.0
                    ? String.format(Locale.US, "%d МБ/с", Math.round(mb))
                    : String.format(Locale.US, "%.1f МБ/с", mb);
            }
        }
    }

    public static String formatBytesShort(long bytes) {
        if (bytes < 0) bytes = 0;
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.US, "%.0f КБ", bytes / 1024.0);
        } else if (bytes < 1024L * 1024L * 1024L) {
            return String.format(Locale.US, "%.1f МБ", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.US, "%.2f ГБ", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
