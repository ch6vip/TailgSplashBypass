package com.tailg.lsposed.adblock;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class ReflectionAccess {
    private ReflectionAccess() {
    }

    static Object getField(Object target, String name) throws ReflectiveOperationException {
        if (target == null) {
            throw new IllegalArgumentException("target == null");
        }
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    static Object invokeNoArg(Object target, String name) throws ReflectiveOperationException {
        if (target == null) {
            throw new IllegalArgumentException("target == null");
        }
        Method method = findMethod(target.getClass(), name);
        method.setAccessible(true);
        try {
            return method.invoke(target);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ReflectiveOperationException reflective) {
                throw reflective;
            }
            throw e;
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(type.getName() + "#" + name);
    }

    private static Method findMethod(Class<?> type, String name) throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name + "()");
    }
}
