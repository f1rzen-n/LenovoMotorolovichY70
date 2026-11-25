package com.moto.resolutionmaster;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

public class ShellHelper {

    private static final String TAG = "ResolutionMaster";

    public static final String SHIZUKU_PACKAGE = "moe.shizuku.privileged.api";
    public static final String SHIZUKU_PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api";
    public static final String SHIZUKU_GITHUB_URL = "https://github.com/RikkaApps/Shizuku/releases";
    public static final String SHIZUKU_4PDA_URL = "https://4pda.to/forum/index.php?showtopic=944888";

    public static final String PERM_WRITE_SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS";
    public static final String ADB_GRANT_COMMAND = "adb shell pm grant com.moto.resolutionmaster android.permission.WRITE_SECURE_SETTINGS";

    public static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1001;

    public static boolean hasWriteSecureSettings(Context context) {
        if (context == null) return false;
        return context.checkCallingOrSelfPermission(PERM_WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean grantWriteSecureSettings(Context context) {
        if (hasWriteSecureSettings(context)) {
            return true;
        }
        String pkg = context.getPackageName();
        String cmd = "pm grant " + pkg + " " + PERM_WRITE_SECURE_SETTINGS;
        boolean ok = runCommand(context, cmd);
        if (ok) {
            Log.i(TAG, "Successfully granted WRITE_SECURE_SETTINGS via Shell/Shizuku/Root!");
        }
        return ok;
    }

    public static boolean runCommand(Context context, String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return false;
        }

        // 1. Try Root (su)
        if (runRoot(cmd)) {
            Log.d(TAG, "Command executed via Root: " + cmd);
            return true;
        }

        // 2. Try Shizuku IPC
        if (runShizuku(cmd)) {
            Log.d(TAG, "Command executed via Shizuku: " + cmd);
            return true;
        }

        return false;
    }

    private static volatile long sLastRootCheckMs = 0L;
    private static volatile boolean sLastRootResult = false;

    public static String runCommandWithOutput(Context context, String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return "";
        }

        // 1. Try Root first (consistent with runCommand priority)
        if (isRootAvailable()) {
            java.lang.Process p = null;
            try {
                p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
                String out = readAllAndWait(p);
                if (out != null) {
                    return out;
                }
            } catch (Throwable t) {
                Log.e(TAG, "runCommandWithOutput Root error", t);
                destroyQuietly(p);
            }
        }

        // 2. Try Shizuku IPC
        if (isShizukuPermissionGranted()) {
            java.lang.Process p = null;
            try {
                p = startShizukuProcess(cmd);
                if (p != null) {
                    String out = readAllAndWait(p);
                    if (out != null) {
                        return out;
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "runCommandWithOutput Shizuku error", t);
                destroyQuietly(p);
            }
        }

        return "";
    }

    public static boolean isRootAvailable() {
        long now = android.os.SystemClock.elapsedRealtime();
        if (sLastRootCheckMs > 0 && (now - sLastRootCheckMs) < 5000L) {
            return sLastRootResult;
        }
        java.lang.Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            readAllAndWait(p);
            boolean ok = p.exitValue() == 0;
            sLastRootResult = ok;
            sLastRootCheckMs = now;
            return ok;
        } catch (Throwable t) {
            destroyQuietly(p);
            sLastRootResult = false;
            sLastRootCheckMs = now;
            return false;
        }
    }

    public static boolean isShizukuInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(SHIZUKU_PACKAGE, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isShizukuRunning() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isShizukuPermissionGranted() {
        try {
            if (!isShizukuRunning()) return false;
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean writeShellFile(Context context, byte[] data, String targetPath) {
        if (data == null || targetPath == null) return false;

        // Check if target file already exists and size matches
        try {
            String checkCmd = "stat -c %s " + targetPath + " 2>/dev/null || wc -c < " + targetPath + " 2>/dev/null";
            String sizeOut = runCommandWithOutput(context, checkCmd);
            if (sizeOut != null && sizeOut.trim().equals(String.valueOf(data.length))) {
                return true;
            }
        } catch (Throwable ignored) {}

        // 1. Try Shizuku process stdin
        if (isShizukuPermissionGranted()) {
            java.lang.Process p = null;
            try {
                p = startShizukuProcess("cat > " + targetPath + " && chmod 644 " + targetPath);
                if (p != null) {
                    java.io.OutputStream os = p.getOutputStream();
                    os.write(data);
                    os.flush();
                    os.close();
                    readAllAndWait(p);
                    if (p.exitValue() == 0) return true;
                }
            } catch (Throwable t) {
                Log.e(TAG, "writeShellFile Shizuku stdin error", t);
                destroyQuietly(p);
            }
        }

        // 2. Try Root process stdin
        if (isRootAvailable()) {
            java.lang.Process p = null;
            try {
                p = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat > " + targetPath + " && chmod 644 " + targetPath});
                java.io.OutputStream os = p.getOutputStream();
                os.write(data);
                os.flush();
                os.close();
                readAllAndWait(p);
                if (p.exitValue() == 0) return true;
            } catch (Throwable t) {
                Log.e(TAG, "writeShellFile Root stdin error", t);
                destroyQuietly(p);
            }
        }

        // 3. Fallback: Base64 string stream via shell
        try {
            String b64 = android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP);
            String b64Cmd = "echo '" + b64 + "' | base64 -d > " + targetPath + " && chmod 644 " + targetPath;
            if (runCommand(context, b64Cmd)) {
                return true;
            }
        } catch (Throwable t) {
            Log.e(TAG, "writeShellFile base64 error", t);
        }

        return false;
    }

    public static void requestShizukuPermission() {
        try {
            if (isShizukuRunning()) {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to request Shizuku permission", t);
        }
    }

    public static boolean runShizuku(String cmd) {
        java.lang.Process p = null;
        try {
            if (!isShizukuPermissionGranted()) {
                Log.w(TAG, "runShizuku: permission not granted");
                return false;
            }
            p = startShizukuProcess(cmd);
            if (p != null) {
                readAllAndWait(p);
                int exit = p.exitValue();
                Log.d(TAG, "runShizuku exited with code: " + exit + " for cmd: " + cmd);
                return exit == 0;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to run command via Shizuku", t);
            destroyQuietly(p);
        }
        return false;
    }

    public static void openShizuku(Context context) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(SHIZUKU_PACKAGE);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to open Shizuku", t);
        }
    }

    public static void openUrl(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to open URL: " + url, t);
        }
    }

    private static boolean runRoot(String cmd) {
        java.lang.Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            readAllAndWait(p);
            boolean ok = p.exitValue() == 0;
            if (!ok) sLastRootCheckMs = 0L;
            return ok;
        } catch (Throwable t) {
            destroyQuietly(p);
            return false;
        }
    }

    private static java.lang.Process startShizukuProcess(String cmd) throws Exception {
        Method newProcessMethod = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
        newProcessMethod.setAccessible(true);
        return (java.lang.Process) newProcessMethod.invoke(null, new String[]{"sh", "-c", cmd}, null, null);
    }

    private static String readAllAndWait(java.lang.Process p) throws Exception {
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
             java.io.InputStream err = p.getErrorStream();
             java.io.OutputStream out = p.getOutputStream()) {
            // Drain stdout
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            // Drain stderr so child never deadlocks on full pipe buffer
            byte[] errBuf = new byte[1024];
            while (err.read(errBuf) != -1) {
                // discard or log if needed
            }
        }
        p.waitFor();
        return output.toString();
    }

    private static void destroyQuietly(java.lang.Process p) {
        if (p != null) {
            try { p.destroy(); } catch (Throwable ignored) {}
        }
    }

    public static void copyToClipboard(Context context, String text, String label) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText(label, text));
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to copy to clipboard", t);
        }
    }
}
