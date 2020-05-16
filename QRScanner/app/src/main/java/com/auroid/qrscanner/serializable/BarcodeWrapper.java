package com.auroid.qrscanner.serializable;

public class BarcodeWrapper {

    public int valueFormat;
    public String displayValue;
    public String url;
    public String phoneNumber;
    public GeoWrapper geoWrapper;
    public EventWrapper eventWrapper;

    public BarcodeWrapper(int valueFormat, String displayValue, String url, String phoneNumber, GeoWrapper geoWrapper, EventWrapper eventWrapper) {
        this.valueFormat = valueFormat;
        this.displayValue = displayValue;
        this.url = url;
        this.phoneNumber = phoneNumber;
        this.geoWrapper = geoWrapper;
        this.eventWrapper = eventWrapper;
    }
}
