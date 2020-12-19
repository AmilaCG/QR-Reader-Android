package com.auroid.qrscanner.imagescanner;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.auroid.qrscanner.consts.CommonDefines;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class ImageScanner {

    public interface DecodeListener {
        void onSuccessfulDecode(List<Barcode> barcodes);
        void onUnsuccessfulDecode();
    }

    private Bitmap mBitmap;
    private BarcodeScanner mScanner;

    public void setImage(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public void decode(DecodeListener listener) {
        InputImage image = InputImage.fromBitmap(mBitmap, 0);

        mScanner = BarcodeScanning.getClient(CommonDefines.barcodeScannerOptions);
        mScanner.process(image).addOnSuccessListener(barcodes -> {
            if (barcodes.size() > 0) {
                listener.onSuccessfulDecode(barcodes);
            } else {
                listener.onUnsuccessfulDecode();
            }
        });
    }

    public Bitmap addTrackers(List<Barcode> barcodes) {
        Bitmap drawableBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(drawableBitmap);

        Paint mTrackerPaint = new Paint();
        mTrackerPaint.setColor(Color.RED);
        mTrackerPaint.setStyle(Paint.Style.STROKE);
        mTrackerPaint.setStrokeJoin(Paint.Join.MITER);
        mTrackerPaint.setStrokeWidth(16);

        for (Barcode barcode : barcodes) {
            Point[] points = barcode.getCornerPoints();

            Path path = new Path();
            path.moveTo(points[0].x, points[0].y);
            path.lineTo(points[1].x, points[1].y);
            path.lineTo(points[2].x, points[2].y);
            path.lineTo(points[3].x, points[3].y);
            path.lineTo(points[0].x, points[0].y);

            canvas.drawPath(path, mTrackerPaint);
        }
        return drawableBitmap;
    }

    public void release() {
        if (mBitmap != null) mBitmap = null;
        mScanner.close();
    }
}
