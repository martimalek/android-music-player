package com.musicplayah;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MediaNotificationManager extends BroadcastReceiver {
    String TAG = Constants.TAG;

    private static final int NOTIFICATION_ID = 412;
    private static final int REQUEST_CODE = 100;
    private static final String CHANNEL_ID = "MusicplayahChannel";

    public static final String ACTION_PAUSE = "com.musicplayah.pause";
    public static final String ACTION_PLAY = "com.musicplayah.play";
    public static final String ACTION_PREV = "com.musicplayah.prev";
    public static final String ACTION_NEXT = "com.musicplayah.next";

    private MediaPlaybackService service;
    private MediaSessionCompat.Token sessionToken;
    private MediaControllerCompat mediaController;
    private MediaControllerCompat.TransportControls transportControls;

    private PlaybackStateCompat playbackState;
    private MediaMetadataCompat metadata;

    private NotificationManagerCompat notificationManager;

    private final PendingIntent pauseIntent;
    private final PendingIntent playIntent;
    private final PendingIntent previousIntent;
    private final PendingIntent nextIntent;

    private boolean hasStarted = false;

    public MediaNotificationManager(MediaPlaybackService service) {
        this.service = service;
        updateSessionToken();

        notificationManager = NotificationManagerCompat.from(service);

        String pkg = service.getPackageName();

        pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        previousIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        nextIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        notificationManager.cancelAll();
    }

    public void startNotification() {
        if (!hasStarted) {
            metadata = mediaController.getMetadata();
            playbackState = mediaController.getPlaybackState();

            Notification notification = createNotification();
            if (notification != null) {
                mediaController.registerCallback(controllerCallback);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PREV);
                service.registerReceiver(this, filter);

                service.startForeground(NOTIFICATION_ID, notification);
                hasStarted = true;
            }
        }
    }

    // TODO: stopNotificaton

    @Override
    public void onReceive(Context context, Intent intent) {

    }

    private void updateSessionToken() {
        MediaSessionCompat.Token token = service.getSessionToken();
        if (sessionToken == null && token != null || sessionToken != null && !sessionToken.equals(token)) {
            if (mediaController != null) mediaController.unregisterCallback(controllerCallback);
            sessionToken = token;
            if (sessionToken != null) {
                mediaController = new MediaControllerCompat(service, sessionToken);
                transportControls = mediaController.getTransportControls();
                if (hasStarted) mediaController.registerCallback(controllerCallback);
            }
        }
    }

    private final MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
        }
    };

    private Notification createNotification() {
        Log.d(TAG, "Creating notification...");

        if (metadata == null || playbackState == null) return null;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, CHANNEL_ID);
        int toggleButtonPosition = 0;

        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            builder.addAction(R.drawable.ic_skip_previous_white_24dp, service.getString(R.string.label_previous), previousIntent);
            toggleButtonPosition = 1;
        }

//        addPlayPauseAction(builder); // TODO

        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) builder.addAction(R.drawable.ic_skip_next_white_24dp, service.getString(R.string.label_next), nextIntent);

        MediaDescriptionCompat description = metadata.getDescription();

        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(new int[]{toggleButtonPosition})
                .setMediaSession(sessionToken))
            .setSmallIcon(R.drawable.ic_notification)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(true)
            .setContentIntent(createContentIntent(description))
            .setContentTitle(description.getTitle())
            .setContentText(description.getSubtitle());

        setNotificationPlaybackState(builder);

        return builder.build();
    }

    private PendingIntent createContentIntent(MediaDescriptionCompat description) {
        Intent openUI = new Intent(service, MainActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        openUI.putExtra(MainActivity.EXTRA_START_FULLSCREEN, true);
//        if (description != null)  openUI.putExtra(MediaBrowserUampActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION, description);
        return PendingIntent.getActivity(service, REQUEST_CODE, openUI, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        if (playbackState == null || !hasStarted) {
            service.stopForeground(true);
            return;
        }
        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING && playbackState.getPosition() >= 0) {
            builder.setWhen(System.currentTimeMillis() - playbackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
        } else {
            builder.setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        builder.setOngoing(playbackState.getState() == PlaybackStateCompat.STATE_PLAYING);
    }
}
