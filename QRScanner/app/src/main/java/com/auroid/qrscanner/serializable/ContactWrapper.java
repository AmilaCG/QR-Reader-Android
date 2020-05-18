package com.auroid.qrscanner.serializable;

public class ContactWrapper {

    public String formattedName;
    public String organization;
    public String title;
    public String[] urls;
    public PhoneWrapper[] phones;
    public EmailWrapper[] emails;
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
