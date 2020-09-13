package com.auroid.qrscanner.consts;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;

// This class holds common definitions across the app
public class CommonDefines {

    public static final BarcodeScannerOptions barcodeScannerOptions =
            new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_CODE_128)
            .build();
}
