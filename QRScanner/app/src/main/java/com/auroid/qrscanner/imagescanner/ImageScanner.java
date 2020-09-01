package com.auroid.qrscanner.imagescanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.widget.Toast;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class ImageScanner {

    public interface DecodeListener {
        void onSuccessfulDecode(List<Barcode> barcodes);
    }

    private Context mContext;
    private Bitmap mBitmap;
    private BarcodeScanner mScanner;

    public ImageScanner(Context context) {
        mContext = context;
    }

    public void setImage(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public void decode(DecodeListener listener) {
        InputImage image = InputImage.fromBitmap(mBitmap, 0);

        final BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_CODE_128)
                .build();

        mScanner = BarcodeScanning.getClient(options);
        mScanner.process(image).addOnSuccessListener(barcodes -> {
            if (barcodes.size() > 0) {
                listener.onSuccessfulDecode(barcodes);
            } else {
                Toast.makeText(mContext, "No barcodes detected", Toast.LENGTH_SHORT).show();
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
