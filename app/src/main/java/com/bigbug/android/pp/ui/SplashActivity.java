package com.bigbug.android.pp.ui;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.util.AccountUtils;
import com.bigbug.android.pp.util.PrefUtils;

import static com.bigbug.android.pp.util.LogUtils.LOGD;
import static com.bigbug.android.pp.util.LogUtils.LOGE;
import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

public class SplashActivity extends AppCompatActivity {
    private final static String TAG = makeLogTag(SplashActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        PrefUtils.init(getApplicationContext());

        AccountUtils.startAuthenticationFlow(
            this, // This must be the activity context to start the authenticate activity.
            AccountUtils.ACCOUNT_TYPE,
            AccountUtils.AUTHTOKEN_TYPE_FULL_ACCESS,
            new SimpleAccountManagerCallback(this)
        );
    }

    private static class SimpleAccountManagerCallback implements AccountManagerCallback<Bundle> {
        private Context mContext;

        public SimpleAccountManagerCallback(@NonNull Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                Bundle result = future.getResult();
                String authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                if (authToken != null) {
                    LOGD(TAG, authToken != null ? "SUCCESS!\nToken: " + authToken : "FAIL");
                    Intent intent = new Intent();
                    intent.setClass(mContext, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    mContext.startActivity(intent);
                }
                LOGD(TAG, "GetTokenForAccount Bundle is " + result);
            } catch (Exception e) {
                LOGE(TAG, "Error: " + e.getMessage());
            }
        }
    }
}
