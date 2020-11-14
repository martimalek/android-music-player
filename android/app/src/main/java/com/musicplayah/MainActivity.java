package com.musicplayah;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;

import com.facebook.react.ReactActivity;

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
  public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
    super.onCreate(savedInstanceState, persistentState);

    setVolumeControlStream(AudioManager.STREAM_MUSIC);
  }
}
