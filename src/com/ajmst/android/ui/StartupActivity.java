package com.ajmst.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.ajmst.android.service.DatabaseHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Imports the existing user database before any service opens a private database. */
public final class StartupActivity extends Activity {
    private static final int REQUEST_DATABASE = 1;
    private static final String TAG = "StartupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File database = getDatabasePath(DatabaseHelper.DATABASE_NAME);
        if (database.isFile() && database.length() > 0) {
            openApp();
        } else if (savedInstanceState == null) {
            showImportPrompt();
        }
    }

    private void showImportPrompt() {
        new AlertDialog.Builder(this)
                .setTitle("导入商品数据库")
                .setMessage("首次使用请选择原来的 AJMST.db 文件。导入时会复制到应用内部，原文件不会被修改。")
                .setPositiveButton("选择数据库", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    startActivityForResult(intent, REQUEST_DATABASE);
                })
                .setNeutralButton("空白开始", (dialog, which) -> openApp())
                .setNegativeButton("退出", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_DATABASE) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            showImportPrompt();
            return;
        }
        try {
            importDatabase(data.getData());
            Toast.makeText(this, "数据库导入成功", Toast.LENGTH_SHORT).show();
            openApp();
        } catch (Exception e) {
            Log.e(TAG, "Database import failed", e);
            Toast.makeText(this, "导入失败，请选择有效的 AJMST.db", Toast.LENGTH_LONG).show();
            showImportPrompt();
        }
    }

    private void importDatabase(Uri uri) throws IOException {
        File target = getDatabasePath(DatabaseHelper.DATABASE_NAME);
        File directory = target.getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Cannot create database directory");
        }
        File temporary = new File(directory, DatabaseHelper.DATABASE_NAME + ".import");
        try {
            try (InputStream input = getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(temporary)) {
                if (input == null) throw new IOException("Cannot open selected database");
                byte[] buffer = new byte[32768];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                output.getFD().sync();
            }
            validateDatabase(temporary);
            if (target.exists() && target.length() == 0 && !target.delete()) {
                throw new IOException("Cannot remove empty database file");
            }
            if (target.exists()) {
                throw new IOException("Database already exists");
            }
            if (!temporary.renameTo(target)) {
                throw new IOException("Cannot install database");
            }
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }

    private void validateDatabase(File file) throws IOException {
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            int version = database.getVersion();
            if (version < 14 || version > 15) throw new IOException("Unsupported database version: " + version);
            cursor = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='advspkfk'", null);
            if (!cursor.moveToFirst()) throw new IOException("Product table is missing");
            cursor.close();
            cursor = database.rawQuery("PRAGMA quick_check", null);
            if (!cursor.moveToFirst() || !"ok".equalsIgnoreCase(cursor.getString(0))) {
                throw new IOException("Database integrity check failed");
            }
        } catch (RuntimeException e) {
            throw new IOException("Invalid SQLite database", e);
        } finally {
            if (cursor != null) cursor.close();
            if (database != null) database.close();
        }
    }

    private void openApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
