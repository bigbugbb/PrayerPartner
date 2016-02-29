package com.bigbug.android.pp;

import com.bigbug.android.pp.util.LogUtils;

public class Application extends android.app.Application {
    private final static String TAG = LogUtils.makeLogTag(Application.class);

    @Override
    public void onCreate() {
        super.onCreate();
//        registerActivityLifecycleCallbacks(new LocalyticsActivityLifecycleCallbacks(this));
    }
}
