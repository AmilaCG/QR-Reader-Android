package com.auroid.qrscanner.serializable;

public class AddressWrapper {

    public String[] addressLines;
    public int type;

    public AddressWrapper(String[] addressLines, int type) {
        this.addressLines = addressLines;
        this.type = type;
    }
}
