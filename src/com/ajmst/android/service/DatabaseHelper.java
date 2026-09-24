package com.ajmst.android.service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.ajmst.android.R;
import com.ajmst.android.entity.MsgQueue;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import java.sql.SQLException;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final int DATABASE_VERSION = 15;
    public static final String DATABASE_NAME = "AJMST.db";
    private List<Class> tableClasses;

    public DatabaseHelper(Context context, List<Class> tableClasses) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, DATABASE_VERSION, R.raw.ormlite_config);
        this.tableClasses = tableClasses;
    }

    @Override // com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onCreate");
            for (Class c : this.tableClasses) {
                TableUtils.createTable(connectionSource, c);
            }
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    @Override // com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onUpgrade");
            TableUtils.createTableIfNotExists(connectionSource, MsgQueue.class);
            db.execSQL("ALTER table advspkfk ADD isBarcodeUpdate INTEGER");
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "onUpgrade databases", e);
            throw new RuntimeException(e);
        }
    }

    @Override // com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper, android.database.sqlite.SQLiteOpenHelper, java.lang.AutoCloseable
    public void close() {
        super.close();
    }
}
