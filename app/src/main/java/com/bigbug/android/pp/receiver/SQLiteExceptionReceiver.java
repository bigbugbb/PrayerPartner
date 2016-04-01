package com.bigbug.android.pp.receiver;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

public class SQLiteExceptionReceiver extends WakefulBroadcastReceiver {
    private final static String TAG = makeLogTag(SQLiteExceptionReceiver.class);

    public static final String ACTION_SQL_EXCEPTION_RECEIVED = "com.bigbug.android.pp.intent.action.SQL_EXCEPTION_RECEIVED";

    public SQLiteExceptionReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(ACTION_SQL_EXCEPTION_RECEIVED)) {
            LOGD(TAG, "Got ACTION_SQL_EXCEPTION_RECEIVED");
        }
    }
}
