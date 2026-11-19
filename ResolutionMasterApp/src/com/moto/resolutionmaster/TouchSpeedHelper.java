package com.moto.resolutionmaster;

import android.content.Context;
import android.provider.Settings;

public class TouchSpeedHelper {

    public static final int MIN_SPEED = -7;
    public static final int MAX_SPEED = 7;
    public static final int DEFAULT_SPEED = 0;

    private static int sLastAppliedSpeed = -999;

    public static void invalidateCache() {
        sLastAppliedSpeed = -999;
    }

    /**
     * Reads current pointer speed from system settings.
     * Android standard range: -7 to +7, default is 0.
     */
    public static int getPointerSpeed(Context context) {
        if (sLastAppliedSpeed != -999) {
            return sLastAppliedSpeed;
        }
        try {
            return Settings.System.getInt(context.getContentResolver(), "pointer_speed", DEFAULT_SPEED);
        } catch (Throwable e) {
            return DEFAULT_SPEED;
        }
    }

    /**
     * Sets pointer speed in range [-7, 7].
     * Executes via Settings.System and ShellHelper (Shizuku/Root/ADB).
     */
    public static boolean setPointerSpeed(Context context, int speed) {
        if (speed < MIN_SPEED) speed = MIN_SPEED;
        if (speed > MAX_SPEED) speed = MAX_SPEED;
        sLastAppliedSpeed = speed;

        try {
            Settings.System.putInt(context.getContentResolver(), "pointer_speed", speed);
        } catch (Throwable ignored) {}

        return ShellHelper.runCommand(context, "settings put system pointer_speed " + speed);
    }

    /**
     * Converts raw speed (-7..+7) to intuitive user-friendly percentage and label.
     */
    public static String getSpeedLabel(int speed) {
        if (speed == 7) {
            return "100% — Турбо (0 мс задержки ⚡)";
        } else if (speed >= 4) {
            return "85% — Игровой (Быстрый отклик 🎮)";
        } else if (speed > 0) {
            int pct = 50 + Math.round((speed / 7.0f) * 50);
            return pct + "% — Повышенный отклик";
        } else if (speed == 0) {
            return "50% — Сток (Сглаживание дрожания ⚖️)";
        } else {
            int pct = Math.max(0, 50 + Math.round((speed / 7.0f) * 50));
            return pct + "% — Сглаженный ввод";
        }
    }

    /**
     * Converts SeekBar progress (0..14) to speed (-7..+7).
     */
    public static int progressToSpeed(int progress) {
        return progress - 7;
    }

    /**
     * Converts speed (-7..+7) to SeekBar progress (0..14).
     */
    public static int speedToProgress(int speed) {
        return speed + 7;
    }

    /**
     * Reads current animation scale (1.0 = standard, 0.5 = 2x faster).
     */
    public static float getAnimationScale(Context context) {
        try {
            return Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.WINDOW_ANIMATION_SCALE, 1.0f);
        } catch (Throwable t) {
            return 1.0f;
        }
    }

    /**
     * Sets animation scales to 0.5x, 1.0x, etc.
     */
    public static boolean setAnimationScales(Context context, float scale) {
        String scaleStr = String.valueOf(scale);
        String cmd = "settings put global window_animation_scale " + scaleStr + " ; " +
                     "settings put global transition_animation_scale " + scaleStr + " ; " +
                     "settings put global animator_duration_scale " + scaleStr;
        return ShellHelper.runCommand(context, cmd);
    }

    /**
     * Human-readable label for animation scale.
     */
    public static String getAnimationScaleLabel(float scale) {
        if (Math.abs(scale - 0.5f) < 0.05f) {
            return "0.5x (Ускорено в 2 раза 🚀)";
        } else if (Math.abs(scale - 1.0f) < 0.05f) {
            return "1.0x (Стандартное время ⚖️)";
        } else if (scale < 0.1f) {
            return "Без анимаций (0.0x ⚡)";
        } else {
            return scale + "x";
        }
    }

    /**
     * Activates full Turbo Game Mode in 1 click:
     * 1. 144 Hz display refresh rate (enables peak 1500 Hz Goodix touch)
     * 2. 100% max touch sensitivity (+7)
     * 3. 0.5x animation scales (cuts UI latency in half)
     */
    public static boolean enableFullTurboMode(Context context) {
        boolean okFps = DisplayManagerHelper.setRefreshRate(context, DisplayManagerHelper.HZ_144);
        boolean okTouch = setPointerSpeed(context, 7);
        boolean okAnim = setAnimationScales(context, 0.5f);
        return okFps || okTouch || okAnim;
    }

    /**
     * Resets system to stock defaults:
     * 1. Auto Refresh Rate (Dynamic 60-144 Hz)
     * 2. Standard Touch Sensitivity (50% / 0)
     * 3. 1.0x Standard Animations
     */
    public static boolean resetToStock(Context context) {
        boolean okFps = DisplayManagerHelper.setRefreshRate(context, DisplayManagerHelper.HZ_AUTO);
        boolean okTouch = setPointerSpeed(context, 0);
        boolean okAnim = setAnimationScales(context, 1.0f);
        return okFps || okTouch || okAnim;
    }
}
