package com.auroid.qrscanner.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;

public class AppRater {

    private static final int DAYS_UNTIL_PROMPT = 3; //Min number of days
    private static final int LAUNCHES_UNTIL_PROMPT = 5; //Min number of launches
    private static final int SCANS_UNTIL_PROMPT = 5; //Min number of scans

    private static final String SHARED_PREF_NAME = "apprater";
    private static final String LAUNCH_COUNT_KEY = "launch_counter";
    private static final String SCAN_COUNT_KEY = "scan_counter";
    private static final String FIRST_LAUNCH_DATE_KEY = "first_launch_date";

    private static boolean shouldLaunchReviewFlow = false;
    private static SharedPreferences.Editor mEditor;
    private static int mScanCount;

    public static void appLaunched(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        mEditor = prefs.edit();

        // Increment launch counter
        int launchCount = prefs.getInt(LAUNCH_COUNT_KEY, 0) + 1;
        mEditor.putInt(LAUNCH_COUNT_KEY, launchCount);

        // Get date of first launch
        long firstLaunchDate = prefs.getLong(FIRST_LAUNCH_DATE_KEY, 0);
        if (firstLaunchDate == 0) {
            firstLaunchDate = System.currentTimeMillis();
            mEditor.putLong(FIRST_LAUNCH_DATE_KEY, firstLaunchDate);
        }

        mScanCount = prefs.getInt(SCAN_COUNT_KEY, 0);

        // Wait at least n days before opening
        if (launchCount >= LAUNCHES_UNTIL_PROMPT && mScanCount >= SCANS_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= firstLaunchDate +
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                shouldLaunchReviewFlow = true;
            }
        }

        mEditor.apply();
    }

    public static void showRateDialog(Activity activity) {
        if (!shouldLaunchReviewFlow) {
            // Increment scan counter
            mScanCount ++;
            if (mEditor != null) {
                mEditor.putInt(SCAN_COUNT_KEY, mScanCount).apply();
            }
            return;
        }
        ReviewManager manager = ReviewManagerFactory.create(activity.getApplicationContext());
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();

                Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                flow.addOnCompleteListener(launchTask -> {
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                    // Reset saved preferences
                    if (mEditor != null) {
                        mEditor.clear().apply();
                    }
                    shouldLaunchReviewFlow = false;
                });
            }
        });
    }
}
