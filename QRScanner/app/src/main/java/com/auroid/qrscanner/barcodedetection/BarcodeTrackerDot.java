package com.auroid.qrscanner.barcodedetection;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.auroid.qrscanner.R;
import com.auroid.qrscanner.camera.GraphicOverlay;

public class BarcodeTrackerDot extends BarcodeGraphicBase {

    private static final int DOT_FADE_OUT_ANIMATOR_DURATION_MS = 300;

    private final Paint mPaint;
    private final int mDotRadius;
    private int mDotAlpha;

    private final RectF mBoundingBox;

    private final GraphicOverlay mGraphicOverlay;
    private final Context mContext;

    BarcodeTrackerDot(GraphicOverlay overlay, RectF boundingBox) {
        super(overlay);
        mGraphicOverlay = overlay;
        mBoundingBox = boundingBox;
        mContext = overlay.getContext();

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);

        mDotRadius =
                context.getResources().getDimensionPixelOffset(R.dimen.static_image_dot_radius_selected);
        mDotAlpha = 255;
    }

    public void fadeOutDot() {
        ValueAnimator fadeOutAnimator;
        fadeOutAnimator =
                ValueAnimator.ofInt(255, 0)
                .setDuration(DOT_FADE_OUT_ANIMATOR_DURATION_MS);
        fadeOutAnimator.addUpdateListener(animation -> {
            mDotAlpha = (Integer) fadeOutAnimator.getAnimatedValue();
            mGraphicOverlay.postInvalidate();
        });
        fadeOutAnimator.start();
    }

    public int getFadeoutDuration() {
        return DOT_FADE_OUT_ANIMATOR_DURATION_MS;
    }

    @Override
    protected void draw(Canvas canvas) {
        super.draw(canvas);

        mPaint.setColor(mContext.getResources().getColor(R.color.tracker_dot));
        mPaint.setAlpha(mDotAlpha);
        canvas.drawCircle(mBoundingBox.centerX(), mBoundingBox.centerY(), mDotRadius, mPaint);
    }
}
