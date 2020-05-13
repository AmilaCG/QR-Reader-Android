package com.auroid.qrscanner;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.view.Gravity;
import android.view.SurfaceView;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.auroid.qrscanner.camera.PreviewFrameSetListener;

public class SurfaceSizeCalculator implements PreviewFrameSetListener {

    private Context mContext;

    private SurfaceView mSurfaceView;
    private BarcodeTrackerView mTrackerView;

    private int mPreviewWidth;
    private int mPreviewHeight;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private ScaleMode mScaleMode = ScaleMode.FILL;

    private enum ScaleMode {
        FIT,
        FILL
    }

    public SurfaceSizeCalculator(Context context,
                                 SurfaceView surfaceView,
                                 BarcodeTrackerView trackerView) {
        this.mContext = context;
        this.mSurfaceView = surfaceView;
        this.mTrackerView = trackerView;
    }

    @Override
    public void onPreviewFrameSet(int width, int height) {
        this.mPreviewWidth = width;
        this.mPreviewHeight = height;

        this.mSurfaceWidth = mSurfaceView.getHolder().getSurfaceFrame().width();
        this.mSurfaceHeight = mSurfaceView.getHolder().getSurfaceFrame().height();

        handleOrientation();
        calculateSurfaceSize();

        CoordinatorLayout.LayoutParams params =
                new CoordinatorLayout.LayoutParams(mSurfaceWidth, mSurfaceHeight);
        params.gravity = Gravity.CENTER;

        Runnable myRunnable = () -> {
            mSurfaceView.setLayoutParams(params);
            mTrackerView.setLayoutParams(params);
        };
        Handler mainHandler = new Handler(mContext.getMainLooper());
        mainHandler.post(myRunnable);
    }

    public void calculateSurfaceSize() {
        int surfaceWidth = (mSurfaceHeight * mPreviewWidth) / mPreviewHeight;
        int surfaceHeight = (mSurfaceWidth * mPreviewHeight) / mPreviewWidth;

        if (surfaceWidth < mSurfaceWidth) {
            switch (mScaleMode) {
                case FIT:
                    mSurfaceWidth = surfaceWidth;
                    break;

                case FILL:
                    mSurfaceHeight = surfaceHeight;
                    break;
            }
        } else if (surfaceHeight < mSurfaceHeight) {
            switch (mScaleMode) {
                case FIT:
                    mSurfaceHeight = surfaceHeight;
                    break;

                case FILL:
                    mSurfaceWidth = surfaceWidth;
                    break;
            }
        }
    }

    private void handleOrientation() {
        int minPrev = Math.min(mPreviewWidth, mPreviewHeight);
        int maxPrev = Math.max(mPreviewWidth, mPreviewHeight);

        if (isPortraitMode()) {
            // Swap width and height sizes when in portrait, since it will be rotated by
            // 90 degrees
            mPreviewWidth = minPrev;
            mPreviewHeight = maxPrev;
        } else {
            mPreviewWidth = maxPrev;
            mPreviewHeight = minPrev;
        }
    }

    private boolean isPortraitMode() {
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        } else {
            return false;
        }
    }
}
