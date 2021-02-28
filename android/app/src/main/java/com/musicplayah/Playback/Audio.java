package com.musicplayah.Playback;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

public class Audio {
    public int id;
    public String artist;
    public String title;
    public String data;
    public String displayName;
    public int duration;

    Audio(int id, String artist, String title, String data, String displayName, int duration) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.data = data;
        this.displayName = displayName;
        this.duration = duration;
    }

    public WritableMap toMap() {
        WritableMap map = new WritableNativeMap();

        map.putInt("id", this.id);
        map.putString("artist", this.artist);
        map.putString("title", this.title);
        map.putString("data", this.data);
        map.putString("displayName", this.displayName);
        map.putInt("duration", this.duration);

        return map;
    }
}
