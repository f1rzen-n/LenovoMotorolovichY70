package com.moto.resolutionmaster;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.Log;

public class AudioHelper {

    private static final String TAG = "AudioHelper";
    public static final String SETTING_DISABLE_ABSOLUTE_VOLUME = "bluetooth_disable_absolute_volume";
    public static final String SETTING_SAFE_VOLUME_STATE = "audio_safe_volume_state";

    public static boolean isBluetoothAbsoluteVolumeDisabled(Context context) {
        try {
            int val = Settings.Global.getInt(context.getContentResolver(), SETTING_DISABLE_ABSOLUTE_VOLUME, 0);
            return val == 1;
        } catch (Throwable t) {
            String out = ShellHelper.runCommandWithOutput(context, "settings get global " + SETTING_DISABLE_ABSOLUTE_VOLUME);
            return "1".equals(out != null ? out.trim() : "");
        }
    }

    public static boolean toggleBluetoothAbsoluteVolume(Context context) {
        boolean currentlyDisabled = isBluetoothAbsoluteVolumeDisabled(context);
        boolean newDisabled = !currentlyDisabled;
        int targetVal = newDisabled ? 1 : 0;

        boolean ok = ShellHelper.runCommand(context, "settings put global " + SETTING_DISABLE_ABSOLUTE_VOLUME + " " + targetVal);
        if (!ok) {
            try {
                ok = Settings.Global.putInt(context.getContentResolver(), SETTING_DISABLE_ABSOLUTE_VOLUME, targetVal);
            } catch (Throwable ignored) {}
        }
        return ok;
    }

    public static boolean isSafeVolumeBypassed(Context context) {
        try {
            int val = Settings.Global.getInt(context.getContentResolver(), SETTING_SAFE_VOLUME_STATE, 0);
            return val == 3;
        } catch (Throwable t) {
            String out = ShellHelper.runCommandWithOutput(context, "settings get global " + SETTING_SAFE_VOLUME_STATE);
            return "3".equals(out != null ? out.trim() : "");
        }
    }

    public static boolean toggleSafeVolumeBypass(Context context) {
        boolean currentlyBypassed = isSafeVolumeBypassed(context);
        boolean newBypass = !currentlyBypassed;
        int targetVal = newBypass ? 3 : 1; // 3 = disabled / bypassed, 1 = default / enforced

        String cmd = "settings put global " + SETTING_SAFE_VOLUME_STATE + " " + targetVal +
                     "; setprop audio.safemedia.bypass " + (newBypass ? "true" : "false") +
                     "; setprop ro.audio.safemedia.bypass " + (newBypass ? "true" : "false");
        boolean ok = ShellHelper.runCommand(context, cmd);
        if (!ok) {
            try {
                ok = Settings.Global.putInt(context.getContentResolver(), SETTING_SAFE_VOLUME_STATE, targetVal);
            } catch (Throwable ignored) {}
        }
        return ok;
    }

    public static int getMediaVolumePercent(Context context) {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int cur = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (max > 0) {
                    return Math.round((cur * 100f) / max);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "getMediaVolumePercent error", t);
        }
        return 50;
    }

    public static void setMediaVolumePercent(Context context, int percent) {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int targetIndex = Math.round((percent * (float) max) / 100f);
                if (targetIndex < 0) targetIndex = 0;
                if (targetIndex > max) targetIndex = max;
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0);
            }
        } catch (Throwable t) {
            int targetIndex = Math.round((percent * 15f) / 100f);
            ShellHelper.runCommand(context, "media volume --stream 3 --set " + targetIndex);
        }
    }

    public static boolean launchMultiVolume(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.motorola.dynamicvolume", "com.motorola.dynamicvolume.ui.SettingsRouterActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable t) {
            try {
                Intent fallback = new Intent(Settings.ACTION_SOUND_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
                return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static boolean launchDolbyAtmos(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.motorola.dolby.dolbyui", "com.motorola.dolby.dolbyui.ui.LauncherActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable t) {
            try {
                Intent fallback = new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL");
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
                return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }
}
