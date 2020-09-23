package com.auroid.qrscanner.consts;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;

// This class holds common definitions across the app
public class CommonDefines {

    public static final BarcodeScannerOptions barcodeScannerOptions =
            new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build();
}
