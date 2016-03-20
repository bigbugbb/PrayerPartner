package com.bigbug.android.pp.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.data.model.Prayer;
import com.bigbug.android.pp.provider.AppContract;

import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

public class PrayerSettingDialog extends DialogFragment {
    public static final String TAG = makeLogTag(PrayerSettingDialog.class);

    private EditText mName;
    private EditText mEmail;
    private ImageView mPhoto;
    private LinearLayout mNameEmailSetting;

    public PrayerSettingDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.prayer_setting_dialog_title)
                .setView(getCustomView(getActivity()))
                .setPositiveButton("Done",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do the data validation
                                String name  = mName.getText().toString();
                                String email = (String) mPhoto.getTag();
                                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
                                    TextView errorView = new TextView(getActivity());
                                    errorView.setText(R.string.prayer_dialog_error_data);
                                    errorView.setTextColor(Color.RED);
                                    errorView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Small);
                                    errorView.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT));
                                    mNameEmailSetting.addView(errorView);
                                    return;
                                }

                                // Insert data into prayers table so the prayers observer can be triggered
                                ContentValues values = new ContentValues();
                                values.put(AppContract.Prayers.NAME, mName.getText().toString());
                                values.put(AppContract.Prayers.EMAIL, mEmail.getText().toString());
                                values.put(AppContract.Prayers.PHOTO, (String) mPhoto.getTag());
                                values.put(AppContract.SyncColumns.UPDATED, System.currentTimeMillis());
                                getContentResolver().insert(AppContract.Prayers.CONTENT_URI, values);
                            }
                        }
                )
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }
                )
                .create();
    }

    public View getCustomView(Context context) {
        View container = LayoutInflater.from(context).inflate(R.layout.fragment_prayer_setting, null);
        mName = (EditText) container.findViewById(R.id.name_setting);
        mEmail = (EditText) container.findViewById(R.id.email_setting);
        mPhoto = (ImageView) container.findViewById(R.id.photo_setting);
        mNameEmailSetting = (LinearLayout) container.findViewById(R.id.name_email_setting);

        mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        return container;
    }

    private ContentResolver getContentResolver() {
        return getActivity().getContentResolver();
    }
}