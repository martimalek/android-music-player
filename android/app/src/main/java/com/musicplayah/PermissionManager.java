package com.musicplayah;

import android.content.pm.PackageManager;

import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.PermissionListener;

import static com.musicplayah.Constants.PERMS_REQUEST_CODE;

public class PermissionManager implements PermissionListener {

    public Promise permsGrantedPromise;

    public PermissionManager() {
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMS_REQUEST_CODE) {
            permsGrantedPromise.resolve(true);
            return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        permsGrantedPromise.reject("E_PERMS", "Permission not accepted");
        return false;
    }
}
