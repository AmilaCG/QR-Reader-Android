package com.auroid.qrscanner.serializable;

import com.google.gson.annotations.SerializedName;

public class AddressWrapper {

    @SerializedName("addressLines")
    public String[] addressLines;
    @SerializedName("type")
    public int type;

    public AddressWrapper(String[] addressLines, int type) {
        this.addressLines = addressLines;
        this.type = type;
    }
}
