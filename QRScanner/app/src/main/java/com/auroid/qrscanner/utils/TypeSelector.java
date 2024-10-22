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

    public static String barcodeFormatAsString(int format) {
        switch (format) {
            case Barcode.FORMAT_AZTEC:
                return "Aztec Code";
            case Barcode.FORMAT_CODABAR:
                return "Codabar";
            case Barcode.FORMAT_CODE_39:
                return "Code 39";
            case Barcode.FORMAT_CODE_93:
                return "Code 93";
            case Barcode.FORMAT_CODE_128:
                return "Code 128";
            case Barcode.FORMAT_DATA_MATRIX:
                return "Data Matrix";
            case Barcode.FORMAT_EAN_8:
                return "EAN-8";
            case Barcode.FORMAT_EAN_13:
                return "EAN-13";
            case Barcode.FORMAT_ITF:
                return "ITF (Interleaved 2 of 5)";
            case Barcode.FORMAT_PDF417:
                return "PDF417";
            case Barcode.FORMAT_QR_CODE:
                return "QR Code";
            case Barcode.FORMAT_UPC_A:
                return "UPC-A";
            case Barcode.FORMAT_UPC_E:
                return "UPC-E";
            case Barcode.FORMAT_UNKNOWN:
                return "Unknown Format";
        }
        return "Unknown Format";
    }
}
