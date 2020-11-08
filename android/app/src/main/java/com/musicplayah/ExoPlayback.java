package com.musicplayah;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_URI;

public class ExoPlayback {
    private String TAG = Constants.TAG;

    private SimpleExoPlayer exoPlayer;
    private ExoPlayerEventListener eventListener = new ExoPlayerEventListener();
    private MusicProvider musicProvider;

    private String currentMediaId;

    private boolean isExoPlayerStopped = false;
    private boolean isNoisyReceiverRegistered = false;

    private final IntentFilter audioNoisyIntentFIlter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private BecomingNoisyReceiver becomingNoisyReceiver = new BecomingNoisyReceiver(
            new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "becomingNoisyReceiver!");
                    Intent i = new Intent(context, MediaPlaybackService.class);
                    i.setAction(MediaPlaybackService.ACTION_CMD);
                    i.putExtra(MediaPlaybackService.CMD_NAME, MediaPlaybackService.CMD_PAUSE); // See onStartCommand
                    context.startService(i);
                }
            }
    );

    private Context context;

    public ExoPlayback(Context context, MusicProvider musicProvider) {
        Log.d(TAG, "Inside ExoPlayback constructor");
        this.context = context;
        this.musicProvider = musicProvider;
    }

    public void play(MediaItem item) {
        Log.d(TAG, "Exoplayer play!");

        registerNoisyReceiver();
        boolean hasMediaChanged = !item.mediaId.equals(currentMediaId);
        if (hasMediaChanged) currentMediaId = item.mediaId;

        Log.d(TAG, "Has media changed ? " + hasMediaChanged);

        if (hasMediaChanged || exoPlayer == null) {
            releaseResources(false);
            if (exoPlayer == null) {
                exoPlayer = new SimpleExoPlayer.Builder(context).build();
                exoPlayer.addListener(eventListener);
            }
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .build();

            exoPlayer.setAudioAttributes(audioAttributes);

            MediaItem mediaItem = musicProvider.getTrackById(item.mediaId);

            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "musicplayah"), null);
            ProgressiveMediaSource.Factory mediaFactory = new ProgressiveMediaSource.Factory(dataSourceFactory, new DefaultExtractorsFactory());

            exoPlayer.setMediaSource(mediaFactory.createMediaSource(mediaItem));
            exoPlayer.prepare();
        }

        exoPlayer.play();
    }

    public void pause() {
        Log.d(TAG, "Pausing...");
        if (exoPlayer != null) exoPlayer.setPlayWhenReady(false);

        releaseResources(false);
        unregisterNoisyReceiver();
    }

    public void stop() {
        unregisterNoisyReceiver();
        releaseResources(true);
    }

    public MediaItem getCurrentPlaying() {
        Log.d(TAG, "Geztting current item playing");

        if (exoPlayer != null){ return exoPlayer.getCurrentMediaItem();}
        return null;
    }

    private void releaseResources(boolean releasePlayer) {
        if (releasePlayer && exoPlayer != null) {
            exoPlayer.release();
            exoPlayer.removeListener(eventListener);
            exoPlayer = null;
            isExoPlayerStopped = true;
        }
    }

    private void registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            context.registerReceiver(becomingNoisyReceiver, audioNoisyIntentFIlter);
            isNoisyReceiverRegistered = true;
        }
    }

    private void unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            context.unregisterReceiver(becomingNoisyReceiver);
            isNoisyReceiverRegistered = false;
        }
    }

    public int getState() {
        Log.d(TAG, "getState");
        if (exoPlayer == null) {
            return isExoPlayerStopped
                    ? PlaybackStateCompat.STATE_STOPPED
                    : PlaybackStateCompat.STATE_NONE;
        }
        Log.d(TAG, "State => " + exoPlayer.getPlaybackState());
        switch (exoPlayer.getPlaybackState()) {
            case ExoPlayer.STATE_IDLE:
            case ExoPlayer.STATE_ENDED:
                return PlaybackStateCompat.STATE_PAUSED;
            case ExoPlayer.STATE_BUFFERING:
                return PlaybackStateCompat.STATE_BUFFERING;
            case ExoPlayer.STATE_READY:
                return exoPlayer.getPlayWhenReady()
                        ? PlaybackStateCompat.STATE_PLAYING
                        : PlaybackStateCompat.STATE_PAUSED;
            default:
                return PlaybackStateCompat.STATE_NONE;
        }
    }

    public boolean isPlaying() {
        if (exoPlayer != null){ return exoPlayer.isPlaying();}
        return false;
    }

    private final class ExoPlayerEventListener implements ExoPlayer.EventListener {
        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.d(TAG, "Error on exoPlayer " + error.getMessage());
        }
    }
}
