package com.amila.qrscanner;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class BarcodeTrackerView extends View {
    private static final String TAG = "BarcodeTrackerView";

    private Point[] mCornerPts;
    private Paint mPaint;

    private static int mPrevSizeWidth;
    private static int mPrevSizeHeight;
    private int mCanvasWidth;
    private int mCanvasHeight;

    public BarcodeTrackerView(Context context) {
        super(context);
        init();
    }

    public BarcodeTrackerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(14);
        mPaint.setColor(Color.GREEN);

        mCornerPts = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        handleOrientation();

        float scaleX = (float) mCanvasWidth / mPrevSizeWidth;
        float scaleY = (float) mCanvasHeight / mPrevSizeHeight;

        if(mCornerPts != null) {
            float[] pts = {
                    mCornerPts[0].x * scaleX, mCornerPts[0].y * scaleY,
                    mCornerPts[1].x * scaleX, mCornerPts[1].y * scaleY,
                    mCornerPts[2].x * scaleX, mCornerPts[2].y * scaleY,
                    mCornerPts[3].x * scaleX, mCornerPts[3].y * scaleY };

            canvas.drawPoints(pts, mPaint);
        }
    }

    @Override
    public void postInvalidate() {
        super.postInvalidate();
    }

    public void updateView(Point[] cornerPoints) {
        this.mCornerPts = cornerPoints;
        postInvalidate();
    }

    public void clearView() {
        mCornerPts = null;
        postInvalidate();
    }

    public static void setPreviewSize(int width, int height) {
        mPrevSizeWidth = width;
        mPrevSizeHeight = height;
        Log.d(TAG, "PreviewSize width: " + width + ", height: " + height);
    }

    private boolean isPortraitMode() {
        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        } else {
            return false;
        }
    }

    private void handleOrientation() {
        int min = Math.min(getWidth(), getHeight());
        int max = Math.max(getWidth(), getHeight());

        int minPrev = Math.min(mPrevSizeWidth, mPrevSizeHeight);
        int maxPrev = Math.max(mPrevSizeWidth, mPrevSizeHeight);
        if (isPortraitMode()) {
            // Swap width and height sizes when in portrait, since it will be rotated by
            // 90 degrees
            mCanvasWidth = min;
            mCanvasHeight = max;
            mPrevSizeWidth = minPrev;
            mPrevSizeHeight = maxPrev;
        } else {
            mCanvasWidth = max;
            mCanvasHeight = min;
            mPrevSizeWidth = maxPrev;
            mPrevSizeHeight = minPrev;
        }
    }
}
