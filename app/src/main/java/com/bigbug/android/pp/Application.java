package com.bigbug.android.pp;

import com.bigbug.android.pp.util.LogUtils;
import com.localytics.android.Localytics;
import com.localytics.android.LocalyticsActivityLifecycleCallbacks;

public class Application extends android.app.Application {
    private final static String TAG = LogUtils.makeLogTag(Application.class);

    @Override
    public void onCreate() {
        super.onCreate();

        Localytics.setLoggingEnabled(true);
        registerActivityLifecycleCallbacks(new LocalyticsActivityLifecycleCallbacks(this));
    }
}
