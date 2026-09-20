package io.github.andrea_lyz.assistrestore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Small reflection helpers. Every lookup returns {@code null} instead of throwing. */
final class Refl {
    private Refl() {
    }

    static Class<?> load(ClassLoader classLoader, String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (Throwable t) {
            return null;
        }
    }

    static Method staticMethod(ClassLoader classLoader, String className, String name, Class<?>... params) {
        Class<?> owner = load(classLoader, className);
        if (owner == null) {
            return null;
        }
        try {
            Method method = owner.getMethod(name, params);
            try {
                method.setAccessible(true);
            } catch (Throwable ignored) {
                // 公开方法无需提升可访问性。
            }
            return method;
        } catch (Throwable t) {
            return method(owner, name, params);
        }
    }

    /** 在当前类及父类中查找声明的方法。 */
    static Method method(Class<?> owner, String name, Class<?>... params) {
        for (Class<?> type = owner; type != null && type != Object.class; type = type.getSuperclass()) {
            try {
                Method method = type.getDeclaredMethod(name, params);
                try {
                    method.setAccessible(true);
                } catch (Throwable ignored) {
                    // 保留该方法；公开方法不需要提升可访问性。
                }
                return method;
            } catch (Throwable ignored) {
                // 继续向父类查找。
            }
        }
        return null;
    }

    /** 在当前类及父类中查找声明的字段。 */
    static Field field(Class<?> owner, String name) {
        for (Class<?> type = owner; type != null && type != Object.class;
                type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                try {
                    field.setAccessible(true);
                } catch (Throwable ignored) {
                    // 公开字段无需提升可访问性。
                }
                return field;
            } catch (Throwable ignored) {
                // 继续向父类查找。
            }
        }
        return null;
    }

    static Boolean staticBooleanField(ClassLoader classLoader, String className, String fieldName) {
        Class<?> owner = load(classLoader, className);
        if (owner == null) {
            return null;
        }
        try {
            return owner.getField(fieldName).getBoolean(null);
        } catch (Throwable t) {
            return null;
        }
    }
}
