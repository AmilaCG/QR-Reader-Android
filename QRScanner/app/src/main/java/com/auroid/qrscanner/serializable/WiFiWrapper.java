package com.auroid.qrscanner.serializable;

public class WiFiWrapper {

    public int encryptionType;
    public String password;
    public String ssid;

    public WiFiWrapper(int encryptionType, String password, String ssid) {
        this.encryptionType = encryptionType;
        this.password = password;
        this.ssid = ssid;
    }
}
