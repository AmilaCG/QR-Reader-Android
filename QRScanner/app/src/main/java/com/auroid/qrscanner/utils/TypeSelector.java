package com.auroid.qrscanner.utils;

import android.provider.ContactsContract;

import com.google.mlkit.vision.barcode.Barcode;

// This class converts "TYPE" constants of Barcode class to ContactsContract.CommonDataKinds class's
// "TYPE" constants.
public class TypeSelector {

    public static int selectPhoneType(int typeFromBarcode) {
        switch (typeFromBarcode) {
            case Barcode.Phone.TYPE_HOME:
                return ContactsContract.CommonDataKinds.Phone.TYPE_HOME;

            case Barcode.Phone.TYPE_MOBILE:
                return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;

            case Barcode.Phone.TYPE_WORK:
                return ContactsContract.CommonDataKinds.Phone.TYPE_WORK;

            case Barcode.Phone.TYPE_FAX:
                return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX;

            default:
                return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
        }
    }

    public static int selectEmailType(int typeFromBarcode) {
        switch (typeFromBarcode) {
            case Barcode.Email.TYPE_HOME:
                return ContactsContract.CommonDataKinds.Email.TYPE_HOME;

            case Barcode.Email.TYPE_WORK:
                return ContactsContract.CommonDataKinds.Email.TYPE_WORK;

            default:
                return ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
        }
    }

    public static int selectAddressType(int typeFromBarcode) {
        switch (typeFromBarcode) {
            case Barcode.Address.TYPE_HOME:
                return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME;

            case Barcode.Address.TYPE_WORK:
                return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK;

            default:
                return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER;
        }
    }

    public static String phoneTypeAsString(int typeFromBarcode) {
        switch (typeFromBarcode) {
            case Barcode.Phone.TYPE_HOME:
                return "Home";

            case Barcode.Phone.TYPE_MOBILE:
                return "Mobile";

            case Barcode.Phone.TYPE_WORK:
                return "Work";

            case Barcode.Phone.TYPE_FAX:
                return "Fax";

            default:
                return "Other";
        }
    }

    public static String emailTypeAsString(int typeFromBarcode) {
        switch (typeFromBarcode) {
            case Barcode.Email.TYPE_HOME:
                return "Home";

            case Barcode.Email.TYPE_WORK:
                return "Work";

            default:
                return "Other";
        }
    }

    public static String addressTypeAsString(int typeFromBarcode) {
        switch (typeFromBarcode) {
            case Barcode.Address.TYPE_HOME:
                return "Home";

            case Barcode.Address.TYPE_WORK:
                return "Work";

            default:
                return "Other";
        }
    }
}
