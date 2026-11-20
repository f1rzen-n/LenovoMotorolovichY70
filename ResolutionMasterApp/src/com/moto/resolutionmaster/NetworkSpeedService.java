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
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class NetworkSpeedService extends Service {

    private static final String TAG = "NetworkSpeedService";
    private static final String CHANNEL_VISIBLE = "net_speed_statusbar_v2";
    private static final String CHANNEL_HIDDEN = "net_speed_hidden_v2";
    private static final int NOTIF_ID_MAIN = 7028;
    private static final int NOTIF_ID_UP = 7029;

    public static final String ACTION_START = "com.moto.resolutionmaster.ACTION_NET_SPEED_START";
    public static final String ACTION_STOP = "com.moto.resolutionmaster.ACTION_NET_SPEED_STOP";
    public static final String ACTION_REFRESH = "com.moto.resolutionmaster.ACTION_NET_SPEED_REFRESH";

    private static volatile boolean sRunning = false;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private NotificationManager mNotificationManager;
    private WindowManager mWindowManager;

    // Right-side fixed status bar overlay views
    private LinearLayout mOverlayRoot;
    private LinearLayout mOverlayPill;
    private TextView mTvOverlayDown;
    private TextView mTvOverlayUp;
    private WindowManager.LayoutParams mOverlayParams;

    // Left-side notification icon paints
    private Paint mValPaint;
    private Paint mUnitPaint;
    private Paint mArrowPaint;
    private Paint mSplitValPaint;

    private long mLastRxBytes = -1;
    private long mLastTxBytes = -1;
    private long mLastMobileTotal = -1;
    private long mLastTimestampMs = 0;

    private long mSessionWifiBytes = 0;
    private long mSessionMobileBytes = 0;

    private long mLastRxPerSec = 0;
    private long mLastTxPerSec = 0;

    private int mIdleStreak = 0;
    private boolean mScreenOn = true;
    private boolean mReceiverRegistered = false;

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mScreenOn = false;
                mHandler.removeCallbacks(mTickRunnable);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mScreenOn = true;
                resetBaseline();
                mHandler.removeCallbacks(mTickRunnable);
                mHandler.post(mTickRunnable);
            }
        }
    };

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            handleScreenMetricsChanged();
        }
    };

    private void handleScreenMetricsChanged() {
        if (!sRunning) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                recreateOverlayWithNewMetrics();
            }
        });
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                recreateOverlayWithNewMetrics();
            }
        }, 350L);
    }

    private void recreateOverlayWithNewMetrics() {
        if (!sRunning) return;
        int mode = NetworkSpeedHelper.getIconMode(this);
        if (mode == NetworkSpeedHelper.ICON_MODE_RIGHT_OVERLAY) {
            removeOverlay();
            ensureOverlayAttached();
            updateOverlayContent(mLastRxPerSec, mLastTxPerSec, NetworkSpeedHelper.isBitsMode(this), false);
        }
    }

    private int mLastKnownDpi = 0;
    private int mLastKnownWidth = 0;

    private final Runnable mTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!sRunning || !mScreenOn) return;
            try {
                int curDpi = DisplayManagerHelper.getCurrentDensity(NetworkSpeedService.this);
                Point pt = DisplayManagerHelper.getCurrentResolution(NetworkSpeedService.this);
                int curWidth = (pt != null) ? Math.min(pt.x, pt.y) : 0;

                if ((mLastKnownDpi > 0 && Math.abs(curDpi - mLastKnownDpi) > 5) ||
                    (mLastKnownWidth > 0 && Math.abs(curWidth - mLastKnownWidth) > 10)) {
                    mLastKnownDpi = curDpi;
                    mLastKnownWidth = curWidth;
                    int mode = NetworkSpeedHelper.getIconMode(NetworkSpeedService.this);
                    if (mode == NetworkSpeedHelper.ICON_MODE_RIGHT_OVERLAY) {
                        removeOverlay();
                        ensureOverlayAttached();
                    }
                } else if (mOverlayRoot != null) {
                    int targetOffsetPx = getClampedOverlayOffsetPx();
                    int pillH = (mOverlayPill != null && mOverlayPill.getHeight() > 0)
                        ? mOverlayPill.getHeight()
                        : dpToPx(18);
                    int targetY = getTargetOverlayY(pillH);
                    if (mOverlayParams != null && (Math.abs(mOverlayParams.x - targetOffsetPx) > 3 || Math.abs(mOverlayParams.y - targetY) > 2)) {
                        updateOverlayPosition();
                    }
                }
            } catch (Throwable ignored) {}
            updateTrafficAndDisplay();
            mHandler.postDelayed(this, 1000L);
        }
    };

    public static boolean isRunning() {
        return sRunning;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        createChannels();
        initPaints();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            mScreenOn = pm.isInteractive();
        }

        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            registerReceiver(mScreenReceiver, filter);
            mReceiverRegistered = true;
        } catch (Throwable ignored) {}

        try {
            getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor("display_density_forced"), false, mSettingsObserver);
            getContentResolver().registerContentObserver(
                Settings.Global.getUriFor("display_size_forced"), false, mSettingsObserver);
        } catch (Throwable ignored) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_START;

        if (ACTION_STOP.equals(action)) {
            stopMonitoring();
            return START_NOT_STICKY;
        }

        if (ACTION_REFRESH.equals(action)) {
            if (sRunning) {
                mIdleStreak = 0;
                int mode = NetworkSpeedHelper.getIconMode(this);
                if (mode != NetworkSpeedHelper.ICON_MODE_TWO && mNotificationManager != null) {
                    mNotificationManager.cancel(NOTIF_ID_UP);
                }
                if (mode == NetworkSpeedHelper.ICON_MODE_RIGHT_OVERLAY) {
                    removeOverlay();
                    ensureOverlayAttached();
                } else {
                    removeOverlay();
                }
                applyDisplay(mLastRxPerSec, mLastTxPerSec, false);
            }
            return START_STICKY;
        }

        if (!sRunning) {
            sRunning = true;
            mSessionWifiBytes = 0;
            mSessionMobileBytes = 0;
            mIdleStreak = 0;
            resetBaseline();
            startAsForeground(0L, 0L, false);
        } else {
            int mode = NetworkSpeedHelper.getIconMode(this);
            if (mode == NetworkSpeedHelper.ICON_MODE_RIGHT_OVERLAY) {
                removeOverlay();
                ensureOverlayAttached();
            } else {
                removeOverlay();
            }
        }

        mHandler.removeCallbacks(mTickRunnable);
        if (mScreenOn) {
            mHandler.postDelayed(mTickRunnable, 200L);
        }
        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!sRunning) return;

        // If orientation is landscape, hide the overlay so it doesn't obstruct fullscreen apps/games
        if (newConfig != null && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (mOverlayRoot != null) {
                mOverlayRoot.setVisibility(View.GONE);
            }
            return;
        }

        // Density or resolution or portrait orientation changed: cleanly recreate overlay
        int mode = NetworkSpeedHelper.getIconMode(this);
        if (mode == NetworkSpeedHelper.ICON_MODE_RIGHT_OVERLAY) {
            removeOverlay();
            ensureOverlayAttached();
            updateOverlayContent(mLastRxPerSec, mLastTxPerSec, NetworkSpeedHelper.isBitsMode(this), false);
        } else {
            removeOverlay();
        }
        applyDisplay(mLastRxPerSec, mLastTxPerSec, false);
    }

    private void stopMonitoring() {
        sRunning = false;
        mHandler.removeCallbacks(mTickRunnable);
        removeOverlay();
        try {
            stopForeground(true);
        } catch (Throwable ignored) {}
        try {
            if (mNotificationManager != null) {
                mNotificationManager.cancel(NOTIF_ID_MAIN);
                mNotificationManager.cancel(NOTIF_ID_UP);
            }
        } catch (Throwable ignored) {}
        stopSelf();
    }

    private void resetBaseline() {
        long rx = TrafficStats.getTotalRxBytes();
        long tx = TrafficStats.getTotalTxBytes();
        long mRx = TrafficStats.getMobileRxBytes();
        long mTx = TrafficStats.getMobileTxBytes();
        mLastRxBytes = rx >= 0 ? rx : 0;
        mLastTxBytes = tx >= 0 ? tx : 0;
        mLastMobileTotal = (mRx >= 0 && mTx >= 0) ? (mRx + mTx) : 0;
        mLastTimestampMs = System.currentTimeMillis();
    }

    private void updateTrafficAndDisplay() {
        long now = System.currentTimeMillis();
        long rx = TrafficStats.getTotalRxBytes();
        long tx = TrafficStats.getTotalTxBytes();
        long mRx = TrafficStats.getMobileRxBytes();
        long mTx = TrafficStats.getMobileTxBytes();

        if (rx < 0) rx = 0;
        if (tx < 0) tx = 0;
        long mobileNow = (mRx >= 0 && mTx >= 0) ? (mRx + mTx) : 0;

        long dtMs = now - mLastTimestampMs;
        if (mLastRxBytes < 0 || dtMs <= 0) {
            mLastRxBytes = rx;
            mLastTxBytes = tx;
            mLastMobileTotal = mobileNow;
            mLastTimestampMs = now;
            return;
        }

        long deltaRx = Math.max(0L, rx - mLastRxBytes);
        long deltaTx = Math.max(0L, tx - mLastTxBytes);
        long deltaTotal = deltaRx + deltaTx;
        long deltaMobile = Math.max(0L, mobileNow - mLastMobileTotal);
        if (deltaMobile > deltaTotal) deltaMobile = deltaTotal;
        long deltaWifi = Math.max(0L, deltaTotal - deltaMobile);

        mSessionMobileBytes += deltaMobile;
        mSessionWifiBytes += deltaWifi;

        mLastRxBytes = rx;
        mLastTxBytes = tx;
        mLastMobileTotal = mobileNow;
        mLastTimestampMs = now;

        long rxPerSec = (deltaRx * 1000L) / dtMs;
        long txPerSec = (deltaTx * 1000L) / dtMs;
        mLastRxPerSec = rxPerSec;
        mLastTxPerSec = txPerSec;

        long totalPerSec = rxPerSec + txPerSec;

        boolean hideOnIdle = NetworkSpeedHelper.isHideOnIdleEnabled(this);
        if (totalPerSec < 1024L) {
            mIdleStreak++;
        } else {
            mIdleStreak = 0;
        }

        boolean shouldHide = hideOnIdle && mIdleStreak >= 2;
        applyDisplay(rxPerSec, txPerSec, shouldHide);
    }

    private void startAsForeground(long rxPerSec, long txPerSec, boolean hidden) {
        try {
            int mode = NetworkSpeedHelper.getIconMode(this);
            boolean bitsMode = NetworkSpeedHelper.isBitsMode(this);
            Notification mainNotif = buildMainNotification(rxPerSec, txPerSec, bitsMode, mode, hidden);
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID_MAIN, mainNotif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID_MAIN, mainNotif);
            }

            if (mode == NetworkSpeedHelper.ICON_MODE_RIGHT_OVERLAY) {
                ensureOverlayAttached();
                updateOverlayContent(rxPerSec, txPerSec, bitsMode, hidden);
            } else if (mode == NetworkSpeedHelper.ICON_MODE_TWO && mNotificationManager != null) {
                if (hidden) {
                    mNotificationManager.cancel(NOTIF_ID_UP);
                } else {
                    mNotificationManager.notify(NOTIF_ID_UP, buildUploadNotification(txPerSec, bitsMode));
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "startForeground error", t);
        }
    }

    private void applyDisplay(long rxPerSec, long txPerSec, boolean hideIcon) {
        int mode = NetworkSpeedHelper.getIconMode(this);
        boolean bitsMode = NetworkSpeedHelper.isBitsMode(this);

        if (mode == NetworkSpeedHelper.ICON_MODE_RIGHT_OVERLAY) {
            ensureOverlayAttached();
            updateOverlayContent(rxPerSec, txPerSec, bitsMode, hideIcon);
        } else {
            removeOverlay();
        }

        if (mNotificationManager == null) return;
        try {
            Notification mainNotif = buildMainNotification(rxPerSec, txPerSec, bitsMode, mode, hideIcon);
            mNotificationManager.notify(NOTIF_ID_MAIN, mainNotif);

            if (mode == NetworkSpeedHelper.ICON_MODE_TWO) {
                if (hideIcon) {
                    mNotificationManager.cancel(NOTIF_ID_UP);
                } else {
                    Notification upNotif = buildUploadNotification(txPerSec, bitsMode);
                    mNotificationManager.notify(NOTIF_ID_UP, upNotif);
                }
            } else {
                mNotificationManager.cancel(NOTIF_ID_UP);
            }
        } catch (Throwable t) {
            Log.e(TAG, "applyDisplay error", t);
        }
    }

    // ==================== Fixed Right-Side Status Bar Overlay ====================

    private float getLiveDensity() {
        int dpi = DisplayManagerHelper.getCurrentDensity(this);
        if (dpi > 0) {
            return dpi / 160.0f;
        }
        return getResources().getDisplayMetrics().density;
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getLiveDensity());
    }

    private int getScreenWidthDp() {
        int wPx = 0;
        try {
            Point pt = DisplayManagerHelper.getCurrentResolution(this);
            if (pt != null && pt.x > 0) {
                wPx = Math.min(pt.x, pt.y);
            }
        } catch (Throwable ignored) {}
        if (wPx <= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mWindowManager != null) {
            try {
                wPx = mWindowManager.getCurrentWindowMetrics().getBounds().width();
            } catch (Throwable ignored) {}
        }
        if (wPx <= 0) {
            wPx = getResources().getDisplayMetrics().widthPixels;
        }
        float density = getLiveDensity();
        return Math.round(wPx / (density > 0 ? density : 1.0f));
    }

    private int getStatusBarHeightPx() {
        float density = getLiveDensity();
        int detected = 0;

        // 1. Live WindowMetrics insets (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mWindowManager != null) {
            try {
                android.view.WindowMetrics wm = mWindowManager.getCurrentWindowMetrics();
                if (wm != null && wm.getWindowInsets() != null) {
                    android.graphics.Insets insets = wm.getWindowInsets().getInsetsIgnoringVisibility(
                        android.view.WindowInsets.Type.statusBars()
                    );
                    if (insets != null && insets.top > 0) {
                        detected = insets.top;
                    }
                    android.view.DisplayCutout cutout = wm.getWindowInsets().getDisplayCutout();
                    if (cutout != null && cutout.getSafeInsetTop() > detected) {
                        detected = cutout.getSafeInsetTop();
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. DisplayCutout from Display (Android 9+)
        if (detected <= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mWindowManager != null) {
            try {
                android.view.Display display = mWindowManager.getDefaultDisplay();
                if (display != null) {
                    android.view.DisplayCutout cutout = display.getCutout();
                    if (cutout != null && cutout.getSafeInsetTop() > 0) {
                        detected = cutout.getSafeInsetTop();
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 3. Root Window Insets if overlay root is already attached
        if (detected <= 0 && mOverlayRoot != null) {
            try {
                android.view.WindowInsets rwi = mOverlayRoot.getRootWindowInsets();
                if (rwi != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        android.graphics.Insets insets = rwi.getInsetsIgnoringVisibility(
                            android.view.WindowInsets.Type.statusBars()
                        );
                        if (insets != null && insets.top > 0) {
                            detected = insets.top;
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && rwi.getDisplayCutout() != null) {
                        int cutTop = rwi.getDisplayCutout().getSafeInsetTop();
                        if (cutTop > detected) detected = cutTop;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 4. Portrait status bar dimension resource
        if (detected <= 0) {
            try {
                int resId = getResources().getIdentifier("status_bar_height_portrait", "dimen", "android");
                if (resId <= 0) {
                    resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                }
                if (resId > 0) {
                    detected = getResources().getDimensionPixelSize(resId);
                }
            } catch (Throwable ignored) {}
        }

        // Validate sbHeight against live density:
        // On Lenovo Legion Y70 / Motorola Edge 70 Max (centered punch hole),
        // the status bar height is ~46dp (44dp - 48dp). At native 520 DPI (3.25 density) it is 150px (46.15dp).
        // Any detected value < 40dp (like the legacy 24dp AOSP dimen) or > 56dp (like unscaled native 150px at density 2.0 = 75dp)
        // is invalid.
        float sbDp = density > 0 ? (detected / density) : 0;
        if (detected <= 0 || sbDp < 40.0f || sbDp > 56.0f) {
            return Math.round(46.0f * density);
        }
        return detected;
    }

    private int getScreenHeightPx() {
        int hPx = 0;
        try {
            Point pt = DisplayManagerHelper.getCurrentResolution(this);
            if (pt != null && pt.y > 0) {
                hPx = Math.max(pt.x, pt.y);
            }
        } catch (Throwable ignored) {}
        if (hPx <= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mWindowManager != null) {
            try {
                hPx = mWindowManager.getMaximumWindowMetrics().getBounds().height();
            } catch (Throwable ignored) {}
        }
        if (hPx <= 0) {
            hPx = getResources().getDisplayMetrics().heightPixels;
        }
        if (hPx <= 0) {
            hPx = 2376;
        }
        return hPx;
    }

    private int getTargetOverlayY(int pillHeightPx) {
        int screenHeight = getScreenHeightPx();
        // Mathematical optical alignment:
        // The physical camera punch-hole on Lenovo Legion Y70 / Motorola Edge 70 Max is centered at
        // physical Y ratio 0.0251f of the screen height (79.5px on 3168p, 59.6px on 2376p, 39.8px on 1584p).
        // The system status bar icons (clock, battery, Wi-Fi, cellular) are permanently centered on this exact line.
        // Therefore, centering the overlay pill on this horizontal axis ensures pixel-perfect vertical alignment
        // across any resolution and any DPI value set by the user.
        int iconCenterY = Math.round(screenHeight * 0.0251f);
        int userYOffsetPx = dpToPx(NetworkSpeedHelper.getOverlayOffsetYDp(this));
        return Math.max(0, iconCenterY - (pillHeightPx / 2) + userYOffsetPx);
    }

    private int getClampedOverlayOffsetPx() {
        int userDp = NetworkSpeedHelper.getOverlayOffsetDp(this);
        int screenWidthDp = getScreenWidthDp();
        // Clamped so it never crosses the center punch-hole camera (center is at ~45-50% width)
        int maxSafeDp = Math.max(45, (int) (screenWidthDp * 0.45f));
        int clampedDp = Math.min(userDp, maxSafeDp);
        return dpToPx(clampedDp);
    }

    private void ensureOverlayAttached() {
        if (mOverlayRoot != null) {
            updateOverlayPosition();
            return;
        }
        if (mWindowManager == null || !Settings.canDrawOverlays(this)) return;

        try {
            float density = getLiveDensity();
            mLastKnownDpi = Math.round(density * 160.0f);
            Point curRes = DisplayManagerHelper.getCurrentResolution(this);
            mLastKnownWidth = (curRes != null) ? Math.min(curRes.x, curRes.y) : 0;

            Typeface condensedBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);

            Context viewCtx = this;
            try {
                Configuration cfg = new Configuration(getResources().getConfiguration());
                cfg.densityDpi = mLastKnownDpi;
                viewCtx = createConfigurationContext(cfg);
            } catch (Throwable ignored) {}

            LinearLayout root = new LinearLayout(viewCtx);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER);
            root.setClickable(false);
            root.setFocusable(false);

            LinearLayout pill = new LinearLayout(viewCtx);
            pill.setOrientation(LinearLayout.VERTICAL);
            pill.setGravity(Gravity.END);
            int padH = dpToPx(3);
            int padV = dpToPx(0.5f);
            pill.setPadding(padH, padV, padH, padV);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.TRANSPARENT);
            bg.setCornerRadius(dpToPx(4));
            pill.setBackground(bg);

            TextView tvDown = new TextView(viewCtx);
            tvDown.setTextColor(0xFFFFFFFF);
            tvDown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
            tvDown.setTypeface(condensedBold);
            tvDown.setIncludeFontPadding(false);
            tvDown.setSingleLine(true);
            tvDown.setMaxLines(1);
            tvDown.setShadowLayer(2.5f, 0f, 1f, 0xEE000000);
            tvDown.setGravity(Gravity.END);
            tvDown.setText("↓0K");

            TextView tvUp = new TextView(viewCtx);
            tvUp.setTextColor(0xFFD1D5DB);
            tvUp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8.5f);
            tvUp.setTypeface(condensedBold);
            tvUp.setIncludeFontPadding(false);
            tvUp.setSingleLine(true);
            tvUp.setMaxLines(1);
            tvUp.setShadowLayer(2.5f, 0f, 1f, 0xEE000000);
            tvUp.setGravity(Gravity.END);
            LinearLayout.LayoutParams upLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            upLp.topMargin = -dpToPx(0.5f);
            tvUp.setLayoutParams(upLp);
            tvUp.setText("↑0K");

            pill.addView(tvDown);
            pill.addView(tvUp);
            root.addView(pill);

            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

            int wmFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                wmFlags,
                PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.END;
            params.x = getClampedOverlayOffsetPx();

            pill.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int pillHeightPx = (pill.getMeasuredHeight() > 0) ? pill.getMeasuredHeight() : dpToPx(18);
            params.y = getTargetOverlayY(pillHeightPx);

            if (Build.VERSION.SDK_INT >= 28) {
                try {
                    params.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30 ? 3 : 1;
                } catch (Throwable ignored) {}
            }
            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    params.setFitInsetsTypes(0);
                } catch (Throwable ignored) {}
            }

            mWindowManager.addView(root, params);
            mOverlayRoot = root;
            mOverlayPill = pill;
            mTvOverlayDown = tvDown;
            mTvOverlayUp = tvUp;
            mOverlayParams = params;
        } catch (Throwable t) {
            Log.e(TAG, "ensureOverlayAttached error", t);
        }
    }

    private void updateOverlayPosition() {
        if (mOverlayRoot == null || mOverlayParams == null || mWindowManager == null) return;
        try {
            if (mOverlayPill != null && mOverlayPill.getHeight() <= 0) {
                mOverlayPill.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
            }
            int pillHeightPx = (mOverlayPill != null && mOverlayPill.getHeight() > 0)
                ? mOverlayPill.getHeight()
                : ((mOverlayPill != null && mOverlayPill.getMeasuredHeight() > 0)
                    ? mOverlayPill.getMeasuredHeight()
                    : dpToPx(18));
            mOverlayParams.x = getClampedOverlayOffsetPx();
            mOverlayParams.y = getTargetOverlayY(pillHeightPx);
            mOverlayParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            mOverlayParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            if (Build.VERSION.SDK_INT >= 30) {
                try {
                    mOverlayParams.setFitInsetsTypes(0);
                } catch (Throwable ignored) {}
            }
            mWindowManager.updateViewLayout(mOverlayRoot, mOverlayParams);
        } catch (Throwable ignored) {}
    }

    private void updateOverlayContent(long rxPerSec, long txPerSec, boolean bitsMode, boolean hide) {
        if (mOverlayRoot == null || mTvOverlayDown == null || mTvOverlayUp == null) return;
        if (hide) {
            if (mOverlayRoot.getVisibility() != View.GONE) {
                mOverlayRoot.setVisibility(View.GONE);
            }
            return;
        }
        if (mOverlayRoot.getVisibility() != View.VISIBLE) {
            mOverlayRoot.setVisibility(View.VISIBLE);
        }
        mTvOverlayDown.setText("↓" + formatOverlaySpeed(rxPerSec, bitsMode));
        mTvOverlayUp.setText("↑" + formatOverlaySpeed(txPerSec, bitsMode));
    }

    private String formatOverlaySpeed(long bytesPerSec, boolean bitsMode) {
        if (bitsMode) {
            double bits = bytesPerSec * 8.0;
            if (bits < 1_000_000.0) {
                return Math.round(bits / 1000.0) + "Kb";
            } else {
                double mb = bits / 1_000_000.0;
                return mb >= 10.0
                    ? String.format(Locale.US, "%dMb", Math.round(mb))
                    : String.format(Locale.US, "%.1fMb", mb);
            }
        } else {
            if (bytesPerSec < 1024L * 1024L) {
                return Math.round(bytesPerSec / 1024.0) + "K/s";
            } else {
                double mb = bytesPerSec / (1024.0 * 1024.0);
                return mb >= 10.0
                    ? String.format(Locale.US, "%dM/s", Math.round(mb))
                    : String.format(Locale.US, "%.1fM/s", mb);
            }
        }
    }

    private void removeOverlay() {
        try {
            if (mOverlayRoot != null && mWindowManager != null) {
                mWindowManager.removeViewImmediate(mOverlayRoot);
            }
        } catch (Throwable ignored) {}
        mOverlayRoot = null;
        mOverlayPill = null;
        mTvOverlayDown = null;
        mTvOverlayUp = null;
        mOverlayParams = null;
    }

    // ==================== Notifications & Left-Side Status Icons ====================

    private PendingIntent createOpenAppIntent(int reqCode) {
        Intent openIntent = new Intent(this, MainMenuActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int piFlags = Build.VERSION.SDK_INT >= 23
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;
        return PendingIntent.getActivity(this, reqCode, openIntent, piFlags);
    }

    private Notification buildMainNotification(long rxPerSec, long txPerSec, boolean bitsMode, int mode, boolean hideIcon) {
        // In Right Overlay mode, keep left notification bar 100% clean using CHANNEL_HIDDEN
        boolean useHiddenChannel = hideIcon || (mode == NetworkSpeedHelper.ICON_MODE_RIGHT_OVERLAY);
        String channelId = useHiddenChannel ? CHANNEL_HIDDEN : CHANNEL_VISIBLE;

        String title;
        String body = "За сессию: Wi-Fi " + NetworkSpeedHelper.formatBytesShort(mSessionWifiBytes)
            + "  •  Моб. " + NetworkSpeedHelper.formatBytesShort(mSessionMobileBytes);

        if (mode == NetworkSpeedHelper.ICON_MODE_TWO) {
            title = "↓ Входящая: " + NetworkSpeedHelper.formatSpeedInline(rxPerSec, bitsMode);
        } else {
            title = "↓ " + NetworkSpeedHelper.formatSpeedInline(rxPerSec, bitsMode)
                + "   •   ↑ " + NetworkSpeedHelper.formatSpeedInline(txPerSec, bitsMode);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(this, channelId)
            : new Notification.Builder(this);

        if (useHiddenChannel) {
            builder.setSmallIcon(R.drawable.ic_tile);
        } else {
            Bitmap bmp = (mode == NetworkSpeedHelper.ICON_MODE_TWO)
                ? renderDirectionalSpeedBitmap(rxPerSec, bitsMode, true)
                : renderDualStackedBitmap(rxPerSec, txPerSec, bitsMode);
            builder.setSmallIcon(Icon.createWithBitmap(bmp));
        }

        builder.setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setWhen(System.currentTimeMillis() + 1000L)
            .setGroup("net_speed_group_dl")
            .setSortKey("0001")
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(createOpenAppIntent(10));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        return builder.build();
    }

    private Notification buildUploadNotification(long txPerSec, boolean bitsMode) {
        String title = "↑ Исходящая: " + NetworkSpeedHelper.formatSpeedInline(txPerSec, bitsMode);
        String body = "Отдача трафика в реальном времени";

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(this, CHANNEL_VISIBLE)
            : new Notification.Builder(this);

        Bitmap bmp = renderDirectionalSpeedBitmap(txPerSec, bitsMode, false);
        builder.setSmallIcon(Icon.createWithBitmap(bmp))
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setWhen(System.currentTimeMillis())
            .setGroup("net_speed_group_ul")
            .setSortKey("0002")
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(createOpenAppIntent(11));

        return builder.build();
    }

    private void initPaints() {
        Typeface condensedBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);

        mValPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mValPaint.setColor(Color.WHITE);
        mValPaint.setTextAlign(Paint.Align.CENTER);
        mValPaint.setTypeface(condensedBold);
        mValPaint.setFakeBoldText(true);

        mUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mUnitPaint.setColor(Color.WHITE);
        mUnitPaint.setTextAlign(Paint.Align.CENTER);
        mUnitPaint.setTypeface(condensedBold);

        mSplitValPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSplitValPaint.setColor(Color.WHITE);
        mSplitValPaint.setTextAlign(Paint.Align.LEFT);
        mSplitValPaint.setTypeface(condensedBold);
        mSplitValPaint.setFakeBoldText(true);

        mArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArrowPaint.setColor(Color.WHITE);
        mArrowPaint.setStyle(Paint.Style.FILL);
    }

    private void drawArrow(Canvas canvas, float cx, float top, float bottom, float halfW, boolean down) {
        Path p = new Path();
        float h = bottom - top;
        float stemHalfW = halfW * 0.36f;
        if (down) {
            float headTop = top + h * 0.48f;
            p.moveTo(cx - stemHalfW, top);
            p.lineTo(cx + stemHalfW, top);
            p.lineTo(cx + stemHalfW, headTop);
            p.lineTo(cx + halfW, headTop);
            p.lineTo(cx, bottom);
            p.lineTo(cx - halfW, headTop);
            p.lineTo(cx - stemHalfW, headTop);
            p.close();
        } else {
            float headBot = top + h * 0.52f;
            p.moveTo(cx, top);
            p.lineTo(cx + halfW, headBot);
            p.lineTo(cx + stemHalfW, headBot);
            p.lineTo(cx + stemHalfW, bottom);
            p.lineTo(cx - stemHalfW, bottom);
            p.lineTo(cx - stemHalfW, headBot);
            p.lineTo(cx - halfW, headBot);
            p.close();
        }
        canvas.drawPath(p, mArrowPaint);
    }

    private String[] splitValueAndUnit(long bytesPerSec, boolean bitsMode) {
        String valStr;
        String unitStr;
        if (bitsMode) {
            double bits = bytesPerSec * 8.0;
            if (bits < 1_000_000.0) {
                long kb = Math.round(bits / 1000.0);
                valStr = String.valueOf(kb);
                unitStr = "Kb/s";
            } else {
                double mb = bits / 1_000_000.0;
                valStr = mb >= 10.0
                    ? String.format(Locale.US, "%d", Math.round(mb))
                    : String.format(Locale.US, "%.1f", mb);
                unitStr = "Mb/s";
            }
        } else {
            if (bytesPerSec < 1024L * 1024L) {
                long kb = Math.round(bytesPerSec / 1024.0);
                valStr = String.valueOf(kb);
                unitStr = "KB/s";
            } else {
                double mb = bytesPerSec / (1024.0 * 1024.0);
                valStr = mb >= 10.0
                    ? String.format(Locale.US, "%d", Math.round(mb))
                    : String.format(Locale.US, "%.1f", mb);
                unitStr = "MB/s";
            }
        }
        return new String[]{valStr, unitStr};
    }

    private String formatCompactOneLine(long bytesPerSec, boolean bitsMode) {
        if (bitsMode) {
            double bits = bytesPerSec * 8.0;
            if (bits < 1_000_000.0) {
                return Math.round(bits / 1000.0) + "K";
            } else {
                double mb = bits / 1_000_000.0;
                return mb >= 10.0
                    ? String.format(Locale.US, "%dM", Math.round(mb))
                    : String.format(Locale.US, "%.1fM", mb);
            }
        } else {
            if (bytesPerSec < 1024L * 1024L) {
                return Math.round(bytesPerSec / 1024.0) + "K";
            } else {
                double mb = bytesPerSec / (1024.0 * 1024.0);
                return mb >= 10.0
                    ? String.format(Locale.US, "%dM", Math.round(mb))
                    : String.format(Locale.US, "%.1fM", mb);
            }
        }
    }

    private Bitmap renderDirectionalSpeedBitmap(long bytesPerSec, boolean bitsMode, boolean isDown) {
        final int size = 96;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        String[] parts = splitValueAndUnit(bytesPerSec, bitsMode);
        String valStr = parts[0];
        String unitStr = parts[1];

        int len = valStr.length();
        float valSize = (len <= 2) ? 58f : (len == 3 ? 50f : 40f);
        mValPaint.setTextSize(valSize);
        while (mValPaint.measureText(valStr) > 90f && valSize > 24f) {
            valSize -= 2f;
            mValPaint.setTextSize(valSize);
        }
        canvas.drawText(valStr, size / 2f, 52f, mValPaint);

        drawArrow(canvas, 13f, 62f, 92f, 11f, isDown);

        mUnitPaint.setTextAlign(Paint.Align.LEFT);
        float unitSize = 30f;
        mUnitPaint.setTextSize(unitSize);
        while (mUnitPaint.measureText(unitStr) > 65f && unitSize > 18f) {
            unitSize -= 2f;
            mUnitPaint.setTextSize(unitSize);
        }
        canvas.drawText(unitStr, 27f, 89f, mUnitPaint);
        mUnitPaint.setTextAlign(Paint.Align.CENTER);

        return bmp;
    }

    private Bitmap renderDualStackedBitmap(long rxPerSec, long txPerSec, boolean bitsMode) {
        final int size = 96;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        String dlStr = formatCompactOneLine(rxPerSec, bitsMode);
        String ulStr = formatCompactOneLine(txPerSec, bitsMode);

        drawArrow(canvas, 12f, 10f, 42f, 10f, true);
        float dlSize = dlStr.length() <= 3 ? 38f : 30f;
        mSplitValPaint.setTextSize(dlSize);
        while (mSplitValPaint.measureText(dlStr) > 68f && dlSize > 18f) {
            dlSize -= 2f;
            mSplitValPaint.setTextSize(dlSize);
        }
        canvas.drawText(dlStr, 26f, 40f, mSplitValPaint);

        drawArrow(canvas, 12f, 56f, 88f, 10f, false);
        float ulSize = ulStr.length() <= 3 ? 38f : 30f;
        mSplitValPaint.setTextSize(ulSize);
        while (mSplitValPaint.measureText(ulStr) > 68f && ulSize > 18f) {
            ulSize -= 2f;
            mSplitValPaint.setTextSize(ulSize);
        }
        canvas.drawText(ulStr, 26f, 86f, mSplitValPaint);

        return bmp;
    }

    private void createChannels() {
        if (mNotificationManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        try {
            mNotificationManager.deleteNotificationChannel("net_speed_visible_v1");
            mNotificationManager.deleteNotificationChannel("net_speed_hidden_v1");
        } catch (Throwable ignored) {}

        NotificationChannel visibleChannel = new NotificationChannel(
            CHANNEL_VISIBLE,
            "Скорость интернета (Статус-бар)",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        visibleChannel.setDescription("Показывает живую скорость интернета в статус-баре");
        visibleChannel.setShowBadge(false);
        visibleChannel.setSound(null, null);
        visibleChannel.enableVibration(false);
        visibleChannel.enableLights(false);
        visibleChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(visibleChannel);

        NotificationChannel hiddenChannel = new NotificationChannel(
            CHANNEL_HIDDEN,
            "Скорость интернета (Шторка / Скрыта при простое)",
            NotificationManager.IMPORTANCE_MIN
        );
        hiddenChannel.setDescription("Держит статистику в шторке без иконки слева");
        hiddenChannel.setShowBadge(false);
        hiddenChannel.setSound(null, null);
        hiddenChannel.enableVibration(false);
        mNotificationManager.createNotificationChannel(hiddenChannel);
    }

    @Override
    public void onDestroy() {
        sRunning = false;
        mHandler.removeCallbacks(mTickRunnable);
        removeOverlay();
        try {
            getContentResolver().unregisterContentObserver(mSettingsObserver);
        } catch (Throwable ignored) {}
        if (mReceiverRegistered) {
            try {
                unregisterReceiver(mScreenReceiver);
            } catch (Throwable ignored) {}
            mReceiverRegistered = false;
        }
        try {
            if (mNotificationManager != null) {
                mNotificationManager.cancel(NOTIF_ID_UP);
            }
        } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
