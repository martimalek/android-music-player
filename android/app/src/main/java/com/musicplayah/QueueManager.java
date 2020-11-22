package com.musicplayah;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueueManager {
    private static final String TAG = Constants.TAG;

    private MusicProvider musicProvider;
    private Context context;
    private Resources resources;
    private List<MediaSessionCompat.QueueItem> playingQueue;
    private MetadataUpdateListener listener;

    private MediaSessionCompat.QueueItem currentPlayingItem;

    public QueueManager(
            MusicProvider musicProvider,
            Resources resources,
            Context context,
            MetadataUpdateListener listener
    ) {
        this.musicProvider = musicProvider;
        this.resources = resources;
        this.context = context;
        this.listener = listener;

        playingQueue = Collections.synchronizedList(new ArrayList<MediaSessionCompat.QueueItem>());

        currentPlayingItem = null;
    }

    private List<MediaSessionCompat.QueueItem> getCurrentQueue() {
            return playingQueue;
    }

    private List<MediaSessionCompat.QueueItem> getRandomQueue(MusicProvider musicProvider, int numberOfSongs) {
        List<MediaMetadataCompat> result = new ArrayList<>(numberOfSongs);

        MediaMetadataCompat randomlyChosenTrack;
        for (int i = 0 ; i < numberOfSongs ; i++) {
            randomlyChosenTrack = musicProvider.getRandomSongFromAllSongsOnDevice();
            result.add(randomlyChosenTrack);
            if (randomlyChosenTrack == null) {
                Log.d(TAG, "randomlyChosenTrack is NULL");
            } else {
                Log.d(TAG, "randomly chosen track: " + randomlyChosenTrack.getDescription().getTitle());
            }
        }

        Log.d(TAG, "getRandomQueue: result.size=" + result.size());

        return convertToQueue(result);
    }

    public void fillRandomQueue() {
        int currentQueueSize = playingQueue.size();
        Log.d(TAG, "fillRandomQueue, current size = " + playingQueue.size());

        if (currentQueueSize < 10) // TODO: Should this value come from somewhere ??
        {
            List<MediaSessionCompat.QueueItem> newTracks = getRandomQueue(musicProvider, 10); // Same ?

            Log.d(TAG, "Adding " +  newTracks.size() + " new songs to the queue");
            playingQueue.addAll(newTracks);
        }

        if (currentPlayingItem == null) currentPlayingItem = playingQueue.get(0);

        listener.onQueueUpdated("AlbumTitle", playingQueue);
    }

    public static int count = 0;

    private static List<MediaSessionCompat.QueueItem> convertToQueue(Iterable<MediaMetadataCompat> tracks) {
        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();

        for (MediaMetadataCompat track : tracks) {
            if (track != null) {
                MediaMetadataCompat trackCopy = new MediaMetadataCompat.Builder(track)
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.getDescription().getMediaId())
                        .build();

                MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(trackCopy.getDescription(), count++);
                queue.add(item);
            }
        }
        return queue;
    }

    public MediaSessionCompat.QueueItem getCurrentMusic() {
        return currentPlayingItem;
    }

    public interface MetadataUpdateListener {
        void onMetadataChanged(MediaMetadataCompat metadata);
        void onMetadataRetrieveError();
        void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> newQueue);
        void onNowPlayingChanged(MediaSessionCompat.QueueItem nowPlaying);
        void onPauseRequest();
    }
}
