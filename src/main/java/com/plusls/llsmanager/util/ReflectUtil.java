package com.plusls.llsmanager.util;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public class ReflectUtil {
    @Nullable
    public static Field getField(Class<?> clazz, String target) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(target);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return field;
    }
}
