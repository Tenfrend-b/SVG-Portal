package com.tenfrend.termux;

import android.annotation.SuppressLint;
import java.lang.reflect.Method;

public class AmConstants {

    public static final int PER_USER_RANGE = 100000;
    public static final int FIRST_APPLICATION_UID = 10000;
    public static final int LAST_APPLICATION_UID = 19999;
    public static final int USER_ALL = -1;
    public static final int USER_CURRENT = -2;
    public static final int USER_NULL = -10000;
    public static final int START_SUCCESS = 0;

    public static int getCurrentUserId() {
        ReflectionUtils.bypassHiddenAPIReflectionRestrictions();

        String className = "android.app.ActivityManager";
        String methodName = "getCurrentUser";
        try {
            @SuppressLint("PrivateApi") Class<?> clazz = Class.forName(className);
            Method method = ReflectionUtils.getDeclaredMethod(clazz, methodName);
            if (method == null) return USER_NULL;

            Integer userId = (Integer) ReflectionUtils.invokeMethod(method, null);
            return userId != null && userId >= 0 ? userId : USER_NULL;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage("AmConstants", "Failed to call getCurrentUser", e);
            return USER_NULL;
        }
    }
}