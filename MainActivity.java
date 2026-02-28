package com.tenfrend.svgshortcuts;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 主 Activity：接收其他应用打开 XML/SVG 的请求，
 * 读取文件第一行的注释路径，然后使用 MT 管理器打开该路径指向的文件。
 * 界面为小弹窗，显示目标路径后自动关闭。
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SVGShortcuts";
    private static final String MT_PACKAGE = "bin.mt.plus"; // MT 管理器包名
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 100;

    private TextView tvPath;
    private Intent pendingIntent; // 用于保存权限请求成功后需要处理的 Intent

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvPath = findViewById(R.id.tv_path);

        // 处理传入 Intent（首次启动或从 onNewIntent 来）
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 当 activity 已存在且以 singleInstance 模式启动时，新 intent 会通过此方法传入
        handleIntent(intent);
    }

    /**
     * 处理传入的 VIEW Intent
     */
    private void handleIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) {
            finish(); // 非 VIEW 意图直接退出
            return;
        }

        Uri uri = intent.getData();
        if (uri == null) {
            Toast.makeText(this, "无效的文件 URI", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 保存 intent，以便权限授予后重新处理
        pendingIntent = intent;

        // 检查并请求必要的存储权限
        if (!checkAndRequestPermissions()) {
            return; // 权限请求中，等待回调
        }

        // 已有权限，开始处理文件
        processFile(uri);
    }

    /**
     * 检查并请求存储权限（根据 Android 版本不同）
     *
     * @return true 表示权限已具备，可以继续；false 表示正在请求权限
     */
    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+：需要 MANAGE_EXTERNAL_STORAGE 权限
            if (!Environment.isExternalStorageManager()) {
                // 引导用户去设置页面授予权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                Toast.makeText(this, "请授予所有文件访问权限后重试", Toast.LENGTH_LONG).show();
                finish(); // 直接退出，用户授予权限后需要重新发送打开请求
                return false;
            }
        } else {
            // Android 10 及以下：需要 READ_EXTERNAL_STORAGE 权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_STORAGE_PERMISSION);
                return false; // 等待用户响应
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予成功，重新处理之前保存的 intent
                if (pendingIntent != null) {
                    handleIntent(pendingIntent);
                } else {
                    finish();
                }
            } else {
                Toast.makeText(this, "需要存储权限才能读取文件", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * 核心处理：从 URI 读取文件第一行，提取注释中的路径，然后用 MT 管理器打开
     */
    private void processFile(Uri uri) {
        String targetPath = extractPathFromUri(uri);
        if (targetPath == null) {
            Toast.makeText(this, "文件格式错误：第一行应为 <!-- 路径 -->", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 在弹窗上显示目标路径
        tvPath.setText("目标路径: " + targetPath);

        // 通过自定义 ContentProvider 暴露目标文件，获得 content:// URI
        Uri exposeUri = FileExposingProvider.getUriForFile(this, targetPath);
        if (exposeUri == null) {
            Toast.makeText(this, "无法访问目标文件", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 启动 MT 管理器打开该文件
        Intent mtIntent = new Intent(Intent.ACTION_VIEW);
        mtIntent.setDataAndType(exposeUri, getMimeType(targetPath));
        mtIntent.setPackage(MT_PACKAGE); // 直接指定 MT 管理器
        mtIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 授予临时读取权限

        try {
            startActivity(mtIntent);
            // 延迟关闭自己，让用户看到路径（约1.5秒）
            new Handler().postDelayed(this::finishAndRemoveTask, 1500);
        } catch (Exception e) {
            Log.e(TAG, "启动 MT 管理器失败", e);
            Toast.makeText(this, "请确保已安装 MT 管理器", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 从传入的 URI 读取文件第一行，提取 <!-- 路径 --> 中的路径
     *
     * @param uri 文件 URI（content 或 file 协议）
     * @return 提取的路径，若失败返回 null
     */
    @Nullable
    private String extractPathFromUri(Uri uri) {
        // 统一使用 ContentResolver 打开输入流，兼容 content 和 file 协议
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            if (is == null) return null;

            String firstLine = reader.readLine();
            if (firstLine == null) return null;

            // 提取 <!-- 和 --> 之间的内容
            int start = firstLine.indexOf("<!--");
            int end = firstLine.indexOf("-->");
            if (start == -1 || end == -1 || end <= start + 4) {
                return null;
            }

            String path = firstLine.substring(start + 4, end).trim();
            // 路径可能经过 URL 编码（如空格被编码为 %20），进行解码
            path = URLDecoder.decode(path, "UTF-8");
            return path;
        } catch (Exception e) {
            Log.e(TAG, "读取文件失败", e);
            return null;
        }
    }

    /**
     * 根据文件扩展名返回 MIME 类型
     */
    private String getMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".xml")) return "text/xml";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}