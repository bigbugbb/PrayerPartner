package com.bigbug.android.pp.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.widget.Toast;

import com.bigbug.android.pp.R;

/**
 * Created by bbo on 3/20/16.
 */
public class PhotoActivity extends BaseActivity {

    protected static final int REQUEST_EXTRA_PERMISSIONS = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        PhotoFragment fragment = (PhotoFragment) getFragmentManager().findFragmentById(R.id.photo_fragment);
        if (fragment == null) {
            fragment = new PhotoFragment();
            getFragmentManager().beginTransaction()
                    .replace(R.id.photo_fragment, fragment)
                    .commit();
        }
    }

    @Override
    protected void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_EXTRA_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // It is possible that the permissions request interaction with the user is interrupted.
        // In this case you will receive empty permissions and results arrays which should be treated as a cancellation.
        if (permissions.length == 0) {
            requestPermissions();
            return;
        }

        if (requestCode == REQUEST_EXTRA_PERMISSIONS) {
            if (permissions[0].equals(Manifest.permission.CAMERA) &&
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "The app needs this permission to take a photo.", Toast.LENGTH_SHORT)
                        .show();
                finish();
            }
        }
    }
}
