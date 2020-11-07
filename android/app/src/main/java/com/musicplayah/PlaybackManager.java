package com.musicplayah;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.io.IOException;


public class PlaybackManager {
    private static String TAG = Constants.TAG;

    private MediaSessionCallback mediaSessionCallback;
    private MediaPlaybackService playbackService;

    private MediaPlayer mediaPlayer;

    private Context context;

    private ExoPlayback exoPlayback;

    PlaybackManager(MediaPlaybackService playbackService, Context context) {
        Log.d(TAG, "Inside PlaybackManager constructor!");
        mediaSessionCallback = new MediaSessionCallback();
        this.playbackService = playbackService;
        this.context = context;
        this.exoPlayback = new ExoPlayback(context);
    }

    public void handlePlayRequest() {
        Log.d(TAG, "Handling Play request!"); // TODO

        if (mediaPlayer == null) initMediaPlayer();

        MediaBrowserCompat.MediaItem item = playbackService.tracks.get(0);

        if (item != null) {
            Uri audioUri = item.getDescription().getMediaUri();
            if (audioUri != null) {
                Log.d(TAG, "There is an item!");
                Log.d(TAG, "Item => " + audioUri.toString());

                playbackService.onPlaybackStart();
                exoPlayback.play(item);
            }
        } else Log.d(TAG, "No items found... T.T");

    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
    }

    private void startMediaPlayer(Uri audioUri) {
        try {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            mediaPlayer.setDataSource(context, audioUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.d(TAG, "Exception while preparing MediaPlayer " + e.getMessage());
        }
    }

    public void handlePauseRequest() {
        Log.d(TAG, "Handling Pause request!"); // TODO
    }

    public void handleStopRequest() {
        Log.d(TAG, "Handling Pause request!"); // TODO
    }

    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mediaSessionCallback;
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            handleStopRequest();
        }
    }

    public interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);
    }
}
