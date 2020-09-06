package com.auroid.qrscanner.serializable;

import com.google.gson.annotations.SerializedName;

public class EmailWrapper {

    @SerializedName("address")
    public String address;
    @SerializedName("type")
    public int type;

    public EmailWrapper(String address, int type) {
        this.address = address;
        this.type = type;
    }
}
