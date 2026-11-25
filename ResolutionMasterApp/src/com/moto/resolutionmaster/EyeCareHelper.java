package com.moto.resolutionmaster;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.Locale;

public class EyeCareHelper {

    private static final String TAG = "EyeCareHelper";
    private static final String PREF_NAME = "eye_care_prefs";
    private static final String KEY_DC_DIMMING = "key_dc_dimming_active";
    private static final String KEY_BLACK_FLOOR_ENABLED = "key_black_floor_enabled";
    private static final String KEY_BLACK_FLOOR_LEVEL = "key_black_floor_level";
    private static final String KEY_BLACK_FLOOR_ROOT_MODE = "key_black_floor_root_mode";

    private static final String KEY_COMBO_ACTIVE = "key_smearing_combo_active";
    private static final String KEY_SAVED_BRIGHTNESS_MODE = "key_saved_brightness_mode";
    private static final String KEY_SAVED_BRIGHTNESS_VAL = "key_saved_brightness_val";

    public static final String SYSFS_PANEL_DC = "/sys/class/drm/card0-DSI-1/panelDC";

    public static final int RESULT_OK_ROOT = 1;
    public static final int RESULT_OK_OVERLAY = 2;
    public static final int RESULT_DISABLED = 0;
    public static final int RESULT_NEED_OVERLAY_PERM = -1;

    public static boolean isDcDimmingActive(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean pref = sp.getBoolean(KEY_DC_DIMMING, false);
        try {
            int sys = Settings.System.getInt(context.getContentResolver(), "dc_dimming", -1);
            if (sys != -1) return sys == 1;
        } catch (Throwable ignored) {}
        return pref;
    }

    public static boolean toggleDcDimming(Context context) {
        boolean cur = isDcDimmingActive(context);
        boolean target = !cur;
        int val = target ? 1 : 0;

        String cmd = "settings put system dc_dimming " + val +
                     "; settings put global dc_dimming " + val +
                     "; echo " + val + " > " + SYSFS_PANEL_DC + " 2>/dev/null";
        boolean ok = ShellHelper.runCommand(context, cmd);

        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_DC_DIMMING, target).apply();
        return ok;
    }

    public static boolean isNightLightActive(Context context) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(), "night_display_activated", 0) == 1;
        } catch (Throwable t) {
            String out = ShellHelper.runCommandWithOutput(context, "settings get secure night_display_activated");
            return "1".equals(out != null ? out.trim() : "");
        }
    }

    public static boolean toggleNightLight(Context context) {
        boolean cur = isNightLightActive(context);
        int target = cur ? 0 : 1;
        boolean ok = ShellHelper.runCommand(context, "settings put secure night_display_activated " + target);
        if (!ok) {
            try {
                ok = Settings.Secure.putInt(context.getContentResolver(), "night_display_activated", target);
            } catch (Throwable ignored) {}
        }
        return ok;
    }

    public static boolean isExtraDimActive(Context context) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(), "reduce_bright_colors_activated", 0) == 1;
        } catch (Throwable t) {
            String out = ShellHelper.runCommandWithOutput(context, "settings get secure reduce_bright_colors_activated");
            return "1".equals(out != null ? out.trim() : "");
        }
    }

    public static boolean setExtraDimActive(Context context, boolean active) {
        int target = active ? 1 : 0;
        boolean ok = ShellHelper.runCommand(context, "settings put secure reduce_bright_colors_activated " + target);
        if (!ok) {
            try {
                ok = Settings.Secure.putInt(context.getContentResolver(), "reduce_bright_colors_activated", target);
            } catch (Throwable ignored) {}
        }
        return ok;
    }

    public static boolean toggleExtraDim(Context context) {
        return setExtraDimActive(context, !isExtraDimActive(context));
    }

    public static int getExtraDimLevel(Context context) {
        try {
            int val = Settings.Secure.getInt(context.getContentResolver(), "reduce_bright_colors_level", -1);
            if (val >= 0 && val <= 100) return val;
        } catch (Throwable ignored) {}
        return 50;
    }

    public static boolean setExtraDimLevel(Context context, int level) {
        if (level < 0) level = 0;
        if (level > 100) level = 100;
        boolean ok = ShellHelper.runCommand(context, "settings put secure reduce_bright_colors_level " + level);
        if (!ok) {
            try {
                ok = Settings.Secure.putInt(context.getContentResolver(), "reduce_bright_colors_level", level);
            } catch (Throwable ignored) {}
        }
        return ok;
    }

    public static int getHardwareBrightnessPercent(Context context) {
        try {
            int raw = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 80);
            int pct = Math.round((raw * 100f) / 255f);
            if (pct < 1) pct = 1;
            if (pct > 100) pct = 100;
            return pct;
        } catch (Throwable ignored) {
            return 30;
        }
    }

    public static boolean setHardwareBrightnessPercent(Context context, int percent) {
        if (percent < 5) percent = 5;
        if (percent > 100) percent = 100;
        int raw = Math.round((percent * 255f) / 100f);
        String cmd = "settings put system screen_brightness_mode 0; settings put system screen_brightness " + raw;
        boolean ok = ShellHelper.runCommand(context, cmd);
        if (!ok) {
            try {
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                ok = Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, raw);
            } catch (Throwable ignored) {}
        }
        return ok;
    }

    // ==================== Anti-Black Smearing: Bright + Extra Dim Combo ====================

    public static boolean isBrightExtraDimComboActive(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_COMBO_ACTIVE, false) && isExtraDimActive(context);
    }

    public static boolean toggleBrightExtraDimCombo(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean currentlyActive = isBrightExtraDimComboActive(context);
        if (!currentlyActive) {
            int savedMode = 1;
            int savedVal = 80;
            try {
                savedMode = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
                savedVal = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 80);
            } catch (Throwable ignored) {}

            sp.edit()
                .putInt(KEY_SAVED_BRIGHTNESS_MODE, savedMode)
                .putInt(KEY_SAVED_BRIGHTNESS_VAL, savedVal)
                .putBoolean(KEY_COMBO_ACTIVE, true)
                .apply();

            int curExtraDim = getExtraDimLevel(context);
            int targetExtraDim = curExtraDim < 55 ? 70 : curExtraDim;

            String cmd = "settings put system screen_brightness_mode 0; " +
                         "settings put system screen_brightness 77; " +
                         "settings put secure reduce_bright_colors_level " + targetExtraDim + "; " +
                         "settings put secure reduce_bright_colors_activated 1";
            boolean ok = ShellHelper.runCommand(context, cmd);
            if (!ok) {
                setHardwareBrightnessPercent(context, 30);
                setExtraDimLevel(context, targetExtraDim);
                setExtraDimActive(context, true);
            }
            return true;
        } else {
            int savedMode = sp.getInt(KEY_SAVED_BRIGHTNESS_MODE, 1);
            int savedVal = sp.getInt(KEY_SAVED_BRIGHTNESS_VAL, 80);
            sp.edit().putBoolean(KEY_COMBO_ACTIVE, false).apply();

            String cmd = "settings put secure reduce_bright_colors_activated 0; " +
                         "settings put system screen_brightness " + savedVal + "; " +
                         "settings put system screen_brightness_mode " + savedMode;
            boolean ok = ShellHelper.runCommand(context, cmd);
            if (!ok) {
                setExtraDimActive(context, false);
            }
            return false;
        }
    }

    // ==================== Anti-Black Smearing: OLED Black Floor Lift ====================

    public static int getBlackFloorLevel(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int lvl = sp.getInt(KEY_BLACK_FLOOR_LEVEL, 4); // default #040404 (~+1.6%)
        if (lvl < 1) lvl = 4;
        if (lvl > 8) lvl = 8;
        return lvl;
    }

    public static void saveBlackFloorLevel(Context context, int level) {
        if (level < 1) level = 1;
        if (level > 8) level = 8;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_BLACK_FLOOR_LEVEL, level).apply();
    }

    public static boolean isBlackFloorActive(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean rootMode = sp.getBoolean(KEY_BLACK_FLOOR_ROOT_MODE, false);
        boolean enabled = sp.getBoolean(KEY_BLACK_FLOOR_ENABLED, false);
        if (rootMode && enabled) return true;
        return BlackFloorOverlayService.isRunning();
    }

    public static String getBlackFloorBackendLabel(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (sp.getBoolean(KEY_BLACK_FLOOR_ROOT_MODE, false) && sp.getBoolean(KEY_BLACK_FLOOR_ENABLED, false)) {
            return "SurfaceFlinger ColorMatrix (Root)";
        }
        if (BlackFloorOverlayService.isRunning()) {
            return "SurfaceFlinger HWC Layer (без Root)";
        }
        return "Выкл";
    }

    public static String formatBlackFloorLabel(boolean active, int level) {
        if (!active) {
            return "Выкл (#000000)";
        }
        float pct = (level * 100f) / 255f;
        return String.format(Locale.US, "+%.1f%% (#%02X%02X%02X)", pct, level, level, level);
    }

    public static int applyBlackFloor(Context context, boolean enable, int level) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!enable) {
            // Stop overlay service
            try {
                Intent stopIntent = new Intent(context, BlackFloorOverlayService.class);
                stopIntent.setAction(BlackFloorOverlayService.ACTION_STOP);
                context.startService(stopIntent);
            } catch (Throwable ignored) {}

            // Reset SurfaceFlinger matrix if root was active
            if (sp.getBoolean(KEY_BLACK_FLOOR_ROOT_MODE, false) || ShellHelper.isRootAvailable()) {
                ShellHelper.runCommand(context, "service call SurfaceFlinger 1015 i32 0");
            }

            sp.edit()
                .putBoolean(KEY_BLACK_FLOOR_ENABLED, false)
                .putBoolean(KEY_BLACK_FLOOR_ROOT_MODE, false)
                .apply();
            return RESULT_DISABLED;
        }

        if (level < 1) level = 1;
        if (level > 8) level = 8;
        saveBlackFloorLevel(context, level);

        // Path A: Try direct SurfaceFlinger ColorMatrix when Root is available
        if (ShellHelper.isRootAvailable()) {
            float offset = level / 255.0f;
            float scale = 1.0f - offset;
            String sfCmd = String.format(Locale.US,
                "service call SurfaceFlinger 1015 i32 1 " +
                "f %.6f f 0 f 0 f 0 " +
                "f 0 f %.6f f 0 f 0 " +
                "f 0 f 0 f %.6f f 0 " +
                "f %.6f f %.6f f %.6f f 1",
                scale, scale, scale, offset, offset, offset);
            String out = ShellHelper.runCommandWithOutput(context, sfCmd);
            if (out != null && !out.contains("Error") && !out.contains("not permitted")) {
                // Stop overlay if it was running
                try {
                    Intent stopIntent = new Intent(context, BlackFloorOverlayService.class);
                    stopIntent.setAction(BlackFloorOverlayService.ACTION_STOP);
                    context.startService(stopIntent);
                } catch (Throwable ignored) {}

                sp.edit()
                    .putBoolean(KEY_BLACK_FLOOR_ENABLED, true)
                    .putBoolean(KEY_BLACK_FLOOR_ROOT_MODE, true)
                    .apply();
                return RESULT_OK_ROOT;
            }
        }

        // Path B: Non-root / Shizuku hardware-composited SurfaceFlinger overlay layer
        if (!Settings.canDrawOverlays(context)) {
            // Try auto-granting SYSTEM_ALERT_WINDOW via Shizuku
            if (ShellHelper.isShizukuPermissionGranted()) {
                ShellHelper.runCommand(context, "appops set " + context.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
            }
        }

        if (!Settings.canDrawOverlays(context)) {
            return RESULT_NEED_OVERLAY_PERM;
        }

        try {
            Intent startIntent = new Intent(context, BlackFloorOverlayService.class);
            startIntent.setAction(BlackFloorOverlayService.ACTION_START);
            startIntent.putExtra(BlackFloorOverlayService.EXTRA_LEVEL, level);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            } else {
                context.startService(startIntent);
            }

            sp.edit()
                .putBoolean(KEY_BLACK_FLOOR_ENABLED, true)
                .putBoolean(KEY_BLACK_FLOOR_ROOT_MODE, false)
                .apply();
            return RESULT_OK_OVERLAY;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start BlackFloorOverlayService", t);
            return RESULT_NEED_OVERLAY_PERM;
        }
    }
}
