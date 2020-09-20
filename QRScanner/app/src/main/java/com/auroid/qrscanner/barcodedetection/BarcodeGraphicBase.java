package com.auroid.qrscanner.barcodedetection;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import androidx.core.content.ContextCompat;
import com.auroid.qrscanner.camera.GraphicOverlay;
import com.auroid.qrscanner.camera.GraphicOverlay.Graphic;
import com.auroid.qrscanner.R;
import com.auroid.qrscanner.utils.PreferenceUtils;

abstract class BarcodeGraphicBase extends Graphic {

    private final Paint scrimPaint;
    private final Paint eraserPaint;
    private final Paint pathPaint;
    private final int boxCornerRadius;
    private final RectF boxRect;

    BarcodeGraphicBase(GraphicOverlay overlay) {
        super(overlay);

        boxCornerRadius =
                context.getResources().getDimensionPixelOffset(R.dimen.barcode_reticle_corner_radius);

        scrimPaint = new Paint();
        scrimPaint.setColor(ContextCompat.getColor(context, R.color.barcode_reticle_background));

        eraserPaint = new Paint();
        eraserPaint.setStrokeWidth(
                context.getResources().getDimensionPixelOffset(R.dimen.barcode_reticle_stroke_width));
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        pathPaint = new Paint();
        pathPaint.setColor(Color.WHITE);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(eraserPaint.getStrokeWidth());

        boxRect = PreferenceUtils.getBarcodeReticleBox(overlay);
    }

    @Override
    protected void draw(Canvas canvas) {
        // Draws the dark background scrim and leaves the box area clear.
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), scrimPaint);
        // As the stroke is always centered, so erase twice with FILL and STROKE respectively to clear
        // all area that the box rect would occupy.
        eraserPaint.setStyle(Style.FILL);
        canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, eraserPaint);
        eraserPaint.setStyle(Style.STROKE);
        canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, pathPaint);

        eraserPaint.setStyle(Style.FILL);
        int strokeWidth =
                context.getResources().getDimensionPixelOffset(R.dimen.barcode_reticle_stroke_width);
        double boxEdgeLengthFactor = 0.75;
        int verticalEraserWidth = (int) ((boxRect.right - boxRect.left) * boxEdgeLengthFactor);
        int horizontalEraserHeight = (int) ((boxRect.bottom - boxRect.top) * boxEdgeLengthFactor);
        // Erase top and bottom strokes
        canvas.drawRect(
                boxRect.left + verticalEraserWidth,
                boxRect.top - strokeWidth,
                boxRect.right - verticalEraserWidth,
                boxRect.bottom + strokeWidth,
                eraserPaint);
        // Erase left and right strokes
        canvas.drawRect(
                boxRect.left - strokeWidth,
                boxRect.top + horizontalEraserHeight,
                boxRect.right + strokeWidth,
                boxRect.bottom - horizontalEraserHeight,
                eraserPaint);
    }
}
