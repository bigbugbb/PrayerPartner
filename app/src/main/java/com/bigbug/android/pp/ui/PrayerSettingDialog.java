package com.bigbug.android.pp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.bigbug.android.pp.R;
import com.bigbug.android.pp.data.model.Prayer;

import java.io.File;

import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

public class PrayerSettingDialog extends DialogFragment {
    public static final String TAG = makeLogTag(PrayerSettingDialog.class);

    private EditText  mName;
    private EditText  mEmail;
    private ImageView mPhoto;

    private Prayer mPrayer;

    private OnPrayerSettingListener mListener;

    public final static int REQUEST_PRAYER_PHOTO = 0;
    public final static String PRAYER_PHOTO_URI = "prayer_photo_uri";

    public PrayerSettingDialog() {
    }

    public void setPrayer(Prayer prayer) {
        mPrayer = prayer;
    }

    public void setListener(OnPrayerSettingListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.prayer_setting_dialog_title)
                .setView(getCustomView(getActivity()));

        if (mPrayer == null) {
            return buildForCreateMode(builder).create();
        } else {
            return buildForUpdateMode(builder).create();
        }
    }

    protected AlertDialog.Builder buildForCreateMode(AlertDialog.Builder builder) {
        return builder
            .setPositiveButton("Add",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Prayer prayer = new Prayer();
                            prayer.name  = mName.getText().toString();
                            prayer.email = mEmail.getText().toString();
                            prayer.photo = (String) mPhoto.getTag();

                            if (mListener != null) {
                                mListener.onCreatePrayer(prayer);
                            }
                        }
                    }
            )
            .setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                        }
                    }
            );
    }

    protected AlertDialog.Builder buildForUpdateMode(AlertDialog.Builder builder) {
        return builder
            .setPositiveButton("Update",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Prayer prayer = new Prayer();
                            prayer.id    = mPrayer.id;
                            prayer.name  = mName.getText().toString();
                            prayer.email = mEmail.getText().toString();
                            prayer.photo = (String) mPhoto.getTag();

                            if (mListener != null) {
                                mListener.onUpdatePrayer(prayer);
                            }
                        }
                    }
            )
            .setNeutralButton("Delete",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (mListener != null) {
                                mListener.onDeletePrayer(mPrayer);
                            }
                        }
                    }
            )
            .setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                        }
                    }
            );
    }

    private View getCustomView(Context context) {
        View container = LayoutInflater.from(context).inflate(R.layout.fragment_prayer_setting, null);
        mName  = (EditText) container.findViewById(R.id.name_setting);
        mEmail = (EditText) container.findViewById(R.id.email_setting);
        mPhoto = (ImageView) container.findViewById(R.id.photo_setting);
        mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PhotoActivity.class);
                startActivityForResult(intent, REQUEST_PRAYER_PHOTO);
            }
        });

        if (mPrayer != null) {
            Uri uri = Uri.fromFile(new File(mPrayer.photo));
            mName.setText(mPrayer.name);
            mEmail.setText(mPrayer.email);
            mPhoto.setImageURI(uri);
            mPhoto.setTag(mPrayer.photo);
        }

        return container;
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