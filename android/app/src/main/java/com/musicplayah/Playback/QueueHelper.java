package com.musicplayah.Playback;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.ArrayList;
import java.util.List;

public class QueueHelper {
    public static int getItemIndexOnQueue(Iterable<MediaSessionCompat.QueueItem> queue, long queueId) {
        int index = 0;
        for (MediaSessionCompat.QueueItem item : queue) {
            if (queueId == item.getQueueId()) return index;
            index++;
        }
        return -1;
    }

    public static boolean isItemAlreadyOnQueue(Iterable<MediaSessionCompat.QueueItem> queue, long queueId) {
        for (MediaSessionCompat.QueueItem item : queue) {
            if (queueId == item.getQueueId()) return true;
        }
        return false;
    }

    private static int count = 0;

    public static List<MediaSessionCompat.QueueItem> convertToQueue(Iterable<MediaMetadataCompat> tracks) {
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
}
