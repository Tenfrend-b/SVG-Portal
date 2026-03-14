package com.tenfrend.svgportal;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

public class GenPortal {
    private static final String TAG = "GenPortal";

    // 原有的 generate 方法：优先原目录，失败则转媒体目录
    public static void generate(Context context, String targetFilePath) {
        if (targetFilePath == null || targetFilePath.isEmpty()) {
            Log.e(TAG, "Target file path is empty");
            Toast.makeText(context, "错误：文件路径为空", Toast.LENGTH_SHORT).show();
            return;
        }

        File targetFile = new File(targetFilePath);
        String fileName = targetFile.getName();
        File parentDir = targetFile.getParentFile();
        if (parentDir == null) {
            Log.e(TAG, "Parent directory is null");
            Toast.makeText(context, "错误：无法获取父目录", Toast.LENGTH_SHORT).show();
            return;
        }
        File newFile = new File(parentDir, fileName + ".svg");

        // 读取模板
        String templateContent = readTemplate(context);
        if (templateContent == null) return;

        String outputContent = templateContent.replace("{{PATH}}", targetFilePath);

        // 尝试写入原目录
        try {
            writeFile(newFile, outputContent);
            Log.i(TAG, "SVG Portal file created: " + newFile.getAbsolutePath());
            Toast.makeText(context, "Portal文件生成成功：" + newFile.getName(), Toast.LENGTH_LONG).show();
            return;
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Failed to write to original location: " + e.getMessage());
        }

        // 原目录失败，尝试媒体目录
        File mediaDir = getMediaDir(context);
        if (mediaDir == null) {
            Log.e(TAG, "Cannot get media directory");
            Toast.makeText(context, "无法获取媒体目录", Toast.LENGTH_SHORT).show();
            return;
        }
        File mediaFile = getUniqueFile(mediaDir, fileName + ".svg");
        try {
            writeFile(mediaFile, outputContent);
            Log.i(TAG, "SVG Portal file created in media dir: " + mediaFile.getAbsolutePath());
            Toast.makeText(context, "已在媒体目录生成：" + mediaFile.getName(), Toast.LENGTH_LONG).show();
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Failed to write to media dir", e);
            Toast.makeText(context, "生成失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // 批量生成：所有文件直接放入媒体目录
    public static void batchGenerateToMedia(Context context, List<String> paths) {
        File mediaDir = getMediaDir(context);
        if (mediaDir == null) {
            Toast.makeText(context, "无法获取媒体目录", Toast.LENGTH_SHORT).show();
            return;
        }

        String templateContent = readTemplate(context);
        if (templateContent == null) return;

        int success = 0, failed = 0;
        for (String path : paths) {
            if (path == null || path.trim().isEmpty()) continue;
            String trimmedPath = path.trim();
            String outputContent = templateContent.replace("{{PATH}}", trimmedPath);

            // 生成文件名
            File targetFile = new File(trimmedPath);
            String baseName = targetFile.getName();
            if (baseName.isEmpty()) baseName = "path_" + System.currentTimeMillis();
            String fileName = baseName + ".svg";
            File outFile = getUniqueFile(mediaDir, fileName);

            try {
                writeFile(outFile, outputContent);
                success++;
            } catch (IOException | SecurityException e) {
                Log.e(TAG, "Failed to write file for path: " + trimmedPath, e);
                failed++;
            }
        }

        final int successFinal = success, failedFinal = failed;
        ((Activity) context).runOnUiThread(() ->
                Toast.makeText(context, "批量生成完成：成功 " + successFinal + " 个，失败 " + failedFinal + " 个", Toast.LENGTH_LONG).show()
        );

    }

    // 读取模板文件
    private static String readTemplate(Context context) {
        try (InputStream is = context.getResources().openRawResource(R.raw.common);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read template", e);
            Toast.makeText(context, "错误：无法读取模板文件", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // 获取应用的媒体目录
    private static File getMediaDir(Context context) {
        File[] mediaDirs = context.getExternalMediaDirs();
        if (mediaDirs != null && mediaDirs.length > 0) {
            File dir = mediaDirs[0];
            if (!dir.exists()) dir.mkdirs();
            return dir;
        }
        return null;
    }

    // 获取不重名的文件
    private static File getUniqueFile(File dir, String baseName) {
        File file = new File(dir, baseName);
        if (!file.exists()) return file;
        int dotIndex = baseName.lastIndexOf('.');
        String nameWithoutExt = (dotIndex == -1) ? baseName : baseName.substring(0, dotIndex);
        String ext = (dotIndex == -1) ? "" : baseName.substring(dotIndex);
        int counter = 1;
        while (true) {
            String newName = nameWithoutExt + "_" + counter + ext;
            File newFile = new File(dir, newName);
            if (!newFile.exists()) return newFile;
            counter++;
        }
    }

    // 写入文件内容
    private static void writeFile(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(content);
            writer.flush();
        }
    }
}