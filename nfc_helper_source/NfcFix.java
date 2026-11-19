import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Fixed framework-routing operations for an authorized shell or root; no raw-controller interface. */
public final class NfcFix {
    private static final String[] IDENTITY = {"uid", "model", "device", "sku", "build", "sdk",
        "boot_locked", "verified_boot", "nfc_profile", "current_user", "nfc_state", "api_shape", "route_count", "caller_package"};
    private static final String FACTORY = "eSE1,eSE1,eSE1";
    private static final String LEGACY  = "DH,DH,eSE1";
    private static final String APPLIED = "DH,DH,DH";

    public interface Backend {
        Map<String, String> snapshot() throws Exception;
        List<?> readRoutes() throws Exception;
        void writeRoutes(int user, List<String> routes, String systemCode) throws Exception;
        void pause() throws Exception;
    }

    public static final class BackendFailure extends Exception {
        private static final long serialVersionUID = 1L;
        final String category;
        final Map<String, String> partialSnapshot;
        public BackendFailure(String category) { this(category, null); }
        public BackendFailure(String category, Map<String, String> partial) {
            super("NFC backend operation failed"); this.category = category;
            partialSnapshot = partial == null ? null : new LinkedHashMap<String, String>(partial);
        }
    }

    static String failureCategory(Exception failure) {
        Throwable cause = failure;
        while (cause instanceof java.lang.reflect.InvocationTargetException && cause.getCause() != null) cause = cause.getCause();
        if (cause instanceof BackendFailure) {
            String c = ((BackendFailure) cause).category;
            if (Arrays.asList("api_missing", "api_ambiguous", "api_shape", "service_unavailable",
                    "caller_package_unverified", "permission_denied", "service_disconnected", "invalid_argument", "invalid_routes").contains(c)) return c;
        }
        if (cause instanceof SecurityException) return "permission_denied";
        if (cause instanceof NoSuchMethodException || cause instanceof ClassNotFoundException) return "api_missing";
        if (cause instanceof IllegalArgumentException) return "invalid_argument";
        if ("android.os.DeadObjectException".equals(cause.getClass().getName()) ||
                "android.os.RemoteException".equals(cause.getClass().getName())) return "service_disconnected";
        return "failed";
    }

    public static final class Report {
        public int exitCode = 1;
        public final Map<String, String> values = new LinkedHashMap<String, String>();
        private Report(String mode) {
            values.put("protocol_version", "2"); values.put("mode", mode);
            for (String key : IDENTITY) values.put(key, "unavailable");
            values.put("api_shape", "unknown");
            values.put("before", "unavailable"); values.put("requested_args", "none");
            values.put("after", "unavailable"); values.put("changed", "false");
            values.put("result", "failed"); values.put("error", "none");
            values.put("rollback", "not_needed"); values.put("rollback_args", "none");
            values.put("rollback_after", "unavailable"); values.put("known_routes", "false");
            values.put("system_code_route_verified", "false");
        }

        public String format() {
            StringBuilder out = new StringBuilder();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                out.append(entry.getKey()).append('=');
                StringBuilder encoded = new StringBuilder();
                String value = entry.getValue();
                for (int i = 0; i < value.length(); i++) {
                    char c = value.charAt(i);
                    if (c >= 32 && c <= 126 && c != '%') encoded.append(c);
                    else {
                        String hex = Integer.toHexString(c).toUpperCase(java.util.Locale.ROOT);
                        encoded.append('%');
                        for (int n = hex.length(); n < 4; n++) encoded.append('0');
                        encoded.append(hex);
                    }
                }
                out.append(encoded.length() <= 512 ? encoded.toString() : "unavailable").append('\n');
            }
            return out.toString();
        }
    }

    private static final class ReadbackMismatch extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static boolean modeValid(String mode) {
        return "status".equals(mode) || "apply".equals(mode) || "undo".equals(mode) || "restore-legacy".equals(mode)
                || "force-apply".equals(mode) || "force-undo".equals(mode);
    }

    private static boolean known(List<String> r) {
        if (r.size() == 4 && !"eSE1".equals(r.get(3))) return false;
        String first = String.join(",", r.subList(0, 3));
        return FACTORY.equals(first) || LEGACY.equals(first) || APPLIED.equals(first);
    }

    private static List<String> target(String mode, int count) {
        String first = ("apply".equals(mode) || "force-apply".equals(mode)) ? APPLIED
                : (("undo".equals(mode) || "force-undo".equals(mode)) ? FACTORY : LEGACY);
        return Arrays.asList((first + (count == 4 ? ",eSE1" : "")).split(",", -1));
    }

    private static void snapshot(Report r, Map<String, String> state) {
        if (state == null) throw new IllegalArgumentException("Missing state");
        for (String key : IDENTITY) {
            String value = state.get(key);
            r.values.put(key, value == null ? ("api_shape".equals(key) ? "unknown" : "unavailable") : value);
        }
    }

    private static String guard(Report r) {
        // Allow shell UID 2000 or Root UID 0
        String uid = r.values.get("uid");
        if (!"2000".equals(uid) && !"0".equals(uid)) return "guard_uid";

        String user = r.values.get("current_user");
        if (!"0".equals(user)) return "guard_current_user";

        String state = r.values.get("nfc_state");
        if (!"on".equals(state)) return "guard_nfc_state";

        String sku = r.values.get("sku");
        boolean validSku = (sku != null && (sku.startsWith("XT2611") || sku.contains("Y70") || sku.contains("dn")));
        if (!validSku) {
            String model = r.values.get("model");
            String device = r.values.get("device");
            boolean validDev = (model != null && (model.contains("Y70") || model.contains("XT2611") || model.contains("Edge 70")))
                    || (device != null && (device.contains("Y70") || device.contains("dipper") || device.contains("halo") || device.contains("XT2611")));
            String mode = r.values.get("mode");
            if (!validDev && !"force-apply".equals(mode) && !"force-undo".equals(mode)) {
                return "guard_sku";
            }
        }

        String shape = r.values.get("api_shape");
        if (!"android16".equals(shape) && !"android17".equals(shape)) return "guard_api_shape";

        boolean newer = "android17".equals(shape);
        if (!(newer ? "4" : "3").equals(r.values.get("route_count"))) return "guard_route_count";
        if (!(newer ? "com.android.shell" : "not_required").equals(r.values.get("caller_package"))) return "guard_caller_package";
        return null;
    }

    private static Report finish(Report r, int code, String result, String error) {
        r.exitCode = code; r.values.put("result", result); r.values.put("error", error); return r;
    }

    private static List<String> read(Backend backend, int count) throws Exception {
        List<?> raw = backend.readRoutes();
        if (raw == null || raw.size() != count) throw new BackendFailure("invalid_routes");
        List<String> copy = new ArrayList<String>(count);
        for (Object item : raw) {
            if (!(item instanceof String)) throw new BackendFailure("invalid_routes");
            copy.add((String) item);
        }
        return copy;
    }

    private static String csv(List<String> routes) {
        List<String> safe = new ArrayList<String>();
        for (String route : routes) {
            safe.add(Arrays.asList("DH", "eSE1", "eSE2", "UICC", "UICC1", "UICC2", "SIM1", "SIM2").contains(route) ? route : "other");
        }
        return String.join(",", safe);
    }

    private static String arguments(List<String> routes) {
        return "0," + csv(routes) + (routes.size() == 4 ? ",null,com.android.shell" : ",eSE1");
    }

    private static void write(Backend backend, List<String> routes) throws Exception {
        backend.writeRoutes(0, new ArrayList<String>(routes), routes.size() == 4 ? null : "eSE1");
    }

    private static List<String> verify(Backend backend, List<String> expected) throws Exception {
        for (int attempt = 0; attempt < 6; attempt++) {
            List<String> current = read(backend, expected.size());
            if (current.equals(expected)) return current;
            if (attempt < 5) backend.pause();
        }
        throw new ReadbackMismatch();
    }

    private static Report snapshotFailure(Report r, String phase, Exception failure) {
        if (failure instanceof BackendFailure && ((BackendFailure) failure).partialSnapshot != null)
            snapshot(r, ((BackendFailure) failure).partialSnapshot);
        return finish(r, 1, "failed", phase + "_" + failureCategory(failure));
    }

    private static void capturedCapability(Report r, String shape, int count, String caller) {
        r.values.put("api_shape", shape); r.values.put("route_count", String.valueOf(count)); r.values.put("caller_package", caller);
    }

    public static Report execute(String[] args, Backend backend) {
        String mode = args != null && args.length >= 1 && args[0] != null ? args[0] : "unavailable";
        Report r = new Report(mode);
        if (args == null || args.length < 1 || !modeValid(mode)) return finish(r, 2, "rejected", "invalid_mode");
        try { snapshot(r, backend.snapshot()); }
        catch (Exception failure) { return snapshotFailure(r, "snapshot", failure); }

        String rejected = guard(r);
        if (rejected != null) return finish(r, 2, "rejected", rejected);

        List<String> before;
        final String shape = r.values.get("api_shape");
        final int count = Integer.parseInt(r.values.get("route_count"));
        final String caller = r.values.get("caller_package");
        try { before = read(backend, count); }
        catch (Exception failure) { return finish(r, 1, "failed", "initial_read_" + failureCategory(failure)); }

        String original = csv(before);
        r.values.put("before", original); r.values.put("after", original);
        r.values.put("known_routes", Boolean.toString(known(before)));
        if ("status".equals(mode)) return finish(r, 0, "ok", "none");
        if (!known(before) && !mode.startsWith("force-")) return finish(r, 2, "rejected", "unknown_routes");

        List<String> expected = target(mode, count);
        r.values.put("requested_args", arguments(expected));
        if (before.equals(expected)) return finish(r, 0, "ok", "none");

        try { snapshot(r, backend.snapshot()); }
        catch (Exception failure) {
            snapshotFailure(r, "prewrite_snapshot", failure); capturedCapability(r, shape, count, caller); return r;
        }
        rejected = guard(r);
        boolean capabilityChanged = !shape.equals(r.values.get("api_shape")) ||
                !String.valueOf(count).equals(r.values.get("route_count")) || !caller.equals(r.values.get("caller_package"));
        capturedCapability(r, shape, count, caller);
        if (rejected != null) return finish(r, 2, "rejected", rejected);
        if (capabilityChanged) return finish(r, 2, "rejected", "capability_changed");

        try {
            if (!before.equals(read(backend, count))) return finish(r, 2, "rejected", "routes_changed");
        } catch (Exception failure) { return finish(r, 1, "failed", "prewrite_read_" + failureCategory(failure)); }

        String phase = "write";
        try {
            write(backend, expected);
            phase = "readback";
            r.values.put("after", csv(verify(backend, expected)));
            r.values.put("changed", "true");
            return finish(r, 0, "ok", "none");
        } catch (Exception failure) {
            String failureCode = failure instanceof ReadbackMismatch ? "readback_mismatch" : phase + "_" + failureCategory(failure);
            r.values.put("after", "unavailable"); r.values.put("changed", "true");
            r.values.put("rollback_args", arguments(before));
            try {
                write(backend, before);
                String restored = csv(verify(backend, before));
                r.values.put("rollback", "verified"); r.values.put("rollback_after", restored);
                r.values.put("after", restored); r.values.put("changed", "false");
                return finish(r, 1, "failed", failureCode);
            } catch (Exception rollbackFailure) {
                r.values.put("rollback", "failed");
                return finish(r, 3, "failed", failureCode);
            }
        }
    }

    public static void main(String[] args) {
        Backend backend;
        try {
            backend = (Backend) Class.forName("AndroidNfcBackend").getDeclaredConstructor().newInstance();
        } catch (Exception failure) {
            Report r = new Report(args != null && args.length >= 1 ? args[0] : "unavailable");
            finish(r, 1, "failed", "backend_unavailable");
            System.out.print(r.format());
            System.exit(r.exitCode);
            return;
        }
        Report r = execute(args, backend);
        System.out.print(r.format());
        System.exit(r.exitCode);
    }
}
