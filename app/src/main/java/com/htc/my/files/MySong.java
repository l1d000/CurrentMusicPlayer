package com.htc.my.files;

/**
 * Created by lidongzhou on 18-2-13.
 */

import android.os.Parcel;
import android.os.Parcelable;

public class MySong implements Parcelable{
    private Long id;             // 音乐id

    private String title;       // 标题

    private String author;      // 作者

    private String music_path;  // 路径

    private Long duration;       // 持续时间

    private Long size;           // 大小

    private String displayName;
    private boolean favorite;
    private String album;

    public MySong() {
        // Empty
    }

    public MySong(Parcel in) {
        readFromParcel(in);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getMusicPath() { return music_path;}

    public void setMusicPath(String music_path) {
        this.music_path = music_path;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.title);
        dest.writeString(this.author);
        dest.writeString(this.music_path);
        dest.writeLong(this.duration);
        dest.writeLong(this.size);

//        dest.writeInt(this.favorite ? 1 : 0);
//        dest.writeString(this.album);
//        dest.writeString(this.displayName);
    }

    public void readFromParcel(Parcel in) {
        this.id = in.readLong();
        this.title = in.readString();

        this.author = in.readString();

        this.music_path = in.readString();
        this.duration = in.readLong();
        this.size = in.readLong();
//        this.album = in.readString();
//        this.displayName = in.readString();
//        this.favorite = in.readInt() == 1;
    }

    public static final Parcelable.Creator<MySong> CREATOR = new Parcelable.Creator<MySong>() {
        @Override
        public MySong createFromParcel(Parcel source) {
            return new MySong(source);
        }

        @Override
        public MySong[] newArray(int size) {
            return new MySong[size];
        }
    };
}
