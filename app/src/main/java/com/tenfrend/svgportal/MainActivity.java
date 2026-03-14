package com.tenfrend.svgportal;

import java.io.File;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

public class MainActivity extends Activity {
    private static final String[] ENTRANCE_ALIASES = {
            ".EntranceA",
            ".EntranceB",
            ".EntranceC",
            ".EntranceD",
            ".EntranceE",
            ".EntranceF"
    };
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText(R.string.entrance_manager_title);
        title.setTextSize(24);
        layout.addView(title);

        // 共存版开关
        final SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        Switch switchCanary = new Switch(this);
        switchCanary.setText("使用共存版 MT 管理器");
        switchCanary.setChecked(prefs.getBoolean("use_canary", true));
        switchCanary.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("use_canary", isChecked).apply());
        layout.addView(switchCanary);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(0xFFCCCCCC);
        layout.addView(divider);

        // 权限状态提示
        TextView permStatus = new TextView(this);
        permStatus.setText(getPermissionStatusText());
        permStatus.setPadding(0, 16, 0, 8);
        layout.addView(permStatus);

        Button permButton = new Button(this);
        permButton.setText("设置权限");
        permButton.setOnClickListener(v -> requestPermissions());
        layout.addView(permButton);
        
        Button batchButton = new Button(this);
        batchButton.setText("批量生成Portal");
        batchButton.setOnClickListener(v -> showBatchInputDialog());
        layout.addView(batchButton);
        // 再添加一个分隔线
        View divider2 = new View(this);
        divider2.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        divider2.setBackgroundColor(0xFFCCCCCC);
        layout.addView(divider2);

        // 入口开关列表
        for (String aliasSuffix : ENTRANCE_ALIASES) {
            String fullName = getPackageName() + aliasSuffix;
            Switch sw = new Switch(this);
            sw.setText(getEntranceDisplayName(aliasSuffix));
            sw.setChecked(isComponentEnabled(fullName));
            sw.setOnCheckedChangeListener((buttonView, isChecked) ->
                    setComponentEnabled(fullName, isChecked));
            layout.addView(sw);
        }

        setContentView(layout);
    }

    /** 获取当前权限状态文本 */
    private String getPermissionStatusText() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                return "所有文件访问权限：已授予 ✓";
            } else {
                return "所有文件访问权限：未授予 ✗";
            }
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return "存储写入权限：已授予 ✓";
            } else {
                return "存储写入权限：未授予 ✗";
            }
        }
    }

    /** 处理权限请求（跳转设置或请求运行时权限） */
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+：跳转系统设置
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            // Android 10 及以下：请求 WRITE_EXTERNAL_STORAGE
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // 刷新界面
            recreate();
        }
    }

    private String getEntranceDisplayName(String suffix) {
        switch (suffix) {
            case ".EntranceA": return getString(R.string.entrance_a_name);
            case ".EntranceB": return getString(R.string.entrance_b_name);
            case ".EntranceC": return getString(R.string.entrance_c_name);
            case ".EntranceD": return getString(R.string.entrance_d_name);
            case ".EntranceE": return getString(R.string.entrance_e_name);
            case ".EntranceF": return getString(R.string.entrance_f_name);
            default: return suffix;
        }
    }

    private boolean isComponentEnabled(String componentName) {
        int state = getPackageManager().getComponentEnabledSetting(
                new ComponentName(this, componentName));
        if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return true;
        } else if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            return false;
        } else {
            try {
                ActivityInfo info = getPackageManager().getActivityInfo(
                        new ComponentName(this, componentName), 0);
                return info.enabled;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
    }

    private void setComponentEnabled(String componentName, boolean enabled) {
        int newState = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                               : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        getPackageManager().setComponentEnabledSetting(
                new ComponentName(this, componentName),
                newState,
                PackageManager.DONT_KILL_APP);
    }
    private void showBatchInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入路径列表");
    
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setLines(10);
        input.setVerticalScrollBarEnabled(true);
        input.setHint("每行一个路径");
    
        builder.setView(input);
    
        builder.setPositiveButton("生成", (dialog, which) -> {
            String text = input.getText().toString();
            processBatchInput(text);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void processBatchInput(String text) {
        String[] lines = text.split("\\n");
        List<String> paths = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                paths.add(trimmed);
            }
        }
        if (paths.isEmpty()) {
            Toast.makeText(this, "没有有效的路径", Toast.LENGTH_SHORT).show();
            return;
        }
        // 在后台线程执行批量生成
        new Thread(() -> GenPortal.batchGenerateToMedia(this, paths)).start();
                File targetFile = new File("/sdcard/Android/media/com.tenfrend.svgportal/");
                Intent openIntent = new Intent(this, OpenPathActivity.class);
                openIntent.setData(Uri.fromFile(targetFile));
                startActivity(openIntent);


    }
}