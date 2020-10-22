package com.musicplayah;

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
}
