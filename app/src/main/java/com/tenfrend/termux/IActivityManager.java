package com.tenfrend.termux;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import java.lang.reflect.InvocationTargetException;

@SuppressLint("PrivateApi")
public class IActivityManager {

    private Object mAm;
    private final String mCallingAppPackage;
    private CrossVersionReflectedMethod mGetProviderMimeTypeMethod;
    private CrossVersionReflectedMethod mStartActivityAsUserMethod;
    private CrossVersionReflectedMethod mStartServiceMethod;
    private CrossVersionReflectedMethod mStopServiceMethod;

    public IActivityManager(String callingAppPackage, boolean setupMethods) throws Exception {
        mCallingAppPackage = callingAppPackage;

        try {
            mAm = android.app.ActivityManager.class
                    .getMethod("getService")
                    .invoke(null);
        } catch (Exception e) {
            mAm = Class.forName("android.app.ActivityManagerNative")
                    .getMethod("getDefault")
                    .invoke(null);
        }

        if (setupMethods) {
            getGetProviderMimeTypeMethod();
            getStartActivityAsUserMethod();
            getStartServiceMethod();
            getStopServiceMethod();
        }
    }

    private CrossVersionReflectedMethod getGetProviderMimeTypeMethod() {
        if (mGetProviderMimeTypeMethod != null) return mGetProviderMimeTypeMethod;
        mGetProviderMimeTypeMethod = new CrossVersionReflectedMethod(mAm.getClass())
                .tryMethodVariantInexact(
                        "getProviderMimeType",
                        Uri.class, "uri",
                        int.class, "userId"
                );
        return mGetProviderMimeTypeMethod;
    }

    private CrossVersionReflectedMethod getStartActivityAsUserMethod() {
        if (mStartActivityAsUserMethod != null) return mStartActivityAsUserMethod;
        mStartActivityAsUserMethod = new CrossVersionReflectedMethod(mAm.getClass())
                .tryMethodVariantInexact(
                        "startActivityAsUser",
                        "android.app.IApplicationThread", "caller", null,
                        String.class, "callingPackage", mCallingAppPackage,
                        Intent.class, "intent", null,
                        String.class, "resolvedType", null,
                        IBinder.class, "resultTo", null,
                        String.class, "resultWho", null,
                        int.class, "requestCode", -1,
                        int.class, "flags", 0,
                        Bundle.class, "options", null,
                        int.class, "userId", 0
                );
        return mStartActivityAsUserMethod;
    }

    private CrossVersionReflectedMethod getStartServiceMethod() {
        if (mStartServiceMethod != null) return mStartServiceMethod;
        mStartServiceMethod = new CrossVersionReflectedMethod(mAm.getClass())
                .tryMethodVariantInexact(
                        "startService",
                        "android.app.IApplicationThread", "caller", null,
                        Intent.class, "service", null,
                        String.class, "resolvedType", null,
                        boolean.class, "requireForeground", false,
                        String.class, "callingPackage", mCallingAppPackage,
                        int.class, "userId", 0
                ).tryMethodVariantInexact(
                        "startService",
                        "android.app.IApplicationThread", "caller", null,
                        Intent.class, "service", null,
                        String.class, "resolvedType", null,
                        String.class, "callingPackage", mCallingAppPackage,
                        int.class, "userId", 0
                ).tryMethodVariantInexact(
                        "startService",
                        "android.app.IApplicationThread", "caller", null,
                        Intent.class, "service", null,
                        String.class, "resolvedType", null,
                        int.class, "userId", 0
                );
        return mStartServiceMethod;
    }

    private CrossVersionReflectedMethod getStopServiceMethod() {
        if (mStopServiceMethod != null) return mStopServiceMethod;
        mStopServiceMethod = new CrossVersionReflectedMethod(mAm.getClass())
                .tryMethodVariantInexact(
                        "stopService",
                        "android.app.IApplicationThread", "caller", null,
                        Intent.class, "service", null,
                        String.class, "resolvedType", null,
                        int.class, "userId", 0
                );
        return mStopServiceMethod;
    }

    public int startActivityAsUser(Intent intent, String resolvedType, int flags, Bundle options, int userId) throws InvocationTargetException {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S || Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2)
            Workarounds.apply();

        return (Integer) getStartActivityAsUserMethod().invoke(
                mAm,
                "intent", intent,
                "resolvedType", resolvedType,
                "flags", flags,
                "options", options,
                "userId", userId
        );
    }

    public String getProviderMimeType(Uri uri, int userId) throws InvocationTargetException {
        if (!"content".equals(uri.getScheme())) return null;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            CrossVersionReflectedMethod method = getGetProviderMimeTypeMethod();
            if (method.isFound()) {
                return (String) getGetProviderMimeTypeMethod().invoke(
                        mAm,
                        "uri", uri,
                        "userId", userId
                );
            }
        }
        return null;
    }

    public ComponentName startService(Intent service, String resolvedType, int userId) throws InvocationTargetException {
        return (ComponentName) getStartServiceMethod().invoke(
                mAm,
                "service", service,
                "resolvedType", resolvedType,
                "userId", userId
        );
    }

    public int stopService(Intent service, String resolvedType, int userId) throws InvocationTargetException {
        return (Integer) getStopServiceMethod().invoke(
                mAm,
                "service", service,
                "resolvedType", resolvedType,
                "userId", userId
        );
    }
}