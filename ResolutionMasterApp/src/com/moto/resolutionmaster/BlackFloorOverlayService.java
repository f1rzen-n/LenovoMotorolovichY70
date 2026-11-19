package com.moto.resolutionmaster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public class BlackFloorOverlayService extends Service {

    private static final String TAG = "BlackFloorOverlay";
    private static final String CHANNEL_ID = "oled_black_floor_channel";
    private static final int NOTIF_ID = 7026;

    public static final String ACTION_START = "com.moto.resolutionmaster.ACTION_BLACK_FLOOR_START";
    public static final String ACTION_STOP = "com.moto.resolutionmaster.ACTION_BLACK_FLOOR_STOP";
    public static final String EXTRA_LEVEL = "extra_black_floor_level";

    private static volatile boolean sRunning = false;
    private static volatile int sCurrentLevel = 0;

    private WindowManager mWindowManager;
    private View mOverlayView;

    public static boolean isRunning() {
        return sRunning;
    }

    public static int getCurrentLevel() {
        return sCurrentLevel;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            removeOverlay();
            stopForegroundSafe();
            sRunning = false;
            sCurrentLevel = 0;
            stopSelf();
            return START_NOT_STICKY;
        }

        int level = EyeCareHelper.getBlackFloorLevel(this);
        if (intent != null && intent.hasExtra(EXTRA_LEVEL)) {
            level = intent.getIntExtra(EXTRA_LEVEL, level);
        }

        if (level <= 0) {
            removeOverlay();
            stopForegroundSafe();
            sRunning = false;
            sCurrentLevel = 0;
            stopSelf();
            return START_NOT_STICKY;
        }

        if (level > 12) level = 12;

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted");
            sRunning = false;
            sCurrentLevel = 0;
            stopSelf();
            return START_NOT_STICKY;
        }

        startForegroundSafe(level);
        applyOverlay(level);
        sRunning = true;
        sCurrentLevel = level;
        return START_STICKY;
    }

    private void applyOverlay(int alphaLevel) {
        try {
            if (mWindowManager == null) {
                mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            }
            if (mWindowManager == null) return;

            int color = Color.argb(alphaLevel, 255, 255, 255);

            if (mOverlayView != null) {
                mOverlayView.setBackgroundColor(color);
                return;
            }

            View view = new View(this);
            view.setBackgroundColor(color);
            view.setClickable(false);
            view.setFocusable(false);

            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

            int wmFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                wmFlags,
                PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 0;

            if (Build.VERSION.SDK_INT >= 28) {
                try {
                    // 3 = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS (API 30+), 1 = SHORT_EDGES (API 28+)
                    params.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30 ? 3 : 1;
                } catch (Throwable ignored) {}
            }

            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    params.setFitInsetsTypes(0);
                } catch (Throwable ignored) {}
            }

            mWindowManager.addView(view, params);
            mOverlayView = view;
        } catch (Throwable t) {
            Log.e(TAG, "applyOverlay error", t);
        }
    }

    private void removeOverlay() {
        try {
            if (mOverlayView != null && mWindowManager != null) {
                mWindowManager.removeViewImmediate(mOverlayView);
            }
        } catch (Throwable t) {
            Log.e(TAG, "removeOverlay error", t);
        } finally {
            mOverlayView = null;
        }
    }

    private void startForegroundSafe(int level) {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Анти-шлейф OLED (Black Floor)",
                    NotificationManager.IMPORTANCE_MIN
                );
                channel.setDescription("Удерживает микро-уровень чёрного для устранения шлейфов OLED");
                channel.setShowBadge(false);
                nm.createNotificationChannel(channel);
            }

            Intent openIntent = new Intent(this, MainMenuActivity.class);
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int piFlags = Build.VERSION.SDK_INT >= 23
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getActivity(this, 0, openIntent, piFlags);

            String hex = String.format("#%02X%02X%02X", level, level, level);
            Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

            Notification notif = builder
                .setSmallIcon(R.drawable.ic_tile)
                .setContentTitle("Анти-шлейф OLED активен (" + hex + ")")
                .setContentText("Подъём уровня чёрного • Нажмите для настройки")
                .setOngoing(true)
                .setContentIntent(pi)
                .build();

            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID, notif);
            }
        } catch (Throwable t) {
            Log.w(TAG, "startForegroundSafe fallback: " + t.getMessage());
        }
    }

    private void stopForegroundSafe() {
        try {
            stopForeground(true);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        sRunning = false;
        sCurrentLevel = 0;
        super.onDestroy();
    }
}
