package com.musicplayah;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Build;
import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.IllegalViewOperationException;

import java.util.Map;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

public class NavbarColorModule extends ReactContextBaseJavaModule {
    public static final String REACT_CLASS = "NavbarColor";
    private static final String ERROR_NO_ACTIVITY = "E_NO_ACTIVITY";
    private static final String ERROR_NO_ACTIVITY_MESSAGE = "Tried to change the navigation bar while not attached to an Activity";
    private static final String ERROR_API_LEVEL = "API_LEVEl";
    private static final String ERROR_API_LEVEL_MESSAGE = "Only Android Oreo and above is supported";

    public NavbarColorModule(ReactApplicationContext context) {
        super(context);
    }

    public void setNavigationBarTheme(Activity activity, Boolean light) {
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Window window = activity.getWindow();
            int flags = window.getDecorView().getSystemUiVisibility();
            if (light) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        return super.getConstants();
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactMethod
    public void changeNavBarColor(final String color, final Boolean light, final Promise promise) {
        if (getCurrentActivity() != null) {
            try {
                final Window window = getCurrentActivity().getWindow();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                        Integer colorFrom = window.getNavigationBarColor();
                        Integer colorTo = Color.parseColor(String.valueOf(color));

                        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animator) {
                                window.setNavigationBarColor((Integer) animator.getAnimatedValue());
                                window.setStatusBarColor((Integer) animator.getAnimatedValue());
                            }
                        });
                        colorAnimation.start();
                        setNavigationBarTheme(getCurrentActivity(), light);
                        promise.resolve(true);
                    }
                });
            } catch (IllegalViewOperationException e) {
                promise.reject("error", e);
            }
        } else promise.reject(ERROR_NO_ACTIVITY, new Throwable(ERROR_NO_ACTIVITY_MESSAGE));
    }
}