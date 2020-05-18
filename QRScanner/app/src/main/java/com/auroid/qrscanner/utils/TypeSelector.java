package com.auroid.qrscanner.utils;

import android.provider.ContactsContract;

import com.google.android.gms.vision.barcode.Barcode;

// This class converts "TYPE" constants of Barcode class to ContactsContract.CommonDataKinds class's
// "TYPE" constants.
public class TypeSelector {

    public static int selectPhoneType(int typeFromBarcode) {
        switch (typeFromBarcode) {
            case Barcode.Phone.HOME:
                return ContactsContract.CommonDataKinds.Phone.TYPE_HOME;

            case Barcode.Phone.MOBILE:
                return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;

            case Barcode.Phone.WORK:
                return ContactsContract.CommonDataKinds.Phone.TYPE_WORK;

            case Barcode.Phone.FAX:
                return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX;

            default:
                return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
        }
    }

    public static int selectEmailType(int typeFromBarcode) {
        switch (typeFromBarcode) {
            case Barcode.Email.HOME:
                return ContactsContract.CommonDataKinds.Email.TYPE_HOME;

            case Barcode.Email.WORK:
                return ContactsContract.CommonDataKinds.Email.TYPE_WORK;

            default:
                return ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
        }
    }

    public static int selectAddressType(int typeFromBarcode) {
        switch (typeFromBarcode) {
            case Barcode.Email.HOME:
                return ContactsContract.CommonDataKinds.Email.TYPE_HOME;

            case Barcode.Email.WORK:
                return ContactsContract.CommonDataKinds.Email.TYPE_WORK;

            default:
                return ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
        }
    }
}
