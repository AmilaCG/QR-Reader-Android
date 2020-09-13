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
    private static final int LAUNCHES_UNTIL_PROMPT = 3; //Min number of launches

    private static ReviewInfo mReviewInfo;

    public static void app_launched(Activity activity) {
        SharedPreferences prefs = activity.getApplicationContext()
                .getSharedPreferences("apprater", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        long launchCount = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launchCount);

        // Get date of first launch
        long firstLaunchDate = prefs.getLong("date_firstlaunch", 0);
        if (firstLaunchDate == 0) {
            firstLaunchDate = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", firstLaunchDate);
        }

        // Wait at least n days before opening
        if (launchCount >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= firstLaunchDate +
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                showRateDialog(activity, editor);
            }
        }

        editor.apply();
    }

    private static void showRateDialog(Activity activity, SharedPreferences.Editor editor) {
        ReviewManager manager = ReviewManagerFactory.create(activity.getApplicationContext());
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                mReviewInfo = task.getResult();
            }
        });

        Task<Void> flow = manager.launchReviewFlow(activity, mReviewInfo);
        flow.addOnCompleteListener(task -> {
            // The flow has finished. The API does not indicate whether the user
            // reviewed or not, or even whether the review dialog was shown. Thus, no
            // matter the result, we continue our app flow.
            if (editor != null) {
                editor.clear().apply();
            }
        });
    }
}
