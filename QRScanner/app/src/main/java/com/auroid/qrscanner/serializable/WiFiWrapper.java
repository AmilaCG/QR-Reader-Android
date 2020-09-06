package com.auroid.qrscanner.serializable;

import com.google.gson.annotations.SerializedName;

public class WiFiWrapper {

    @SerializedName("encryptionType")
    public int encryptionType;
    @SerializedName("password")
    public String password;
    @SerializedName("ssid")
    public String ssid;

    public WiFiWrapper(int encryptionType, String password, String ssid) {
        this.encryptionType = encryptionType;
        this.password = password;
        this.ssid = ssid;
    }
}
