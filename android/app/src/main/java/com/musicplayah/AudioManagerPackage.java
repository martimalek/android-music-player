package com.musicplayah;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.musicplayah.Utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudioManagerPackage implements ReactPackage  {
    @NonNull
    @Override
    public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }

    @NonNull
    @Override
    public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new AudioManagerModule(reactContext));

        Log.d(Constants.TAG, "Adding modules! " + modules.size());
        return modules;
    }
}
