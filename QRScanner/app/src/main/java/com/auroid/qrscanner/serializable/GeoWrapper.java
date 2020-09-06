package com.auroid.qrscanner.serializable;

import com.google.gson.annotations.SerializedName;

public class GeoWrapper {

    @SerializedName("lat")
    public double lat;
    @SerializedName("lng")
    public double lng;

    public GeoWrapper(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }
}
