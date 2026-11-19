package com.moto.resolutionmaster;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PackageHelper {

    private static final String TAG = "PackageHelper";
    private static final String PREF_APPOPS = "appops_freeze";

    // Packages protected by Motorola firmware (in /system_ext/etc/nondisable/ or marked protected)
    public static final Set<String> NONDISABLE_PACKAGES = new HashSet<>(Arrays.asList(
        "com.motorola.paks",
        "com.motorola.paks.notification",
        "com.motorola.ccc.devicemanagement"
    ));

    public interface BatchCallback {
        void onProgress(int current, int total, String packageName, boolean success);
        void onComplete(int successCount, int total);
    }

    public static boolean isProtectedOrNonDisable(String packageName) {
        return packageName != null && NONDISABLE_PACKAGES.contains(packageName);
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isPackageEnabled(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();

            // 1. Check standard component enabled settings
            int state = pm.getApplicationEnabledSetting(packageName);
            if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
                return false;
            }

            // 2. Check suspended state (Android 10+)
            try {
                if (pm.isPackageSuspended(packageName)) {
                    return false;
                }
            } catch (Throwable ignored) {}

            // 3. For protected/nondisable packages without Root: check AppOps frozen state
            if (isProtectedOrNonDisable(packageName)) {
                SharedPreferences prefs = context.getSharedPreferences(PREF_APPOPS, Context.MODE_PRIVATE);
                if (prefs.getBoolean("frozen_" + packageName, false)) {
                    return false;
                }
            }

            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return ai.enabled;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean disablePackage(Context context, String packageName) {
        // If already disabled or suspended, consider done
        if (!isPackageEnabled(context, packageName)) {
            return true;
        }

        boolean ok = false;

        // 1. Protected Motorola firmware packages (nondisable):
        // Motorola's PackageManagerService rejects pm disable-user even under Root (su)!
        // Therefore, for both Root and Shizuku, we use the AppOps deep isolation & freeze method.
        if (isProtectedOrNonDisable(packageName)) {
            String freezeCmd = 
                "cmd appops set " + packageName + " RUN_IN_BACKGROUND ignore; " +
                "cmd appops set " + packageName + " RUN_ANY_IN_BACKGROUND ignore; " +
                "cmd appops set " + packageName + " START_FOREGROUND ignore; " +
                "cmd appops set " + packageName + " POST_NOTIFICATION ignore; " +
                "cmd appops set " + packageName + " WAKE_LOCK ignore; " +
                "cmd deviceidle whitelist -" + packageName + "; " +
                "am force-stop " + packageName;
            ShellHelper.runCommand(context, freezeCmd);

            context.getSharedPreferences(PREF_APPOPS, Context.MODE_PRIVATE)
                .edit().putBoolean("frozen_" + packageName, true).apply();
            return true;
        }

        // 2. Standard packages: if Root available, su executes pm disable-user
        if (ShellHelper.isRootAvailable()) {
            ok = ShellHelper.runCommand(context, "pm disable-user --user 0 " + packageName);
            if (!ok || isPackageEnabled(context, packageName)) {
                ok = ShellHelper.runCommand(context, "pm suspend --user 0 " + packageName);
            }
        } else {
            // 3. Shizuku / ADB shell: try pm disable-user
            ok = ShellHelper.runCommand(context, "pm disable-user --user 0 " + packageName);

            // 4. Fallback for Android 14 component restrictions (Facebook, Glance): pm suspend
            if (!ok || isPackageEnabled(context, packageName)) {
                ok = ShellHelper.runCommand(context, "pm suspend --user 0 " + packageName);
            }
        }

        // Always cut background execution and force stop
        ShellHelper.runCommand(context,
            "cmd appops set " + packageName + " RUN_IN_BACKGROUND ignore; " +
            "cmd appops set " + packageName + " RUN_ANY_IN_BACKGROUND ignore; " +
            "am force-stop " + packageName);

        boolean nowDisabled = !isPackageEnabled(context, packageName);
        Log.d(TAG, "disablePackage " + packageName + " ok=" + ok + " nowDisabled=" + nowDisabled);
        return ok || nowDisabled;
    }

    public static boolean enablePackage(Context context, String packageName) {
        if (isProtectedOrNonDisable(packageName)) {
            context.getSharedPreferences(PREF_APPOPS, Context.MODE_PRIVATE)
                .edit().putBoolean("frozen_" + packageName, false).apply();
        }

        String cmd = 
            "pm enable --user 0 " + packageName + "; " +
            "pm unsuspend --user 0 " + packageName + "; " +
            "cmd appops set " + packageName + " RUN_IN_BACKGROUND allow; " +
            "cmd appops set " + packageName + " RUN_ANY_IN_BACKGROUND allow; " +
            "cmd appops set " + packageName + " START_FOREGROUND allow; " +
            "cmd appops set " + packageName + " POST_NOTIFICATION allow; " +
            "cmd appops set " + packageName + " WAKE_LOCK allow";
        boolean ok = ShellHelper.runCommand(context, cmd);
        Log.d(TAG, "enablePackage " + packageName + ": " + ok);
        return ok;
    }

    public static List<AppInfo> getInstalledApps(Context context) {
        List<AppInfo> result = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        try {
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
            for (PackageInfo pi : packages) {
                if (pi.applicationInfo == null) continue;

                String pkg = pi.packageName;
                // Exclude self from disabling
                if ("com.moto.resolutionmaster".equals(pkg) || "moe.shizuku.privileged.api".equals(pkg)) {
                    continue;
                }

                String label;
                try {
                    CharSequence cs = pi.applicationInfo.loadLabel(pm);
                    label = (cs != null) ? cs.toString() : pkg;
                } catch (Throwable t) {
                    label = pkg;
                }

                Drawable icon;
                try {
                    icon = pi.applicationInfo.loadIcon(pm);
                } catch (Throwable t) {
                    icon = pm.getDefaultActivityIcon();
                }

                boolean isSystem = (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                boolean isEnabled = isPackageEnabled(context, pkg);

                PackageDescriptions.PackageMeta meta = PackageDescriptions.getMeta(pkg);

                result.add(new AppInfo(pkg, label, icon, isSystem, isEnabled,
                        meta.description, meta.safetyLevel, meta.getSafetyLabel(), meta.getSafetyColor()));
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load installed apps", t);
        }

        Collections.sort(result, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo o1, AppInfo o2) {
                return o1.label.compareToIgnoreCase(o2.label);
            }
        });

        return result;
    }

    public static void runBatchDisable(final Context context, final List<String> packageList, final BatchCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int total = packageList.size();
                int success = 0;
                for (int i = 0; i < total; i++) {
                    String pkg = packageList.get(i);
                    boolean ok = disablePackage(context, pkg);
                    if (ok) success++;
                    if (callback != null) {
                        callback.onProgress(i + 1, total, pkg, ok);
                    }
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException ignored) {}
                }
                if (callback != null) {
                    callback.onComplete(success, total);
                }
            }
        }).start();
    }

    public static void runBatchEnable(final Context context, final List<String> packageList, final BatchCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int total = packageList.size();
                int success = 0;
                for (int i = 0; i < total; i++) {
                    String pkg = packageList.get(i);
                    boolean ok = enablePackage(context, pkg);
                    if (ok) success++;
                    if (callback != null) {
                        callback.onProgress(i + 1, total, pkg, ok);
                    }
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException ignored) {}
                }
                if (callback != null) {
                    callback.onComplete(success, total);
                }
            }
        }).start();
    }
}
