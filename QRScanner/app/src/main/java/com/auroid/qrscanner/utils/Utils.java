package com.auroid.qrscanner.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

/** Utility class to provide helper methods. */
public class Utils {

    static final int RC_PHOTO_LIBRARY = 26;

    private static final String TAG = "Utils";

    public static boolean isPortraitMode(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
    }

    public static void openImagePicker(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent, RC_PHOTO_LIBRARY);
    }
}
