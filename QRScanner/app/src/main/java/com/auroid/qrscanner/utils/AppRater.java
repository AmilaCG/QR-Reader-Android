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
import com.google.android.play.core.review.ReviewManagerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class AppRater implements DefaultLifecycleObserver {
    private static final String TAG = "AppRater";
    private static final long COOLDOWN_MS = TimeUnit.DAYS.toMillis(90);
    // A session lasts for this app process, including activity recreation.
    private static boolean mAttemptedThisSession;

    private final AppCompatActivity mActivity;
    private final SharedPreferences mPrefs;
    private boolean mBusy;
    private boolean mRequestPending;

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

    public void maybeRequestReview(Runnable onFinished) {
        long lastAttempt = mPrefs.getLong("last_review_attempt", 0);
        if (mAttemptedThisSession || mBusy || mPrefs.getInt("scan_counter", 0) < 5
                || mPrefs.getInt("scan_days", 0) < 2
                || (lastAttempt != 0 && System.currentTimeMillis() - lastAttempt < COOLDOWN_MS)
                || !canLaunch()) return;

        mAttemptedThisSession = true;
        mBusy = true;
        mRequestPending = true;
        Log.d(TAG, "Requesting review eligibility from Play");
        ReviewManager manager = ReviewManagerFactory.create(mActivity.getApplicationContext());
        manager.requestReviewFlow().addOnCompleteListener(request -> {
            // Leaving cancels the opportunity, even if the user returns quickly.
            if (!mRequestPending) return;
            mRequestPending = false;
            if (!request.isSuccessful() || !canLaunch()) {
                if (!request.isSuccessful()) Log.w(TAG, "Review request failed", request.getException());
                mBusy = false;
                onFinished.run();
                return;
            }
            mPrefs.edit().putLong("last_review_attempt", System.currentTimeMillis()).apply();
            Log.d(TAG, "Launching review flow (display and submission are not observable)");
            manager.launchReviewFlow(mActivity, request.getResult()).addOnCompleteListener(flow -> {
                if (!flow.isSuccessful()) Log.w(TAG, "Review launch failed", flow.getException());
                mBusy = false;
                onFinished.run();
            });
        });
    }

    private boolean canLaunch() {
        return !mActivity.isFinishing() && !mActivity.isDestroyed()
                && mActivity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (mRequestPending) {
            mRequestPending = false;
            mBusy = false;
        }
    }
}
