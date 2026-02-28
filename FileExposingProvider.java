package com.tenfrend.svgshortcuts;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * 自定义 ContentProvider，将任意文件路径映射为 content:// URI 供其他应用访问。
 * 安全性：只允许 MT 管理器包名访问，防止恶意应用通过此 provider 读取任意文件。
 */
public class FileExposingProvider extends ContentProvider {

    private static final String TAG = "FileExposingProvider";
    private static final String MT_PACKAGE = "bin.mt.plus"; // 允许访问的包名

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        // 检查调用者是否是 MT 管理器
        if (!isCallerMT()) {
            Log.w(TAG, "拒绝非 MT 应用的访问");
            throw new SecurityException("Only MT Manager is allowed to access this file");
        }

        // 只允许读模式
        if (!"r".equals(mode)) {
            throw new SecurityException("Only read mode is supported");
        }

        // 从 URI 中获取文件路径（URI 的 path 部分即为原始路径，已自动解码）
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            throw new FileNotFoundException("No path in URI");
        }

        File file = new File(path);
        Log.d(TAG, "开放文件: " + file.getAbsolutePath());

        // 检查文件是否存在且可读
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + path);
        }
        if (!file.canRead()) {
            throw new SecurityException("File not readable");
        }

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    /**
     * 判断调用者 UID 对应的包名是否是 MT 管理器
     */
    private boolean isCallerMT() {
        int uid = Binder.getCallingUid();
        String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
        if (packages != null) {
            for (String pkg : packages) {
                if (MT_PACKAGE.equals(pkg)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // 不需要实现，返回 null 即可
        return null;
    }

    // 以下查询/插入/更新/删除方法无需实现，直接返回 null 或 0
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }

    /**
     * 根据文件路径生成 content URI
     *
     * @param context  上下文
     * @param filePath 绝对路径
     * @return content://authority/编码后的路径
     */
    public static Uri getUriForFile(android.content.Context context, String filePath) {
        String authority = context.getPackageName() + ".fileprovider";
        // 对路径进行编码，保留斜杠不编码，以保证 URI 的 path 部分可读
        String encodedPath = Uri.encode(filePath, "/");
        return Uri.parse("content://" + authority + "/" + encodedPath);
    }
}