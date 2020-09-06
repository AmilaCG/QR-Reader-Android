package com.auroid.qrscanner.serializable;

import com.google.gson.annotations.SerializedName;

public class BarcodeWrapper {

    @SerializedName("valueFormat")
    public int valueFormat;
    @SerializedName("displayValue")
    public String displayValue;
    @SerializedName("rawValue")
    public String rawValue;
    @SerializedName("url")
    public String url;
    @SerializedName("phoneNumber")
    public String phoneNumber;
    @SerializedName("geoWrapper")
    public GeoWrapper geoWrapper;
    @SerializedName("eventWrapper")
    public EventWrapper eventWrapper;
    @SerializedName("contactWrapper")
    public ContactWrapper contactWrapper;
    @SerializedName("wifiWrapper")
    public WiFiWrapper wifiWrapper;

    public BarcodeWrapper(int valueFormat, String displayValue, String rawValue) {
        this.valueFormat = valueFormat;
        this.displayValue = displayValue;
        this.rawValue = rawValue;
        this.url = null;
        this.phoneNumber = null;
        this.geoWrapper = null;
        this.eventWrapper = null;
        this.contactWrapper = null;
        this.wifiWrapper = null;
    }
}
