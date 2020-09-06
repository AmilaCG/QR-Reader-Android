package com.auroid.qrscanner.serializable;

import com.google.gson.annotations.SerializedName;

public class PhoneWrapper {

    @SerializedName("number")
    public String number;
    @SerializedName("type")
    public int type;

    public PhoneWrapper(String number, int type) {
        this.number = number;
        this.type = type;
    }
}
