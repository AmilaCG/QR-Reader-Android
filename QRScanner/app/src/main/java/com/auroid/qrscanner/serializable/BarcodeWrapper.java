package com.auroid.qrscanner.serializable;

public class BarcodeWrapper {

    public int valueFormat;
    public String displayValue;
    public String rawValue;
    public String url;
    public String phoneNumber;
    public GeoWrapper geoWrapper;
    public EventWrapper eventWrapper;
    public ContactWrapper contactWrapper;

    public BarcodeWrapper(int valueFormat, String displayValue, String rawValue) {
        this.valueFormat = valueFormat;
        this.displayValue = displayValue;
        this.rawValue = rawValue;
        this.url = null;
        this.phoneNumber = null;
        this.geoWrapper = null;
        this.eventWrapper = null;
        this.contactWrapper = null;
    }
}
