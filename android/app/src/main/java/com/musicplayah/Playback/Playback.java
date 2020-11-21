package com.musicplayah.Playback;

import android.support.v4.media.session.MediaSessionCompat;

import com.google.android.exoplayer2.MediaItem;

public interface Playback {

    void stop();

    int getState();

    boolean isConnected();

    boolean isPlaying();

    long getCurrentPosition();

//    void updateLastKnownStreamPosition();

    void play(MediaSessionCompat.QueueItem item);

    void pause();

//    void seekTo(long position);
//
    MediaItem getCurrentPlaying();

    interface Callback {

        void onCompletion();

        void onPlaybackStatusChanged(int state);

        void onError(String error);

        void setCurrentMediaId(String mediaId);
    }

    void setCallback(Callback callback);
}
