package com.plusls.llsmanager.util;

import java.lang.reflect.Field;

public class ReflectUtil {
    public static Field getField(Class<?> clazz, String target) {
        try {
            Field field = clazz.getDeclaredField(target);
            field.setAccessible(true);
            return field;

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
    }
}
