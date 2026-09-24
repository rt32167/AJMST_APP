package com.ajmst.android.service;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/** Read-only access to files deliberately placed in the app's shared cache. */
public final class SharedFileProvider extends ContentProvider {
    public static File sharedFile(Context context, String name) throws IOException {
        if (name == null || name.contains("/") || name.contains("\\") || name.equals("..")) {
            throw new IOException("Invalid shared file name");
        }
        File directory = new File(context.getCacheDir(), "shared");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Cannot create shared cache");
        return new File(directory, name);
    }

    public static Uri uriFor(Context context, File file) {
        return new Uri.Builder().scheme("content")
                .authority(context.getPackageName() + ".files")
                .appendPath(file.getName()).build();
    }

    private File fileFor(Uri uri) throws FileNotFoundException {
        try {
            if (!uri.getAuthority().equals(getContext().getPackageName() + ".files")) {
                throw new FileNotFoundException("Invalid authority");
            }
            File file = sharedFile(getContext(), uri.getLastPathSegment());
            if (!file.isFile()) throw new FileNotFoundException(uri.toString());
            return file;
        } catch (IOException | NullPointerException e) {
            throw new FileNotFoundException(uri.toString());
        }
    }

    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name != null && name.endsWith(".png")) return "image/png";
        if (name != null && name.endsWith(".csv")) return "text/csv";
        return "application/octet-stream";
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) {
        try {
            File file = fileFor(uri);
            String[] columns = projection == null
                    ? new String[] { OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE } : projection;
            MatrixCursor result = new MatrixCursor(columns, 1);
            Object[] values = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i] = file.getName();
                if (OpenableColumns.SIZE.equals(columns[i])) values[i] = file.length();
            }
            result.addRow(values);
            return result;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection,
                                String[] selectionArgs) { throw new UnsupportedOperationException(); }
}
