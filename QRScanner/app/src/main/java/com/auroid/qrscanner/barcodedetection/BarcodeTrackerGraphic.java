package com.auroid.qrscanner.barcodedetection;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;

import com.auroid.qrscanner.camera.GraphicOverlay;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

class BarcodeTrackerGraphic extends BarcodeGraphicBase {

    private final Point[] mCornerPts;
    private GraphicOverlay mOverlay;

    BarcodeTrackerGraphic(GraphicOverlay overlay, FirebaseVisionBarcode barcode) {
        super(overlay);
        mOverlay = overlay;
        mCornerPts = barcode.getCornerPoints();
    }

    @Override
    protected void draw(Canvas canvas) {
        super.draw(canvas);

        drawCorners(canvas);
    }

    private void drawCorners(Canvas canvas) {
        Path path = new Path();

        // Higher the value, smaller the corner size (1 = min value = full rectangle)
        float cornerSizeFactor = 5.5f;
        float segX;
        float segY;

        path.reset();

        segX = (mOverlay.translateX(mCornerPts[1].x) - mOverlay.translateX(mCornerPts[0].x))
                / cornerSizeFactor;
        segY = (mOverlay.translateX(mCornerPts[1].y) - mOverlay.translateX(mCornerPts[0].y))
                / cornerSizeFactor;

        path.moveTo(mOverlay.translateX(mCornerPts[1].x) - segX,
                mOverlay.translateY(mCornerPts[1].y) - segY);

        //         __
        //
        //
        //
        //
        path.lineTo(mOverlay.translateX(mCornerPts[1].x) , mOverlay.translateY(mCornerPts[1].y));

        segX = (mOverlay.translateX(mCornerPts[2].x) - mOverlay.translateX(mCornerPts[1].x))
                / cornerSizeFactor;
        segY = (mOverlay.translateX(mCornerPts[2].y) - mOverlay.translateX(mCornerPts[1].y))
                / cornerSizeFactor;

        //         __
        //           |
        //
        //
        //
        path.lineTo(mOverlay.translateX(mCornerPts[1].x) + segX,
                mOverlay.translateY(mCornerPts[1].y) + segY);

        path.moveTo(mOverlay.translateX(mCornerPts[2].x) - segX,
                mOverlay.translateY(mCornerPts[2].y) - segY);

        //         __
        //           |
        //
        //
        //           |
        path.lineTo(mOverlay.translateX(mCornerPts[2].x) , mOverlay.translateY(mCornerPts[2].y));

        segX = (mOverlay.translateX(mCornerPts[3].x) - mOverlay.translateX(mCornerPts[2].x))
                / cornerSizeFactor;
        segY = (mOverlay.translateX(mCornerPts[3].y) - mOverlay.translateX(mCornerPts[2].y))
                / cornerSizeFactor;

        //         __
        //           |
        //
        //
        //         __|
        path.lineTo(mOverlay.translateX(mCornerPts[2].x) + segX,
                mOverlay.translateY(mCornerPts[2].y) + segY);

        path.moveTo(mOverlay.translateX(mCornerPts[3].x) - segX,
                mOverlay.translateY(mCornerPts[3].y) - segY);

        //         __
        //           |
        //
        //
        //  __     __|
        path.lineTo(mOverlay.translateX(mCornerPts[3].x) , mOverlay.translateY(mCornerPts[3].y));

        segX = (mOverlay.translateX(mCornerPts[0].x) - mOverlay.translateX(mCornerPts[3].x))
                / cornerSizeFactor;
        segY = (mOverlay.translateX(mCornerPts[0].y) - mOverlay.translateX(mCornerPts[3].y))
                / cornerSizeFactor;

        //         __
        //           |
        //
        //
        // |__     __|
        path.lineTo(mOverlay.translateX(mCornerPts[3].x) + segX,
                mOverlay.translateY(mCornerPts[3].y) + segY);

        path.moveTo(mOverlay.translateX(mCornerPts[0].x) - segX,
                mOverlay.translateY(mCornerPts[0].y) - segY);

        //         __
        // |         |
        //
        //
        // |__     __|
        path.lineTo(mOverlay.translateX(mCornerPts[0].x) , mOverlay.translateY(mCornerPts[0].y));

        segX = (mOverlay.translateX(mCornerPts[1].x) - mOverlay.translateX(mCornerPts[0].x))
                / cornerSizeFactor;
        segY = (mOverlay.translateX(mCornerPts[1].y) - mOverlay.translateX(mCornerPts[0].y))
                / cornerSizeFactor;

        //  __     __
        // |         |
        //
        //
        // |__     __|
        path.lineTo(mOverlay.translateX(mCornerPts[0].x) + segX,
                mOverlay.translateY(mCornerPts[0].y) + segY);

        canvas.drawPath(path, trackerPaint);
    }
}
