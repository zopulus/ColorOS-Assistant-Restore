package io.github.andrea_lyz.assistrestore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Makes the Google app believe it runs on a device Google ships Circle to Search for.
 *
 * <p>GSA decides whether Circle to Search is available from the device identity, so on a ColorOS
 * device the feature stays off even when the framework side is fixed. The values below are the
 * Samsung S24 Ultra identity that GSA accepts, applied only inside the Google app process because
 * that is the only process this hook is injected into.</p>
 *
 * <p>The technique follows the approach used by the Oplus-Assistant-Hook project.</p>
 */
final class GoogleAppHooks {
    private static final String MANUFACTURER = "samsung";
    private static final String BRAND = "samsung";
    private static final String MODEL = "SM-S928B";
    private static final String PRODUCT = "e3s";
    private static final String DEVICE = "e3s";

    private GoogleAppHooks() {
    }

    static void install(AssistRestoreModule module, ClassLoader classLoader) {
        if (!AssistConfig.isEnabled(HookPrefs.get())) {
            module.logInfo("google_app_identity_spoof_skipped reason=module_disabled");
            return;
        }
        if (!AssistConfig.spoofGoogleBuild(HookPrefs.get())) {
            module.logInfo("google_app_identity_spoof_skipped reason=switch_off");
            return;
        }
        try {
            Class<?> build = Class.forName("android.os.Build", true, classLoader);
            setStaticField(build, "MANUFACTURER", MANUFACTURER);
            setStaticField(build, "BRAND", BRAND);
            setStaticField(build, "MODEL", MODEL);
            setStaticField(build, "PRODUCT", PRODUCT);
            setStaticField(build, "DEVICE", DEVICE);
            module.logInfo("google_app_identity_spoofed model=" + MODEL
                    + " manufacturer=" + MANUFACTURER);
        } catch (Throwable t) {
            module.logError("google_app_identity_spoof_failed", t);
        }
    }

    private static void setStaticField(Class<?> owner, String name, String value)
            throws Throwable {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        try {
            field.set(null, value);
            return;
        } catch (IllegalAccessException finalField) {
            // Android 17 禁止反射写入 Build 的 final 字段，改用 Unsafe 写入 ART 静态槽位。
            Object unsafe = unsafe();
            Method staticFieldBase = unsafe.getClass().getMethod("staticFieldBase", Field.class);
            Method staticFieldOffset = unsafe.getClass().getMethod("staticFieldOffset", Field.class);
            Method putObjectVolatile = unsafe.getClass().getMethod(
                    "putObjectVolatile", Object.class, long.class, Object.class);
            Object base = staticFieldBase.invoke(unsafe, field);
            long offset = ((Number) staticFieldOffset.invoke(unsafe, field)).longValue();
            putObjectVolatile.invoke(unsafe, base, offset, value);
        }
    }

    private static Object unsafe() throws Throwable {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeClass.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        return singleton.get(null);
    }
}
