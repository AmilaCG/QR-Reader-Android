package com.auroid.qrscanner.serializable;

import com.google.gson.annotations.SerializedName;

public class EventWrapper {

    @SerializedName("description")
    public String description;
    @SerializedName("location")
    public String location;
    @SerializedName("organizer")
    public String organizer;
    @SerializedName("status")
    public String status;
    @SerializedName("summary")
    public String summary;
    @SerializedName("start")
    public String start;
    @SerializedName("end")
    public String end;

    public EventWrapper(String description, String location, String organizer, String status, String summary, String start, String end) {
        this.description = description;
        this.location = location;
        this.organizer = organizer;
        this.status = status;
        this.summary = summary;
        this.start = start;
        this.end = end;
    }
}
