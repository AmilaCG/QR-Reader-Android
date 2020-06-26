package com.auroid.qrscanner.barcodedetection;

import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import androidx.core.content.ContextCompat;

import com.auroid.qrscanner.R;
import com.auroid.qrscanner.camera.GraphicOverlay;
import com.google.mlkit.vision.barcode.Barcode;

class BarcodeTrackerGraphic extends BarcodeGraphicBase {

    private final Point[] mCornerPts;
    private final Paint mTrackerPaint;
    private GraphicOverlay mOverlay;

    BarcodeTrackerGraphic(GraphicOverlay overlay, Barcode barcode) {
        super(overlay);
        mOverlay = overlay;
        mCornerPts = barcode.getCornerPoints();

        int trackerCornerRadius =
                context.getResources().getDimensionPixelOffset(R.dimen.barcode_tracker_corner_radius);

        mTrackerPaint = new Paint();
        mTrackerPaint.setColor(ContextCompat.getColor(context, R.color.colorTracker));
        mTrackerPaint.setStyle(Paint.Style.STROKE);
        mTrackerPaint.setStrokeJoin(Paint.Join.MITER);
        mTrackerPaint.setStrokeWidth(8);
        mTrackerPaint.setPathEffect(new CornerPathEffect(trackerCornerRadius));
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

        canvas.drawPath(path, mTrackerPaint);
    }
}
