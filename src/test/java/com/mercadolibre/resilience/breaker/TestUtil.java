package com.mercadolibre.resilience.breaker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class TestUtil {

    public static Object getAttribute(Object instance, String name) throws NoSuchFieldException, IllegalAccessException {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);

        return field.get(instance);
    }

    public static void setAttribute(Object instance, String name, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);

        Field modifiers = field.getClass().getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(instance, value);
    }

    public static Object invoke(Object instance, String name, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        return invokeSimple(instance, instance.getClass(), name, true, args);
    }

    private static Object invokeSimple(Object instance, Class<?> clazz, String name, boolean recurse, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Class<?>[] classes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) classes[i] = resolveClass(args[i]);

        try {
            Method method = clazz.getDeclaredMethod(name, classes);
            method.setAccessible(true);

            return method.invoke(instance, args);
        } catch (NoSuchMethodException e) {
            if (recurse) return invokeSimple(instance, clazz.getSuperclass(), name, false, args);

            throw e;
        }
    }

    private static Class<?> resolveClass(Object instance) throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = instance.getClass();
        try {
            return (Class<?>) clazz.getField("TYPE").get(null);
        } catch (NoSuchFieldException e) {
            return clazz;
        }
    }

}
