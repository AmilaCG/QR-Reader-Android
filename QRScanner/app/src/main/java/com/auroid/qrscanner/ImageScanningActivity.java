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
import com.auroid.qrscanner.imagescanner.PreviewCardAdapter;
import com.auroid.qrscanner.imagescanner.TrackerDotView;
import com.auroid.qrscanner.serializable.BarcodeWrapper;
import com.auroid.qrscanner.settings.PreferenceUtils;
import com.auroid.qrscanner.utils.Utils;

import com.google.android.material.chip.Chip;
import com.google.common.collect.ImmutableList;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.mlkit.vision.barcode.Barcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageScanningActivity extends AppCompatActivity
        implements View.OnClickListener, ImageScanner.DecodeListener, PreviewCardAdapter.CardItemListener {

    private static final String TAG = "ImageScanningActivity";

    private final int MAX_IMAGE_SIZE = 2048;
    private static final int RC_PHOTO_LIBRARY = 26;

    private Bitmap mInputImage;
    private ImageView mImagePreview;
    private ProgressBar mProgressBar;
    private TextView mDecodingText;
    private Chip mPromptChip;

    private final ExecutorService imageDecodeExecutor = Executors.newSingleThreadExecutor();
    private ImageScanner mImageScanner;

    private RecyclerView mPreviewCardCarousel;
    private ViewGroup mDotViewContainer;
    private int mDotViewSize;
    private int mCurrentSelectedBarcodeIndex = 0;

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_scanning);

        mImagePreview = findViewById(R.id.image_preview);
        mProgressBar = findViewById(R.id.progressBar);
        mDecodingText = findViewById(R.id.text_decoding);
        mPromptChip = findViewById(R.id.bottom_prompt_chip);

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

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
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
        mPreviewCardCarousel.setAdapter(new PreviewCardAdapter(ImmutableList.of(), this));
        mPreviewCardCarousel.clearOnScrollListeners();
        mPromptChip.setVisibility(View.GONE);
        mDotViewContainer.removeAllViews();
        mCurrentSelectedBarcodeIndex = 0;

        imageDecodeExecutor.execute(() -> {
            try {
                mInputImage = Utils.loadImage(this, imageUri, MAX_IMAGE_SIZE);
            } catch (IOException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
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
                showPromptChip(getString(R.string.static_image_prompt_detected_results));
            });

            mImageScanner = new ImageScanner(this);

            mImageScanner.setImage(mInputImage);
            mImageScanner.decode(this);
        });
    }

    @Override
    public void onSuccessfulDecode(List<Barcode> barcodes) {
        if (barcodes.size() > 0) {
            Toast.makeText(this, barcodes.size() + " barcode(s) detected",
                    Toast.LENGTH_SHORT).show();

            List<DetectedBarcode> detectedBarcodeList = new ArrayList<>();
            for (int i = 0; i < barcodes.size(); i++) {
                detectedBarcodeList.add(new DetectedBarcode(barcodes.get(i), i));
            }

            mPreviewCardCarousel.setAdapter(
                    new PreviewCardAdapter(ImmutableList.copyOf(detectedBarcodeList), this));
            mPreviewCardCarousel.addOnScrollListener(
                    new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                            Log.d(TAG, "New card scroll state: " + newState);
                            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                                    View childView = recyclerView.getChildAt(i);
                                    if (childView.getX() >= 0) {
                                        int cardIndex = recyclerView.getChildAdapterPosition(childView);
                                        if (cardIndex != mCurrentSelectedBarcodeIndex) {
                                            selectNewBarcode(cardIndex);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
            );

            for (DetectedBarcode detectedBarcode : detectedBarcodeList) {
                TrackerDotView dotView = createDotView(detectedBarcode);
                dotView.setOnClickListener(
                        v -> {
                            if (detectedBarcode.getBarcodeIndex() == mCurrentSelectedBarcodeIndex) {
                                showBarcodeResult(detectedBarcode.getBarcode());
                            } else {
                                selectNewBarcode(detectedBarcode.getBarcodeIndex());
                                showBarcodeResult(detectedBarcode.getBarcode());
                                mPreviewCardCarousel.smoothScrollToPosition(
                                        detectedBarcode.getBarcodeIndex());
                            }
                        });

                mDotViewContainer.addView(dotView);
                AnimatorSet animatorSet =
                        ((AnimatorSet) AnimatorInflater.loadAnimator(
                                this, R.animator.static_image_dot_enter));
                animatorSet.setTarget(dotView);
                animatorSet.start();
            }
        }
    }

    @Override
    public void onUnsuccessfulDecode() {
        showPromptChip(getString(R.string.static_image_prompt_detected_no_results));
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

    private void selectNewBarcode(int objectIndex) {
        TrackerDotView dotViewToDeselect =
                (TrackerDotView) mDotViewContainer.getChildAt(mCurrentSelectedBarcodeIndex);
        dotViewToDeselect.playAnimationWithSelectedState(false);

        mCurrentSelectedBarcodeIndex = objectIndex;

        TrackerDotView selectedDotView =
                (TrackerDotView) mDotViewContainer.getChildAt(mCurrentSelectedBarcodeIndex);
        selectedDotView.playAnimationWithSelectedState(true);
    }

    @Override
    public void onPreviewCardClicked(DetectedBarcode detectedBarcode) {
        showBarcodeResult(detectedBarcode.getBarcode());
    }

    private void showBarcodeResult(Barcode barcode) {
        ResultHandler resultHandler = new ResultHandler(this);
        resultHandler.pushToDatabase(barcode);

        boolean openInBrowser = PreferenceUtils.shouldOpenDirectlyInBrowser(this);
        int barcodeValueType = barcode.getValueType();
        if (openInBrowser && barcodeValueType == Barcode.TYPE_URL) {
            BarcodeWrapper barcodeWrapper = new BarcodeWrapper(
                    barcodeValueType,
                    barcode.getDisplayValue(),
                    barcode.getRawValue());
            barcodeWrapper.url =
                    java.util.Objects.requireNonNull(barcode.getUrl()).getUrl();

            ActionHandler actionHandler = new ActionHandler(this, barcodeWrapper);
            actionHandler.openBrowser();
        } else {
            Intent intent = new Intent(this, BarcodeResultActivity.class);
            intent.putExtra("RESULT", resultHandler.getResultJson());
            startActivity(intent);
        }
        mFirebaseAnalytics.logEvent("scan_barcode_static_image", null);
        resultHandler.release();
    }

    private void showPromptChip(String message) {
        mPromptChip.setVisibility(View.VISIBLE);
        mPromptChip.setText(message);
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