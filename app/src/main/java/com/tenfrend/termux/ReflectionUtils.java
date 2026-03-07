package com.tenfrend.termux;

import android.os.Build;
import android.util.Log;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionUtils {

    private static boolean HIDDEN_API_REFLECTION_RESTRICTIONS_BYPASSED = Build.VERSION.SDK_INT < Build.VERSION_CODES.P;

    public synchronized static void bypassHiddenAPIReflectionRestrictions() {
        if (!HIDDEN_API_REFLECTION_RESTRICTIONS_BYPASSED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("");
                HIDDEN_API_REFLECTION_RESTRICTIONS_BYPASSED = true;
            } catch (Throwable t) {
                Log.e("ReflectionUtils", "Failed to bypass hidden API restrictions", t);
            }
        }
    }

    public static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
            Log.e("ReflectionUtils", "Failed to get method " + methodName + " from " + clazz, e);
            return null;
        }
    }

    public static Object invokeMethod(Method method, Object obj, Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(obj, args);
        } catch (Exception e) {
            Log.e("ReflectionUtils", "Failed to invoke method " + method.getName(), e);
            return null;
        }
    }
}