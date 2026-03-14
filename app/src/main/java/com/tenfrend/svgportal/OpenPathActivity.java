package com.tenfrend.svgportal;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import com.tenfrend.termux.AmConstants;
import com.tenfrend.termux.IActivityManager;
import com.tenfrend.termux.ReflectionUtils;

import java.io.File;

public class OpenPathActivity extends Activity {

    private static final String TAG = "OpenPath";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 在 onCreate 开头添加 SharedPreferences 读取
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean useCanary = prefs.getBoolean("use_canary", true); // 默认使用共存版
        
        String packageName = useCanary ? "bin.mt.plus.canary" : "bin.mt.plus";
        // 假设 MT 管理器的文件浏览 Activity 类名为 bin.mt.plus.OpenFileActivity（可根据实际情况调整）
        String activityName = packageName + ".OpenFileActivity";
        // 绕过隐藏 API 限制
        ReflectionUtils.bypassHiddenAPIReflectionRestrictions();

        // 解析传入的文件路径
        Uri dataUri = getIntent().getData();
        String pathExtra = getIntent().getStringExtra("path");

        String filePath = null;
        if (dataUri != null) {
            filePath = dataUri.getPath();  // 从 file:// 或 content:// 获取路径
        } else if (pathExtra != null) {
            filePath = pathExtra;
        }

        if (filePath == null) {
            Log.e(TAG, "No file path provided");
            finish();
            return;
        }

        File file = new File(filePath);

        // 构建目标 Intent（发给 MT 管理器）
        Intent target = new Intent(Intent.ACTION_VIEW);
        target.setDataAndType(Uri.fromFile(file), getMimeType(file));
        target.setClassName(packageName,activityName);
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // 获取当前用户 ID
        int userId = AmConstants.getCurrentUserId();
        if (userId == AmConstants.USER_NULL) {
            userId = Process.myUid() / AmConstants.PER_USER_RANGE;
        }

        try {
            // 使用 IActivityManager 启动
            IActivityManager am = new IActivityManager(getPackageName(), true);
            int result = am.startActivityAsUser(target, target.getType(), 0, null, userId);
            if (result == AmConstants.START_SUCCESS) {
                Log.i(TAG, "Activity started successfully");
            } else {
                Log.e(TAG, "Failed to start activity, result code: " + result);
                // 降级尝试普通启动（可能抛出 FileUriExposedException）
                fallbackStart(target);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting activity via IActivityManager", e);
            fallbackStart(target);
        }

        finish();
    }

    private void fallbackStart(Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Fallback start failed", e);
        }
    }

    private String getMimeType(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            String ext = name.substring(dot + 1).toLowerCase();
            switch (ext) {
                case "txt": return "text/plain";
                case "jpg": case "jpeg": return "image/jpeg";
                case "png": return "image/png";
                case "mp4": return "video/mp4";
                case "pdf": return "application/pdf";
                default: return "*/*";
            }
        }
        return "*/*";
    }
}