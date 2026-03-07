package com.tenfrend.svgportal;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Entrances extends Activity {

    private static final String TAG = "Entrances";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 根据启动的别名分发到对应的处理逻辑
        ComponentName component = getIntent().getComponent();
        if (component == null) {
            finish();
            return;
        }

        String className = component.getClassName();
        Intent intent = getIntent();
        Uri data = intent.getData();
        String action = intent.getAction();
        String type = intent.getType();

        // 根据别名分发
        if (className.endsWith(".EntranceA")) {
            handleEntranceA(data, action, type);
        } else if (className.endsWith(".EntranceB")) {
            handleEntranceB(data, action, type);
        } else if (className.endsWith(".EntranceC")) {
            handleEntranceC(data, action, type);
        } else if (className.endsWith(".EntranceD")) {
            handleEntranceD(data, action, type);
        } else if (className.endsWith(".EntranceE")) {
            handleEntranceE(data, action, type);
        } else if (className.endsWith(".EntranceF")) {
            handleEntranceF(data, action, type);
        } else {
            Log.w(TAG, "Unknown entrance: " + className);
        }

        finish(); // 分发完成后关闭自身
    }

    private void handleEntranceA(Uri data, String action, String type) {
        Log.i(TAG, "EntranceA triggered: 向量传送门 (文件打开方式)");

        if (data == null) {
            Log.e(TAG, "No data URI provided");
            return;
        }

        ContentResolver resolver = getContentResolver();
        StringBuilder contentBuilder = new StringBuilder();
        boolean headerValid = false;
        boolean collecting = false;
        boolean foundEnd = false;

        try (InputStream is = resolver.openInputStream(data);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) {
                    if (!"<!--".equals(line)) {
                        Log.d(TAG, "First line mismatch, not a valid portal file");
                        return;
                    }
                } else if (lineNumber == 2) {
                    if (!"target".equals(line)) {
                        Log.d(TAG, "Second line mismatch, not a valid portal file");
                        return;
                    }
                    headerValid = true;
                    collecting = true; // 开始收集内容
                } else if (collecting) {
                    if ("-->".equals(line)) {
                        foundEnd = true;
                        break; // 结束标记
                    } else {
                        contentBuilder.append(line).append("\n");
                    }
                }
            }

            if (!headerValid) {
                Log.d(TAG, "File header not valid");
                return;
            }
            if (!foundEnd) {
                Log.d(TAG, "No closing --> found");
                return;
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading file", e);
            return;
        }

        // 获取收集到的文本并删除所有的 "\\\n" (反斜杠+换行符)
        String rawContent = contentBuilder.toString();
        String processedPath = rawContent.replace("\\\n", "");

        // 仅删除最后一个换行符（如果有），保留其他空白字符
        if (processedPath.endsWith("\n")) {
            processedPath = processedPath.substring(0, processedPath.length() - 1);
        }

        // 直接使用该字符串作为路径，不再执行 trim()
        if (processedPath.isEmpty()) {
            Log.e(TAG, "Processed path is empty");
        return;
}

        // 启动 OpenPathActivity 打开该路径
        File targetFile = new File(processedPath);
        if (!targetFile.exists()) {
            Log.e(TAG, "Target file does not exist for SVG-P: " + processedPath);
            // 仍然尝试打开，让MT管理器去处理具体问题
        }

        Intent openIntent = new Intent(this, OpenPathActivity.class);
        openIntent.setData(Uri.fromFile(targetFile));
        startActivity(openIntent);
    }

    private void handleEntranceB(Uri data, String action, String type) {
        Log.i(TAG, "EntranceB triggered: 生成传送门 (单文件分享方式)");
        // TODO: 实现单文件分享逻辑
    }

    private void handleEntranceC(Uri data, String action, String type) {
        Log.i(TAG, "EntranceC triggered: 生成传送门库 (多文件分享方式)");
        // TODO: 实现多文件分享逻辑
    }

    private void handleEntranceD(Uri data, String action, String type) {
        Log.i(TAG, "EntranceD triggered: 加入任务清单 (单/多文件分享方式)");
        // TODO: 实现单/多文件分享逻辑
    }

    private void handleEntranceE(Uri data, String action, String type) {
        Log.i(TAG, "EntranceE triggered: 向量传送侧门 (文件打开方式)");
        // TODO: 实现文件打开逻辑
    }

    private void handleEntranceF(Uri data, String action, String type) {
        Log.i(TAG, "EntranceF triggered: 调用Termux操作 (单/多文件分享方式)");
        // TODO: 实现调用 Termux 的逻辑
    }
}