package com.auroid.qrscanner.camera;

public interface PreviewFrameSetListener {

    // PreviewFrameListener used to let others know the resolution of the preview frame created by
    // Camera API
    void onPreviewFrameSet(int width, int height);
}
