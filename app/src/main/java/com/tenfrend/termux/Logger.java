package com.tenfrend.termux;

import android.util.Log;

public class Logger {
    public static void logError(String tag, String message) {
        Log.e(tag, message);
    }

    public static void logStackTraceWithMessage(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }
}