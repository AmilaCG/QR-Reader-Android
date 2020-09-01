package com.auroid.qrscanner.imagescanner;

import android.graphics.Rect;
import com.google.mlkit.vision.barcode.Barcode;

public class DetectedBarcode {

    private final Barcode mBarcode;
    private final int mBarcodeIndex;

    public DetectedBarcode(Barcode barcode, int index) {
        mBarcode = barcode;
        mBarcodeIndex = index;
    }

    public int getBarcodeIndex() {
        return mBarcodeIndex;
    }

    public Rect getBoundingBox() {
        return mBarcode.getBoundingBox();
    }

    public Barcode getBarcode() {
        return mBarcode;
    }
}
