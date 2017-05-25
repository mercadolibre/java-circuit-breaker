package com.mercadolibre.resilience.breaker.control;

import com.mercadolibre.resilience.breaker.stats.Stats;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentNavigableMap;

class ControlTestBase {

    @SuppressWarnings("unchecked")
    protected ConcurrentNavigableMap<Long, Stats> getStats(OnOffCircuitControl control) {
        try {
            return (ConcurrentNavigableMap<Long, Stats>) getAttribute("stats", control);
        } catch (NoSuchFieldException | IllegalAccessException e ) {
            throw new RuntimeException(e);
        }
    }

    protected Object invokeMethod(String name, Object target, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?>[] argClasses = new Class<?>[args.length];

        for (int i = 0; i < args.length; i++)
            argClasses[i] = args[i].getClass();

        Method method = target.getClass().getDeclaredMethod(name, argClasses);
        method.setAccessible(true);

        return method.invoke(target, args);
    }

    protected void setAttribute(String name, Object target, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);

        field.set(target, value);
    }

    protected Object getAttribute(String name, Object target) throws IllegalAccessException, NoSuchFieldException {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);

        return f.get(target);
    }
}
