package com.auroid.qrscanner.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class AppRater implements DefaultLifecycleObserver {
    private static final String TAG = "AppRater";
    private static final long COOLDOWN_MS = TimeUnit.DAYS.toMillis(60);
    // A session lasts for this app process, including activity recreation.
    private static boolean mAttemptedThisSession;

    private final AppCompatActivity mActivity;
    private final SharedPreferences mPrefs;
    private ReviewManager mReviewManager;
    private ReviewInfo mReviewInfo;
    private boolean mBusy;
    private boolean mReviewOpportunity;

    public AppRater(AppCompatActivity activity) {
        mActivity = activity;
        mPrefs = mActivity.getSharedPreferences("apprater", Context.MODE_PRIVATE);
        mActivity.getLifecycle().addObserver(this);
    }

    public void recordScan() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
        int days = mPrefs.getInt("scan_days", 0);
        if (!today.equals(mPrefs.getString("last_scan_day", ""))) days++;
        // Reuse the old scan counter; do not erase existing usage history.
        mPrefs.edit()
                .putInt("scan_counter", mPrefs.getInt("scan_counter", 0) + 1)
                .putInt("scan_days", days)
                .putString("last_scan_day", today)
                .apply();
    }

    public boolean isBusy() {
        return mBusy;
    }

    private boolean isEligible() {
        long lastAttempt = mPrefs.getLong("last_review_attempt", 0);
        return mPrefs.getInt("scan_counter", 0) >= 5
                && mPrefs.getInt("scan_days", 0) >= 2
                && (lastAttempt == 0 || System.currentTimeMillis() - lastAttempt >= COOLDOWN_MS);
    }

    public void prepareReview() {
        if (mAttemptedThisSession || mBusy || !isEligible() || !canLaunch()) {
            return;
        }

        mAttemptedThisSession = true;
        mReviewOpportunity = true;
        Log.d(TAG, "Requesting review eligibility from Play");
        mReviewManager = ReviewManagerFactory.create(mActivity.getApplicationContext());
        mReviewManager.requestReviewFlow().addOnCompleteListener(request -> {
            // Preparation may finish while the result screen or an external app is open.
            // Never launch here, or accept a response after the return opportunity ends.
            if (!mReviewOpportunity) {
                return;
            }
            if (request.isSuccessful()) {
                mReviewInfo = request.getResult();
            } else {
                Log.w(TAG, "Review request failed", request.getException());
            }
        });
    }

    public void launchPreparedReview(Runnable onFinished) {
        ReviewInfo reviewInfo = mReviewInfo;
        discardReview();
        if (reviewInfo == null || mBusy || !isEligible() || !canLaunch()) {
            return;
        }

        mBusy = true;
        mPrefs.edit().putLong("last_review_attempt", System.currentTimeMillis()).apply();
        Log.d(TAG, "Launching review flow (display and submission are not observable)");
        mReviewManager.launchReviewFlow(mActivity, reviewInfo).addOnCompleteListener(flow -> {
            if (!flow.isSuccessful()) Log.w(TAG, "Review launch failed", flow.getException());
            mBusy = false;
            onFinished.run();
        });
    }

    private void discardReview() {
        mReviewOpportunity = false;
        mReviewInfo = null;
    }

    private boolean canLaunch() {
        return !mActivity.isFinishing() && !mActivity.isDestroyed()
                && mActivity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        discardReview();
    }
}
