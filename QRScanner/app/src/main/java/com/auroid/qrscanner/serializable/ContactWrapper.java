package com.auroid.qrscanner.serializable;

import com.google.gson.annotations.SerializedName;

public class ContactWrapper {

    @SerializedName("formattedName")
    public String formattedName;
    @SerializedName("organization")
    public String organization;
    @SerializedName("title")
    public String title;
    @SerializedName("urls")
    public String[] urls;
    @SerializedName("phones")
    public PhoneWrapper[] phones;
    @SerializedName("emails")
    public EmailWrapper[] emails;
    @SerializedName("addresses")
    public AddressWrapper[] addresses;

    public ContactWrapper(String formattedName,
                          String organization,
                          String title,
                          String[] urls,
                          PhoneWrapper[] phones,
                          EmailWrapper[] emails,
                          AddressWrapper[] addresses) {
        this.formattedName = formattedName;
        this.organization = organization;
        this.title = title;
        this.urls = urls;
        this.phones = phones;
        this.emails = emails;
        this.addresses = addresses;
    }
}
