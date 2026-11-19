package com.moto.resolutionmaster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

public class AodTimeoutService extends Service {

    private static final String TAG = "AodTimeoutService";
    private static final String CHANNEL_ID = "aod_timeout_channel";
    private static final int NOTIF_ID = 2002;

    private static boolean sRunning = false;

    private Handler mHandler;
    private PowerManager mPowerManager;
    private boolean mIsReceiverRegistered = false;

    public static boolean isRunning() {
        return sRunning;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, AodTimeoutService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Throwable t) {
            try { context.startService(intent); } catch (Throwable ignored) {}
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, AodTimeoutService.class);
        try { context.stopService(intent); } catch (Throwable ignored) {}
    }

    private final Runnable mFadeRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPowerManager != null && !mPowerManager.isInteractive()) {
                Log.i(TAG, "Screen still inactive. Fading out AOD now.");
                AodTimeoutHelper.setAodOff(AodTimeoutService.this);
            }
        }
    };

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String action = intent.getAction();

            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                // Check if charging
                boolean keepOnCharging = AodTimeoutHelper.isKeepOnCharging(context);
                if (keepOnCharging && isDeviceCharging(context)) {
                    Log.d(TAG, "Device is charging; skipping AOD fade.");
                    return;
                }

                int timeoutSec = AodTimeoutHelper.getTimeoutSeconds(context);
                if (timeoutSec <= 0) timeoutSec = AodTimeoutHelper.DEFAULT_TIMEOUT_SEC;

                mHandler.removeCallbacks(mFadeRunnable);
                mHandler.postDelayed(mFadeRunnable, timeoutSec * 1000L);
                Log.d(TAG, "Screen off. Scheduled AOD fade in " + timeoutSec + "s");

            } else if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
                mHandler.removeCallbacks(mFadeRunnable);
                AodTimeoutHelper.restoreAodOn(context);
                Log.d(TAG, "Screen woke up. Restored AOD for next lock.");
            }
        }
    };

    private boolean isDeviceCharging(Context context) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                       status == BatteryManager.BATTERY_STATUS_FULL;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sRunning = true;
        mHandler = new Handler(Looper.getMainLooper());
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        createNotificationChannel();
        startForegroundNotification();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenReceiver, filter);
        mIsReceiverRegistered = true;

        // Ensure AOD is initially on
        AodTimeoutHelper.restoreAodOn(this);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Умное затухание AOD",
                NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("Фоновый таймер экономии заряда Always On Display");
            channel.enableVibration(false);
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void startForegroundNotification() {
        try {
            Intent appIntent = new Intent(this, MainMenuActivity.class);
            PendingIntent pi = PendingIntent.getActivity(
                this, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this);
            }

            builder.setSmallIcon(R.drawable.ic_cat_aod)
                   .setContentTitle("Умное затухание AOD")
                   .setContentText("Таймер: " + AodTimeoutHelper.getTimeoutSeconds(this) + " сек")
                   .setContentIntent(pi)
                   .setOngoing(true);

            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID, builder.build());
            }
        } catch (Throwable t) {
            Log.e(TAG, "startForeground error", t);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        sRunning = false;
        if (mHandler != null) {
            mHandler.removeCallbacks(mFadeRunnable);
        }
        if (mIsReceiverRegistered) {
            try {
                unregisterReceiver(mScreenReceiver);
            } catch (Throwable ignored) {}
            mIsReceiverRegistered = false;
        }
        // Restore AOD so screen is not left dark
        AodTimeoutHelper.restoreAodOn(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
