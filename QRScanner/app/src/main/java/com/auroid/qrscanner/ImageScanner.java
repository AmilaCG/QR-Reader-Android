package com.auroid.qrscanner;

import android.graphics.Bitmap;

public class ImageScanner {

    private Bitmap mBitmap;

    public ImageScanner(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public void decode() {

    }

    public void release() {
        if (mBitmap != null) mBitmap = null;
    }
}
