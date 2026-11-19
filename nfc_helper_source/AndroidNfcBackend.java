import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reflects reviewed NFC API shapes. Supports both UID 2000 (Shell) and UID 0 (Root). */
public final class AndroidNfcBackend implements NfcFix.Backend {
    private Object adapter, emulation;
    private Method getState, getRoutes, overwrite;
    private String apiShape;
    private static final Class<?>[] API16 = {int.class, String.class, String.class, String.class, String.class};
    private static final Class<?>[] API17 = {int.class, String.class, String.class, String.class, String.class, String.class, String.class};

    public AndroidNfcBackend() {}

    private static String property(Method get, String name) throws Exception {
        Object value = get.invoke(null, name);
        return value instanceof String ? (String) value : "unavailable";
    }

    static String inspectApi(Class<?> adapterType, Class<?> cardType) throws Exception {
        Method state = adapterType.getMethod("getState");
        Method read = cardType.getMethod("getRoutingStatus");
        if (state.getReturnType() != int.class || !List.class.isAssignableFrom(read.getReturnType()))
            throw new NfcFix.BackendFailure("api_shape");
        Method selected = null;
        for (Method candidate : cardType.getMethods()) {
            if ("overwriteRoutingTable".equals(candidate.getName())) {
                if (selected != null) throw new NfcFix.BackendFailure("api_ambiguous");
                selected = candidate;
            }
        }
        if (selected == null) throw new NfcFix.BackendFailure("api_missing");
        if (selected.getReturnType() != void.class) throw new NfcFix.BackendFailure("api_shape");
        if (Arrays.equals(API16, selected.getParameterTypes())) return "android16";
        if (Arrays.equals(API17, selected.getParameterTypes())) return "android17";
        throw new NfcFix.BackendFailure("api_shape");
    }

    private void connect() throws Exception {
        if (adapter != null) return;
        Class<?> binderType = Class.forName("android.os.IBinder");
        Object binder = Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null, "nfc");
        if (binder == null) throw new NfcFix.BackendFailure("service_unavailable");
        Object candidate = Class.forName("android.nfc.INfcAdapter$Stub").getMethod("asInterface", binderType).invoke(null, binder);
        if (candidate == null) throw new NfcFix.BackendFailure("service_unavailable");
        Class<?> adapterType = Class.forName("android.nfc.INfcAdapter");
        Object card = adapterType.getMethod("getNfcCardEmulationInterface").invoke(candidate);
        if (card == null) throw new NfcFix.BackendFailure("service_unavailable");
        Class<?> cardType = Class.forName("android.nfc.INfcCardEmulation");
        String shape = inspectApi(adapterType, cardType);
        Method state = adapterType.getMethod("getState"), read = cardType.getMethod("getRoutingStatus");
        Method write = cardType.getMethod("overwriteRoutingTable", "android17".equals(shape) ? API17 : API16);
        adapter = candidate; emulation = card; getState = state; getRoutes = read; overwrite = write; apiShape = shape;
    }

    private static boolean shellPackageBelongsToUid(int uid) throws Exception {
        if (uid == 0) return true; // root has all permissions
        if (uid != 2000) return false;
        Object pm = Class.forName("android.app.AppGlobals").getMethod("getPackageManager").invoke(null);
        if (pm == null) return false;
        Object result = Class.forName("android.content.pm.IPackageManager").getMethod("getPackagesForUid", int.class).invoke(pm, uid);
        return result instanceof String[] && Arrays.asList((String[]) result).contains("com.android.shell");
    }

    public Map<String, String> snapshot() throws Exception {
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("api_shape", "unknown"); values.put("route_count", "unavailable"); values.put("caller_package", "unavailable");
        try {
            int uid = ((Integer) Class.forName("android.os.Process").getMethod("myUid").invoke(null)).intValue();
            values.put("uid", String.valueOf(uid));
            Method get = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
            String[][] props = {{"model", "ro.product.model"}, {"device", "ro.product.device"}, {"sku", "ro.boot.hardware.sku"},
                {"build", "ro.build.display.id"}, {"sdk", "ro.build.version.sdk"}, {"boot_locked", "ro.boot.flash.locked"},
                {"verified_boot", "ro.boot.verifiedbootstate"}, {"nfc_profile", "ro.vendor.hw.nfc"}};
            for (String[] p : props) values.put(p[0], property(get, p[1]));
            values.put("current_user", String.valueOf(Class.forName("android.app.ActivityManager").getMethod("getCurrentUser").invoke(null)));
            connect();
            values.put("api_shape", apiShape); values.put("route_count", "android17".equals(apiShape) ? "4" : "3");
            Object state = getState.invoke(adapter);
            values.put("nfc_state", Integer.valueOf(1).equals(state) ? "off" : Integer.valueOf(2).equals(state) ? "turning_on" :
                    Integer.valueOf(3).equals(state) ? "on" : Integer.valueOf(4).equals(state) ? "turning_off" : "unavailable");
            if ("android17".equals(apiShape)) {
                if (!shellPackageBelongsToUid(uid)) throw new NfcFix.BackendFailure("caller_package_unverified");
                values.put("caller_package", "com.android.shell");
            } else {
                values.put("caller_package", "not_required");
            }
            return values;
        } catch (Exception failure) {
            throw new NfcFix.BackendFailure(NfcFix.failureCategory(failure), values);
        }
    }

    public List<?> readRoutes() throws Exception {
        connect();
        Object result = getRoutes.invoke(emulation);
        if (!(result instanceof List)) throw new NfcFix.BackendFailure("invalid_routes");
        return new ArrayList<Object>((List<?>) result);
    }

    static Object[] writeArguments(String shape, int user, List<String> routes, String systemCode) throws Exception {
        boolean newer = "android17".equals(shape);
        if ((!newer && !"android16".equals(shape)) || routes == null || routes.size() != (newer ? 4 : 3))
            throw new NfcFix.BackendFailure("api_shape");
        if (user != 0 || (newer ? systemCode != null : !"eSE1".equals(systemCode)))
            throw new NfcFix.BackendFailure("invalid_argument");
        for (String route : routes) {
            if (!"DH".equals(route) && !"eSE1".equals(route)) throw new NfcFix.BackendFailure("invalid_routes");
        }
        if (newer && !"eSE1".equals(routes.get(3))) throw new NfcFix.BackendFailure("invalid_routes");
        if (newer) return new Object[]{Integer.valueOf(user), routes.get(0), routes.get(1), routes.get(2), routes.get(3), systemCode, "com.android.shell"};
        return new Object[]{Integer.valueOf(user), routes.get(0), routes.get(1), routes.get(2), systemCode};
    }

    public void writeRoutes(int user, List<String> routes, String systemCode) throws Exception {
        connect();
        overwrite.invoke(emulation, writeArguments(apiShape, user, routes, systemCode));
    }

    public void pause() throws InterruptedException { Thread.sleep(150); }
}
