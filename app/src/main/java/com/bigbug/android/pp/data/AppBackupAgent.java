package com.bigbug.android.pp.data;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackupDataOutput;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

public class AppBackupAgent extends BackupAgent {

    private final static String TAG = makeLogTag(AppBackupAgent.class);

    @Override
    public void onCreate() {
        super.onCreate();
        LOGD(TAG, "onCreate");
    }

    @Override
    public void onFullBackup(FullBackupDataOutput data) throws IOException {
        super.onFullBackup(data);
        LOGD(TAG, "onFullBackup");
    }

    @Override
    public void onRestoreFinished() {
        super.onRestoreFinished();
        LOGD(TAG, "onRestoreFinished");
    }

    @Override
    public void onRestoreFile(ParcelFileDescriptor data, long size, File destination, int type, long mode, long mtime) throws IOException {
        super.onRestoreFile(data, size, destination, type, mode, mtime);
        LOGD(TAG, "onRestoreFile");
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        LOGD(TAG, "onBackup");
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        LOGD(TAG, "onRestore");
    }
}
