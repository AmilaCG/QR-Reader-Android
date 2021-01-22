package com.auroid.qrscanner;

import android.app.Application;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;

public class App extends Application {

    private AdView adView;

    @Override
    public void onCreate() {
        super.onCreate();

        // Use this IMG_16_9_APP_INSTALL# in front of the placement id to test ads
        adView = new AdView(this, "3384360194976084_3408475895897847", AdSize.BANNER_HEIGHT_50);
        adView.loadAd();
    }

    public void loadAd(LinearLayout adContainer) {
        // Locate the Banner Ad in activity xml
        if (adView.getParent() != null) {
            ViewGroup tempVg = (ViewGroup) adView.getParent();
            tempVg.removeView(adView);
        }

        adContainer.addView(adView);
    }
}
