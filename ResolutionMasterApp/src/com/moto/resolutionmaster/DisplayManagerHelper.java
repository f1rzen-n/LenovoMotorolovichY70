package com.moto.resolutionmaster;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.Method;

public class DisplayManagerHelper {

    // Resolution Constants
    public static final int NATIVE_WIDTH = 1440;
    public static final int NATIVE_HEIGHT = 3168;
    public static final int NATIVE_DENSITY = 520;

    // Shared DPI Preferences (used by both MainActivity and ResolutionTileService)
    public static final String PREFS_DPI = "resolution_master_dpi";
    public static final String KEY_FHD_DPI = "fhd_dpi";
    public static final String KEY_QHD_DPI = "qhd_dpi";
    public static final String KEY_HD_DPI = "hd_dpi";

    public static final int QHD_WIDTH = NATIVE_WIDTH;
    public static final int QHD_HEIGHT = NATIVE_HEIGHT;
    public static final int QHD_DENSITY = NATIVE_DENSITY;

    public static final int FHD_WIDTH = 1080;
    public static final int FHD_HEIGHT = 2376;
    public static final int FHD_DENSITY = 390;

    public static final int HD_WIDTH = 720;
    public static final int HD_HEIGHT = 1584;
    public static final int HD_DENSITY = 260;

    // Refresh Rate Constants
    public static final float HZ_144 = 144.0f;
    public static final float HZ_120 = 120.0f;
    public static final float HZ_90 = 90.0f;
    public static final float HZ_60 = 60.0f;
    public static final float HZ_AUTO = 0.0f;

    private static boolean hiddenApiBypassed = false;

    public static synchronized void bypassHiddenApi(Context context) {
        if (!hiddenApiBypassed) {
            try {
                Method forName = Class.class.getDeclaredMethod("forName", String.class);
                Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);

                Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
                Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null);
                Method setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});

                Object vmRuntime = getRuntime.invoke(null);
                setHiddenApiExemptions.invoke(vmRuntime, new Object[]{new String[]{"L"}});
                hiddenApiBypassed = true;
            } catch (Throwable ignored) {}
        }

        if (context != null) {
            try {
                Settings.Global.putInt(context.getContentResolver(), "hidden_api_policy", 1);
            } catch (Throwable ignored) {}
        }
    }

    public static boolean setResolution(Context context, int width, int height, int density) {
        bypassHiddenApi(context);

        // 1. Native WindowManager reflection (Direct in-app, requires only WRITE_SECURE_SETTINGS)
        try {
            Class<?> wmgClass = Class.forName("android.view.WindowManagerGlobal");
            Method getWmsMethod = wmgClass.getMethod("getWindowManagerService");
            Object wms = getWmsMethod.invoke(null);

            if (wms != null) {
                Method setSize = wms.getClass().getMethod("setForcedDisplaySize", int.class, int.class, int.class);
                setSize.invoke(wms, Display.DEFAULT_DISPLAY, width, height);

                int userId = android.os.Process.myUid() / 100000;
                Method setDensity = wms.getClass().getMethod("setForcedDisplayDensityForUser", int.class, int.class, int.class);
                setDensity.invoke(wms, Display.DEFAULT_DISPLAY, density, userId);
                try {
                    NetworkSpeedHelper.refreshServiceDelayed(context, 150);
                    NetworkSpeedHelper.refreshServiceDelayed(context, 400);
                    NetworkSpeedHelper.refreshServiceDelayed(context, 800);
                } catch (Throwable ignored) {}
                return true;
            }
        } catch (Throwable t) {
            android.util.Log.e("ResolutionMaster", "Native setResolution failed, trying shell fallback", t);
        }

        // 2. Shell fallback (Root / Shizuku if available)
        String shellCmd = "wm size " + width + "x" + height + "; wm density " + density;
        boolean res = ShellHelper.runCommand(context, shellCmd);
        try {
            NetworkSpeedHelper.refreshServiceDelayed(context, 150);
            NetworkSpeedHelper.refreshServiceDelayed(context, 400);
            NetworkSpeedHelper.refreshServiceDelayed(context, 800);
        } catch (Throwable ignored) {}
        return res;
    }

    public static boolean resetResolution(Context context) {
        bypassHiddenApi(context);

        // 1. Native WindowManager reflection (Direct in-app, requires only WRITE_SECURE_SETTINGS)
        try {
            Class<?> wmgClass = Class.forName("android.view.WindowManagerGlobal");
            Method getWmsMethod = wmgClass.getMethod("getWindowManagerService");
            Object wms = getWmsMethod.invoke(null);

            if (wms != null) {
                Method clearSize = wms.getClass().getMethod("clearForcedDisplaySize", int.class);
                clearSize.invoke(wms, Display.DEFAULT_DISPLAY);

                int userId = android.os.Process.myUid() / 100000;
                Method clearDensity = wms.getClass().getMethod("clearForcedDisplayDensityForUser", int.class, int.class);
                clearDensity.invoke(wms, Display.DEFAULT_DISPLAY, userId);
                try {
                    NetworkSpeedHelper.refreshServiceDelayed(context, 150);
                    NetworkSpeedHelper.refreshServiceDelayed(context, 400);
                    NetworkSpeedHelper.refreshServiceDelayed(context, 800);
                } catch (Throwable ignored) {}
                return true;
            }
        } catch (Throwable t) {
            android.util.Log.e("ResolutionMaster", "Native resetResolution failed, trying shell fallback", t);
        }

        // 2. Shell fallback (Root / Shizuku if available)
        boolean res = ShellHelper.runCommand(context, "wm size reset; wm density reset");
        try {
            NetworkSpeedHelper.refreshServiceDelayed(context, 150);
            NetworkSpeedHelper.refreshServiceDelayed(context, 400);
            NetworkSpeedHelper.refreshServiceDelayed(context, 800);
        } catch (Throwable ignored) {}
        return res;
    }

    public static boolean setRefreshRate(Context context, float rate) {
        bypassHiddenApi(context);
        android.util.Log.d("ResolutionMaster", "setRefreshRate requested: " + rate);
        boolean success = false;

        // 1. Direct ContentProvider call (bypasses client-side whitelist, requires WRITE_SECURE_SETTINGS)
        try {
            ContentResolver cr = context.getContentResolver();
            Uri systemUri = Uri.parse("content://settings/system");
            Uri globalUri = Uri.parse("content://settings/global");

            if (rate <= 0.0f) {
                try {
                    cr.delete(Settings.System.CONTENT_URI, "name=?", new String[]{"min_refresh_rate"});
                    cr.delete(Settings.System.CONTENT_URI, "name=?", new String[]{"peak_refresh_rate"});
                } catch (Throwable ignored) {}
                try {
                    cr.delete(Settings.Global.CONTENT_URI, "name=?", new String[]{"min_refresh_rate"});
                    cr.delete(Settings.Global.CONTENT_URI, "name=?", new String[]{"peak_refresh_rate"});
                } catch (Throwable ignored) {}
            } else {
                android.os.Bundle b = new android.os.Bundle();
                String valStr = String.format(java.util.Locale.US, "%.1f", rate);
                b.putString("value", valStr);

                cr.call(systemUri, "PUT_system", "min_refresh_rate", b);
                cr.call(systemUri, "PUT_system", "peak_refresh_rate", b);
                try {
                    cr.call(globalUri, "PUT_global", "min_refresh_rate", b);
                    cr.call(globalUri, "PUT_global", "peak_refresh_rate", b);
                } catch (Throwable ignored) {}
            }
            success = true;
            android.util.Log.i("ResolutionMaster", "ContentProvider setRefreshRate succeeded: " + rate);
        } catch (Throwable t) {
            android.util.Log.w("ResolutionMaster", "ContentProvider setRefreshRate failed: " + t.getMessage());
        }

        // 2. Direct Settings.Global fallback (safe try-catch)
        try {
            ContentResolver cr = context.getContentResolver();
            if (rate <= 0.0f) {
                Settings.Global.putFloat(cr, "min_refresh_rate", 0.0f);
                Settings.Global.putFloat(cr, "peak_refresh_rate", 0.0f);
            } else {
                Settings.Global.putFloat(cr, "min_refresh_rate", rate);
                Settings.Global.putFloat(cr, "peak_refresh_rate", rate);
            }
            success = true;
        } catch (Throwable ignored) {}

        // 3. Shell fallback (Root / Shizuku if available or if needed)
        try {
            final String shellCmd;
            if (rate <= 0.0f) {
                shellCmd = "settings delete system min_refresh_rate; settings delete system peak_refresh_rate; settings delete global min_refresh_rate; settings delete global peak_refresh_rate";
            } else {
                shellCmd = String.format(java.util.Locale.US,
                    "settings put system min_refresh_rate %.1f; settings put system peak_refresh_rate %.1f; settings put global min_refresh_rate %.1f; settings put global peak_refresh_rate %.1f",
                    rate, rate, rate, rate);
            }
            if (ShellHelper.runCommand(context, shellCmd)) {
                success = true;
            }
        } catch (Throwable t) {
            android.util.Log.w("ResolutionMaster", "Shell fallback failed: " + t.getMessage());
        }

        return success;
    }

    public static float getConfiguredRefreshRate(Context context) {
        // 1. Direct ContentProvider GET_system call
        try {
            ContentResolver cr = context.getContentResolver();
            Uri systemUri = Uri.parse("content://settings/system");
            android.os.Bundle b = cr.call(systemUri, "GET_system", "peak_refresh_rate", null);
            if (b != null && b.containsKey("value")) {
                String val = b.getString("value");
                if (val != null && !val.trim().isEmpty() && !"null".equalsIgnoreCase(val)) {
                    float f = Float.parseFloat(val);
                    if (f > 0.0f) return f;
                }
            }
        } catch (Throwable ignored) {}

        // 2. Settings.System / Settings.Global standard getters
        try {
            ContentResolver cr = context.getContentResolver();
            float peak = Settings.System.getFloat(cr, "peak_refresh_rate", -1.0f);
            if (peak > 0.0f) return peak;
            peak = Settings.Global.getFloat(cr, "peak_refresh_rate", -1.0f);
            if (peak > 0.0f) return peak;
        } catch (Throwable ignored) {}

        return HZ_AUTO;
    }

    public static Point getCurrentResolution(Context context) {
        // 1. Instantaneous forced display size setting check
        try {
            ContentResolver cr = context.getContentResolver();
            String forced = Settings.Global.getString(cr, "display_size_forced");
            if (forced != null && forced.contains(",")) {
                String[] parts = forced.split(",");
                if (parts.length == 2) {
                    int w = Integer.parseInt(parts[0].trim());
                    int h = Integer.parseInt(parts[1].trim());
                    if (w > 0 && h > 0) {
                        return new Point(Math.min(w, h), Math.max(w, h));
                    }
                }
            } else if (forced != null && forced.contains("x")) {
                String[] parts = forced.split("x");
                if (parts.length == 2) {
                    int w = Integer.parseInt(parts[0].trim());
                    int h = Integer.parseInt(parts[1].trim());
                    if (w > 0 && h > 0) {
                        return new Point(Math.min(w, h), Math.max(w, h));
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 2. WindowMetrics (Android 11+)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                android.graphics.Rect bounds = wm.getMaximumWindowMetrics().getBounds();
                int w = bounds.width();
                int h = bounds.height();
                if (w > 0 && h > 0) {
                    return new Point(Math.min(w, h), Math.max(w, h));
                }
            }
        } catch (Throwable ignored) {}

        // 3. Display fallback
        Point p = new Point();
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            display.getRealSize(p);
            return new Point(Math.min(p.x, p.y), Math.max(p.x, p.y));
        } catch (Throwable ignored) {}

        return new Point(NATIVE_WIDTH, NATIVE_HEIGHT);
    }

    public static int getCurrentDensity(Context context) {
        // 1. Instantaneous forced density setting check
        try {
            ContentResolver cr = context.getContentResolver();
            String forced = Settings.Secure.getString(cr, "display_density_forced");
            if (forced == null || forced.trim().isEmpty() || "null".equalsIgnoreCase(forced)) {
                forced = Settings.Global.getString(cr, "display_density_forced");
            }
            if (forced != null && !forced.trim().isEmpty() && !"null".equalsIgnoreCase(forced)) {
                int d = Integer.parseInt(forced.trim());
                if (d > 0) return d;
            }
        } catch (Throwable ignored) {}

        // 2. Resources Configuration check
        try {
            int dpi = context.getResources().getConfiguration().densityDpi;
            if (dpi > 0) return dpi;
        } catch (Throwable ignored) {}

        return NATIVE_DENSITY;
    }

    public static float getCurrentRefreshRate(Context context) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            return wm.getDefaultDisplay().getRefreshRate();
        } catch (Throwable t) {
            return 120.0f;
        }
    }

    public static boolean isFHD(Context context) {
        Point p = getCurrentResolution(context);
        return Math.min(p.x, p.y) <= 1080 && Math.min(p.x, p.y) > 720;
    }

    public static boolean isNative(Context context) {
        Point p = getCurrentResolution(context);
        return Math.min(p.x, p.y) >= 1440;
    }
}
