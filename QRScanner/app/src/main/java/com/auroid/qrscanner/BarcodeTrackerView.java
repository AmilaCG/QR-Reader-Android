package com.auroid.qrscanner;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

public class BarcodeTrackerView extends View {
    private static final String TAG = "BarcodeTrackerView";

    private Point[] mCornerPts;
    private Paint mPaint;
    private Path mPath;

    private static int mPrevSizeWidth;
    private static int mPrevSizeHeight;
    private int mCanvasWidth;
    private int mCanvasHeight;

    public BarcodeTrackerView(Context context) {
        super(context);
        init(context);
    }

    public BarcodeTrackerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        float radius = 20.0f;
        CornerPathEffect cornerPathEffect = new CornerPathEffect(radius);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(8);
        mPaint.setColor(ContextCompat.getColor(context, R.color.colorAccent));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setPathEffect(cornerPathEffect);

        mCornerPts = null;

        mPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        handleOrientation();

        if(mCornerPts != null) {
            Log.d(TAG, "onDraw: called");
            drawCorners(canvas);
        }
    }

    private void drawCorners(Canvas canvas) {
        float scaleX = (float) mCanvasWidth / mPrevSizeWidth;
        float scaleY = (float) mCanvasHeight / mPrevSizeHeight;
        // Higher the value, smaller the corner size (1 = min value = full rectangle)
        float cornerSizeFactor = 5.5f;

        float segX;
        float segY;

        mPath.reset();

        segX = (mCornerPts[1].x * scaleX - mCornerPts[0].x * scaleX) / cornerSizeFactor;
        segY = (mCornerPts[1].y * scaleX - mCornerPts[0].y * scaleX) / cornerSizeFactor;

        mPath.moveTo(mCornerPts[1].x * scaleX - segX, mCornerPts[1].y * scaleY - segY);

        //         __
        //
        //
        //
        //
        mPath.lineTo(mCornerPts[1].x * scaleX , mCornerPts[1].y * scaleY);

        segX = (mCornerPts[2].x * scaleX - mCornerPts[1].x * scaleX) / cornerSizeFactor;
        segY = (mCornerPts[2].y * scaleX - mCornerPts[1].y * scaleX) / cornerSizeFactor;

        //         __
        //           |
        //
        //
        //
        mPath.lineTo(mCornerPts[1].x * scaleX + segX , mCornerPts[1].y * scaleY + segY);

        mPath.moveTo(mCornerPts[2].x * scaleX - segX, mCornerPts[2].y * scaleY - segY);

        //         __
        //           |
        //
        //
        //           |
        mPath.lineTo(mCornerPts[2].x * scaleX , mCornerPts[2].y * scaleY);

        segX = (mCornerPts[3].x * scaleX - mCornerPts[2].x * scaleX) / cornerSizeFactor;
        segY = (mCornerPts[3].y * scaleX - mCornerPts[2].y * scaleX) / cornerSizeFactor;

        //         __
        //           |
        //
        //
        //         __|
        mPath.lineTo(mCornerPts[2].x * scaleX + segX , mCornerPts[2].y * scaleY + segY);

        mPath.moveTo(mCornerPts[3].x * scaleX - segX, mCornerPts[3].y * scaleY - segY);

        //         __
        //           |
        //
        //
        //  __     __|
        mPath.lineTo(mCornerPts[3].x * scaleX , mCornerPts[3].y * scaleY);

        segX = (mCornerPts[0].x * scaleX - mCornerPts[3].x * scaleX) / cornerSizeFactor;
        segY = (mCornerPts[0].y * scaleX - mCornerPts[3].y * scaleX) / cornerSizeFactor;

        //         __
        //           |
        //
        //
        // |__     __|
        mPath.lineTo(mCornerPts[3].x * scaleX + segX , mCornerPts[3].y * scaleY + segY);

        mPath.moveTo(mCornerPts[0].x * scaleX - segX, mCornerPts[0].y * scaleY - segY);

        //         __
        // |         |
        //
        //
        // |__     __|
        mPath.lineTo(mCornerPts[0].x * scaleX , mCornerPts[0].y * scaleY);

        segX = (mCornerPts[1].x * scaleX - mCornerPts[0].x * scaleX) / cornerSizeFactor;
        segY = (mCornerPts[1].y * scaleX - mCornerPts[0].y * scaleX) / cornerSizeFactor;

        //  __     __
        // |         |
        //
        //
        // |__     __|
        mPath.lineTo(mCornerPts[0].x * scaleX + segX , mCornerPts[0].y * scaleY + segY);

        canvas.drawPath(mPath, mPaint);
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
