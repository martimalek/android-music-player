package com.musicplayah;

import android.content.pm.PackageManager;

import com.facebook.react.modules.core.PermissionListener;
import com.musicplayah.Utils.ObservableBoolean;

import static com.musicplayah.Utils.Constants.PERMISSION_OBSERVER_KEY;
import static com.musicplayah.Utils.Constants.PERMS_REQUEST_CODE;

public class PermissionManager implements PermissionListener {

    public ObservableBoolean isPermissionGranted;

    public PermissionManager() {
        isPermissionGranted = new ObservableBoolean(PERMISSION_OBSERVER_KEY, false);
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMS_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isPermissionGranted.setValue(true);
            return true;
        }
        isPermissionGranted.setValue(false);
        return false;
    }
}
