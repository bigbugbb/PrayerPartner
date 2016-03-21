package com.bigbug.android.pp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.provider.AppContract;

import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

public class PrayerSettingDialog extends DialogFragment {
    public static final String TAG = makeLogTag(PrayerSettingDialog.class);

    private EditText mName;
    private EditText mEmail;
    private ImageView mPhoto;

    public final static int REQUEST_PRAYER_PHOTO = 0;
    public final static String PRAYER_PHOTO_URI = "prayer_photo_uri";

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
                                String email = mEmail.getText().toString();
                                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
                                    final String errorMsg = getString(R.string.prayer_dialog_error_data);
                                    Toast.makeText(getActivity(), errorMsg, Toast.LENGTH_LONG).show();
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
        mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PhotoActivity.class);
                startActivityForResult(intent, REQUEST_PRAYER_PHOTO);
            }
        });
        return container;
    }

    private ContentResolver getContentResolver() {
        return getActivity().getContentResolver();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PRAYER_PHOTO && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra(PRAYER_PHOTO_URI);
            mPhoto.setImageURI(uri);
            mPhoto.setTag(uri.getPath());
        }
    }
}