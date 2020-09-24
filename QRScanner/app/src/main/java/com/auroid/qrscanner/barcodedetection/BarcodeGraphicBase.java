/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.auroid.qrscanner.barcodedetection;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import androidx.core.content.ContextCompat;
import com.auroid.qrscanner.camera.GraphicOverlay;
import com.auroid.qrscanner.camera.GraphicOverlay.Graphic;
import com.auroid.qrscanner.R;
import com.auroid.qrscanner.utils.PreferenceUtils;

abstract class BarcodeGraphicBase extends Graphic {

    private final Paint boxPaint;
    private final Paint scrimPaint;
    private final Paint eraserPaint;

    final int boxCornerRadius;
    final Paint pathPaint;
    final RectF boxRect;

    BarcodeGraphicBase(GraphicOverlay overlay) {
        super(overlay);

        boxPaint = new Paint();
        boxPaint.setColor(ContextCompat.getColor(context, R.color.barcode_reticle_stroke));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(
                context.getResources().getDimensionPixelOffset(R.dimen.barcode_reticle_stroke_width));

        scrimPaint = new Paint();
        scrimPaint.setColor(ContextCompat.getColor(context, R.color.barcode_reticle_background));
        eraserPaint = new Paint();
        eraserPaint.setStrokeWidth(boxPaint.getStrokeWidth());
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        boxCornerRadius =
                context.getResources().getDimensionPixelOffset(R.dimen.barcode_reticle_corner_radius);

        pathPaint = new Paint();
        pathPaint.setColor(Color.WHITE);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(boxPaint.getStrokeWidth());
        pathPaint.setPathEffect(new CornerPathEffect(boxCornerRadius));
        pathPaint.setAntiAlias(true);

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
        canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, eraserPaint);

        // Draws the box.
        canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, boxPaint);
        drawCorners(canvas);
    }

    // TODO: In Android 6, ending line is shorter Due to the PathEffect on pathPaint. Fix it.
    private void drawCorners(Canvas canvas) {
        Path path = new Path();

        // Higher the value, smaller the corner size (1 = min value = full rectangle)
        float cornerSizeFactor = 5f;
        float segX;
        float segY;

        path.reset();

        segX = (boxRect.right - boxRect.left) / cornerSizeFactor;
        path.moveTo(boxRect.right - segX, boxRect.top);

        //         __
        //
        //
        //
        //
        path.lineTo(boxRect.right , boxRect.top);

        segY = (boxRect.bottom - boxRect.top) / cornerSizeFactor;

        //         __
        //           |
        //
        //
        //
        path.lineTo(boxRect.right, boxRect.top + segY);

        path.moveTo(boxRect.right, boxRect.bottom - segY);

        //         __
        //           |
        //
        //
        //           |
        path.lineTo(boxRect.right , boxRect.bottom);

        segX = (boxRect.left - boxRect.right) / cornerSizeFactor;

        //         __
        //           |
        //
        //
        //         __|
        path.lineTo(boxRect.right + segX, boxRect.bottom);

        path.moveTo(boxRect.left - segX, boxRect.bottom);

        //         __
        //           |
        //
        //
        //  __     __|
        path.lineTo(boxRect.left , boxRect.bottom);

        segY = (boxRect.top - boxRect.bottom) / cornerSizeFactor;

        //         __
        //           |
        //
        //
        // |__     __|
        path.lineTo(boxRect.left, boxRect.bottom + segY);

        path.moveTo(boxRect.left, boxRect.top - segY);

        //         __
        // |         |
        //
        //
        // |__     __|
        path.lineTo(boxRect.left , boxRect.top);

        segX = (boxRect.right - boxRect.left) / cornerSizeFactor;

        //  __     __
        // |         |
        //
        //
        // |__     __|
        path.lineTo(boxRect.left + segX, boxRect.top);

        canvas.drawPath(path, pathPaint);
    }
}
