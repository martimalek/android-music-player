package com.musicplayah;

import android.os.Bundle;
import com.facebook.react.ReactActivity;

import com.zoontek.rnbootsplash.RNBootSplash;

public class MainActivity extends ReactActivity {

  public static final String START_FULLSCREEN = "com.musicplayah.START_FULLSCREEN";
  public static final String MEDIA_DESCRIPTION = "com.musicplayah.MEDIA_DESCRIPTION";
  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "MusicPlayah";
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    RNBootSplash.init(R.drawable.bootsplash, MainActivity.this); // <- display the generated bootsplash.xml drawable over our MainActivity
  }
}
