package com.musicplayah;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

public class MediaNotificationManager extends BroadcastReceiver {
    String TAG = Constants.TAG + "_NOTIFICATION";

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

    private Context context;

    private NotificationCompat.Builder builder;

    public MediaNotificationManager(MediaPlaybackService service, Context context) {
        this.service = service;
        this.context = context;

        updateSessionToken();

        notificationManager = NotificationManagerCompat.from(service);

        String pkg = service.getPackageName();

        pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        previousIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        nextIntent = PendingIntent.getBroadcast(service, REQUEST_CODE, new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);

        builder = new NotificationCompat.Builder(service, CHANNEL_ID);

        notificationManager.cancelAll();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startNotification() {
        if (!hasStarted) {
            Log.d(TAG, "Starting notification");
            metadata = mediaController.getMetadata();
            playbackState = mediaController.getPlaybackState();

            Notification notification = createNotification();

            if (notification != null) {
                Log.d(TAG, "Notification " + notification.toString());
                mediaController.registerCallback(controllerCallback);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_PLAY);
                filter.addAction(ACTION_PAUSE);
                filter.addAction(ACTION_NEXT);
                filter.addAction(ACTION_PREV);
                service.registerReceiver(this, filter);

                service.startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "Service started");

                hasStarted = true;
            }
        }
    }

    public void stopNotification() {
        Log.d(TAG, "stopNotification");
        if (hasStarted) {
            hasStarted = false;
            mediaController.unregisterCallback(controllerCallback);
            try {
                notificationManager.cancel(NOTIFICATION_ID);
                service.unregisterReceiver(this);
            } catch (IllegalArgumentException e) {
                // Ignored
            }
            service.stopForeground(true);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received notificationManager");
        final String action = intent.getAction();

        switch (action) {
            case ACTION_PAUSE:
                transportControls.pause();
                break;
            case ACTION_PLAY:
                transportControls.play();
                break;
            case ACTION_NEXT:
                transportControls.skipToNext();
                break;
            case ACTION_PREV:
                transportControls.skipToPrevious();
                break;
            default:
                Log.d(TAG, "Notification Manager received an invalid action");
        }
    }

    private void updateSessionToken() {
        Log.d(TAG, "NotificationManager updateSessionToken");

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

    private PendingIntent createContentIntent(MediaDescriptionCompat description) {
        Intent resultIntent = new Intent(service, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        resultIntent.putExtra(MainActivity.START_FULLSCREEN, true);
        if (description != null) resultIntent.putExtra(MainActivity.MEDIA_DESCRIPTION, description);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(service);
        stackBuilder.addNextIntentWithParentStack(resultIntent);

        return stackBuilder.getPendingIntent(REQUEST_CODE, PendingIntent.FLAG_UPDATE_CURRENT);
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        return pendingIntent;
    }

    private final MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            Log.d(TAG, "Session destroyed");
            updateSessionToken();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            playbackState = state;
            Log.d(TAG, "Notification new state " + state);
            if (state.getState() == PlaybackStateCompat.STATE_STOPPED || state.getState() == PlaybackStateCompat.STATE_NONE) stopNotification();
            else {
                Notification notification = updateNotification();
                if (notification != null) notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onMetadataChanged(MediaMetadataCompat newMetadata) {
            metadata = newMetadata;
            Log.d(TAG, "Updating metadata " + newMetadata);

            Notification notification = updateNotification();
            if (notification != null) notificationManager.notify(NOTIFICATION_ID, notification);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification createNotification() {
        Log.d(TAG, "Creating notification...");

        if (metadata == null || playbackState == null) {
            Log.d(TAG, "THERE IS NO METADATA!!!!");
            return null;
        }

        NotificationChannel chan = new NotificationChannel(CHANNEL_ID, "MusicPlayahChannel", NotificationManager.IMPORTANCE_LOW);

        chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        int toggleButtonPosition = 0;

        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            builder.addAction(R.drawable.ic_skip_previous_white_24dp, service.getString(R.string.label_previous), previousIntent);
            toggleButtonPosition = 1;
        }

        addPlayPauseAction(builder);

        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) builder.addAction(R.drawable.ic_skip_next_white_24dp, service.getString(R.string.label_next), nextIntent);

        MediaDescriptionCompat description = metadata.getDescription();

        setNotificationPlaybackState(builder);

        Notification notification = builder
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(toggleButtonPosition)
                        .setMediaSession(sessionToken))
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(false)
                .setShowWhen(false)
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);

        return notification;
    }

    @SuppressLint("RestrictedApi")
    NotificationCompat.Extender clearActionsExtender = builder -> {
        builder.mActions.clear(); // Seems that there is not a better approach...
        return builder;
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification updateNotification() {
        Log.d(TAG, "Updating notification..." + builder);

        if (metadata == null || playbackState == null) {
            Log.d(TAG, "THERE IS NO METADATA!!!!");
            return null;
        }

        builder.extend(clearActionsExtender); // clear actions to recreate them

        int toggleButtonPosition = 0;

        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            builder.addAction(R.drawable.ic_skip_previous_white_24dp, service.getString(R.string.label_previous), previousIntent);
            toggleButtonPosition = 1;
        }

        addPlayPauseAction(builder);

        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) builder.addAction(R.drawable.ic_skip_next_white_24dp, service.getString(R.string.label_next), nextIntent);

        MediaDescriptionCompat description = metadata.getDescription();

        setNotificationPlaybackState(builder);

        Notification notification = builder
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(toggleButtonPosition))
                .setContentIntent(createContentIntent(description))
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);

        return notification;
    }

    private void addPlayPauseAction(NotificationCompat.Builder builder) {
        Log.d(TAG, "Adding play pause action in notification");
        String label;
        int icon;
        PendingIntent intent;

        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            icon = R.drawable.uamp_ic_pause_white_24dp;
            label = service.getString(R.string.label_pause);
            intent = pauseIntent;
        } else {
            icon = R.drawable.uamp_ic_play_arrow_white_24dp;
            label = service.getString(R.string.label_play);
            intent = playIntent;
        }
        builder.addAction(new NotificationCompat.Action(icon, label, intent));
    }

    private void setNotificationPlaybackState(NotificationCompat.Builder builder) {
        Log.d(TAG, "NotificationManager setNotificationPlaybackState");

//        if (playbackState == null || !hasStarted) {
//            Log.d(TAG, "NotificationManager Stopping..." + builder);
//            Log.d(TAG, "playbackState => " + playbackState + " hasStarted => " + hasStarted);
//            service.stopForeground(true);
//            return;
//        }

        builder.setOngoing(playbackState.getState() == PlaybackStateCompat.STATE_PLAYING);
    }
}
