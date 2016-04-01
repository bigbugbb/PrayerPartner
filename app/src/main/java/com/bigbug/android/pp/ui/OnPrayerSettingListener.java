package com.bigbug.android.pp.ui;

import com.bigbug.android.pp.data.model.Prayer;

interface OnPrayerSettingListener {
    void onCreatePrayer(Prayer prayer);
    void onUpdatePrayer(Prayer prayer);
    void onDeletePrayer(Prayer prayer);
}