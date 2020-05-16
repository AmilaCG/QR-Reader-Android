package com.auroid.qrscanner.serializable;

public class EventWrapper {

    public String description;
    public String location;
    public String organizer;
    public String status;
    public String summary;
    public String start;
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
