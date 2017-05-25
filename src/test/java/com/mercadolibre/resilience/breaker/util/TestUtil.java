package com.mercadolibre.resilience.breaker.util;

import java.lang.reflect.Field;

public class TestUtil {

    public static Object getAttribute(String name, Object target) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);

            return field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
