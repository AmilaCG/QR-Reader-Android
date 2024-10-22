/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.auroid.qrscanner.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;

import androidx.preference.PreferenceManager;
import androidx.annotation.StringRes;

import com.auroid.qrscanner.R;
import com.auroid.qrscanner.camera.GraphicOverlay;

/** Utility class to retrieve shared preferences. */
public class PreferenceUtils {

    public static RectF getBarcodeReticleBox(GraphicOverlay overlay) {
        Context context = overlay.getContext();
        float overlayWidth = overlay.getWidth();
        float overlayHeight = overlay.getHeight();

        float boxEdgeLen;
        if (overlayWidth <= overlayHeight) {
            boxEdgeLen = overlayWidth * getCropPercentage(context) / 100;
        } else {
            boxEdgeLen = overlayHeight * getCropPercentage(context) / 100;
        }

        float cx = overlayWidth / 2;
        float cy = overlayHeight / 2;
        // Square reticle box
        float halfEdgeLen = boxEdgeLen / 2;
        return new RectF(
                cx - halfEdgeLen,
                cy - halfEdgeLen,
                cx + halfEdgeLen,
                cy + halfEdgeLen);
    }

    public static boolean shouldOpenDirectlyInBrowser(Context context) {
        return getBooleanPref(context, R.string.pref_key_open_browser, false);
    }

    public static boolean shouldPlayAudioBeep(Context context) {
        return getBooleanPref(context, R.string.pref_key_play_audio_beep, true);
    }

    public static int getCropPercentage(Context context) {
        return getIntPref(context, R.string.pref_key_crop_percentage, 65);
    }

    private static int getIntPref(Context context, @StringRes int prefKeyId, int defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(prefKeyId);
        return sharedPreferences.getInt(prefKey, defaultValue);
    }

    private static boolean getBooleanPref(
            Context context, @StringRes int prefKeyId, boolean defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefKey = context.getString(prefKeyId);
        return sharedPreferences.getBoolean(prefKey, defaultValue);
    }
}
