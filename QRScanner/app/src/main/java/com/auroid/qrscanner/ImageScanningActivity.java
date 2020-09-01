package com.auroid.qrscanner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.auroid.qrscanner.imagescanner.DetectedBarcode;
import com.auroid.qrscanner.imagescanner.ImageScanner;
import com.auroid.qrscanner.imagescanner.TrackerDotView;
import com.auroid.qrscanner.utils.Utils;

import com.google.mlkit.vision.barcode.Barcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageScanningActivity extends AppCompatActivity
        implements View.OnClickListener, ImageScanner.DecodeListener {

    private static final String TAG = "ImageScanningActivity";

    private final int MAX_IMAGE_SIZE = 2048;
    private static final int RC_PHOTO_LIBRARY = 26;

    private Bitmap mInputImage;
    private ImageView mImagePreview;
    private ProgressBar mProgressBar;
    private TextView mDecodingText;
    private final ExecutorService imageDecodeExecutor = Executors.newSingleThreadExecutor();
    private ImageScanner mImageScanner;

    private RecyclerView mPreviewCardCarousel;
    private ViewGroup mDotViewContainer;
    private int mDotViewSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_scanning);

        mImagePreview = findViewById(R.id.image_preview);
        mProgressBar = findViewById(R.id.progressBar);
        mDecodingText = findViewById(R.id.text_decoding);

        findViewById(R.id.close_button).setOnClickListener(this);
        findViewById(R.id.photo_library_button).setOnClickListener(this);

        mPreviewCardCarousel = findViewById(R.id.card_recycler_view);
        mPreviewCardCarousel.setHasFixedSize(true);
        mPreviewCardCarousel.setLayoutManager(
                new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        mPreviewCardCarousel.addItemDecoration(new CardItemDecoration(getResources()));

        mDotViewContainer = findViewById(R.id.dot_view_container);
        mDotViewSize = getResources().getDimensionPixelOffset(R.dimen.static_image_dot_view_size);

        Uri pickedImageUri = getIntent().getData();
        if (pickedImageUri != null) {
            decodeImage(pickedImageUri);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageScanner.release();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.close_button) {
            onBackPressed();
        } else if (id == R.id.photo_library_button) {
            Utils.openImagePicker(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == RC_PHOTO_LIBRARY && data != null) {
            mImageScanner.release();
            mProgressBar.setVisibility(View.VISIBLE);
            mDecodingText.setVisibility(View.VISIBLE);
            ((BitmapDrawable) mImagePreview.getDrawable()).getBitmap().recycle();
            mImagePreview.setVisibility(View.GONE);

            decodeImage(data.getData());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void decodeImage(Uri imageUri) {
        mDotViewContainer.removeAllViews();
        imageDecodeExecutor.execute(() -> {
            try {
                mInputImage = Utils.loadImage(this, imageUri, MAX_IMAGE_SIZE);
            } catch (IOException e) {
                Log.e(TAG, "Failed to load file: " + imageUri, e);
                finish();
            }
            runOnUiThread(() -> {
                mProgressBar.setVisibility(View.GONE);
                mDecodingText.setVisibility(View.GONE);
                if (mImagePreview.getVisibility() == View.GONE) {
                    mImagePreview.setVisibility(View.VISIBLE);
                }
                mImagePreview.setImageBitmap(mInputImage);
            });

            mImageScanner = new ImageScanner(this);

            mImageScanner.setImage(mInputImage);
            mImageScanner.decode(this);
        });
    }

    @Override
    public void onSuccessfulDecode(List<Barcode> barcodes) {
        if (barcodes.size() > 0) {
            Toast.makeText(this, "Detected barcodes: " + barcodes.size(),
                    Toast.LENGTH_SHORT).show();

            mImagePreview.setImageBitmap(mImageScanner.addTrackers(barcodes));

            List<DetectedBarcode> detectedBarcodeList = new ArrayList<>();
            for (int i = 0; i < barcodes.size(); i++) {
                detectedBarcodeList.add(new DetectedBarcode(barcodes.get(i), i));
            }

            for(DetectedBarcode detectedBarcode : detectedBarcodeList) {
                TrackerDotView dotView = createDotView(detectedBarcode);
                mDotViewContainer.addView(dotView);
                AnimatorSet animatorSet =
                        ((AnimatorSet) AnimatorInflater.loadAnimator(
                                this, R.animator.static_image_dot_enter));
                animatorSet.setTarget(dotView);
                animatorSet.start();
            }
        }
    }
    
    private TrackerDotView createDotView(DetectedBarcode detectedBarcode) {
        float viewCoordinateScale;
        float horizontalGap;
        float verticalGap;
        float inputImageViewRatio = (float) mImagePreview.getWidth() / mImagePreview.getHeight();
        float inputBitmapRatio = (float) mInputImage.getWidth() / mInputImage.getHeight();
        if (inputBitmapRatio <= inputImageViewRatio) { // Image content fills height
            viewCoordinateScale = (float) mImagePreview.getHeight() / mInputImage.getHeight();
            horizontalGap =
                    (mImagePreview.getWidth() - mInputImage.getWidth() * viewCoordinateScale) / 2;
            verticalGap = 0;
        } else { // Image content fills width
            viewCoordinateScale = (float) mImagePreview.getWidth() / mInputImage.getWidth();
            horizontalGap = 0;
            verticalGap =
                    (mImagePreview.getHeight() - mInputImage.getHeight() * viewCoordinateScale) / 2;
        }

        Rect boundingBox = detectedBarcode.getBoundingBox();
        RectF boxInViewCoordinate =
                new RectF(
                        boundingBox.left * viewCoordinateScale + horizontalGap,
                        boundingBox.top * viewCoordinateScale + verticalGap,
                        boundingBox.right * viewCoordinateScale + horizontalGap,
                        boundingBox.bottom * viewCoordinateScale + verticalGap);
        boolean initialSelected = (detectedBarcode.getBarcodeIndex() == 0);
        TrackerDotView dotView = new TrackerDotView(this, initialSelected);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(mDotViewSize, mDotViewSize);
        PointF dotCenter =
                new PointF(
                        (boxInViewCoordinate.right + boxInViewCoordinate.left) / 2,
                        (boxInViewCoordinate.bottom + boxInViewCoordinate.top) / 2);
        layoutParams.setMargins(
                (int)(dotCenter.x - mDotViewSize / 2f), (int)(dotCenter.y - mDotViewSize / 2f), 0, 0);
        dotView.setLayoutParams(layoutParams);
        return dotView;
    }

    private static class CardItemDecoration extends RecyclerView.ItemDecoration {

        private final int cardSpacing;

        private CardItemDecoration(Resources resources) {
            cardSpacing = resources.getDimensionPixelOffset(R.dimen.preview_card_spacing);
        }

        @Override
        public void getItemOffsets(
                @NonNull Rect outRect,
                @NonNull View view,
                @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {
            int adapterPosition = parent.getChildAdapterPosition(view);
            outRect.left = adapterPosition == 0 ? cardSpacing * 2 : cardSpacing;
            if (parent.getAdapter() != null
                    && adapterPosition == parent.getAdapter().getItemCount() - 1) {
                outRect.right = cardSpacing;
            }
        }
    }
}