package com.musicplayah;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;

public class ExoPlayback {

    private SimpleExoPlayer exoPlayer;
    private Player.EventListener eventListener; // TODO: create eventListener

    private Context context;

    public ExoPlayback(Context context) {
        this.context = context;
    }

    public void play(MediaBrowserCompat.MediaItem item) {
        if (exoPlayer == null) {
            exoPlayer = new SimpleExoPlayer.Builder(context).build();
            exoPlayer.addListener(eventListener);
        }
    }

    public void pause() {

    }
}
