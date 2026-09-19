package com.auroid.qrscanner.wifi;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.ParcelFileDescriptor;
import android.os.Process;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.auroid.qrscanner.ActionHandler;
import com.auroid.qrscanner.BarcodeResultActivity;
import com.auroid.qrscanner.R;
import com.auroid.qrscanner.ScanHistoryActivity;
import com.auroid.qrscanner.serializable.BarcodeWrapper;
import com.auroid.qrscanner.serializable.WiFiWrapper;
import com.google.gson.Gson;
import com.google.mlkit.vision.barcode.common.Barcode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

/** Uses the real suggestion service; temporary SSIDs are never broadcast by an access point. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29, maxSdkVersion = 29)
public class Android10WifiSuggestionTest {

    private Context mContext;
    private WifiManager mManager;
    private String mSsid;
    private WifiNetworkSuggestion mSuggestion;
    private boolean mChangedAppOp;
    private String mOriginalAppOp;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mManager = mContext.getApplicationContext().getSystemService(WifiManager.class);
        mSsid = "QRTest-" + UUID.randomUUID().toString().substring(0, 12);
        assertNotNull(mManager);
    }

    @After
    public void cleanUp() throws Exception {
        if (mChangedAppOp) {
            setWifiAppOp(mOriginalAppOp);
        }
        if (mSuggestion != null) {
            mManager.removeNetworkSuggestions(Collections.singletonList(mSuggestion));
        }
    }

    @Test
    public void scanSubmitsOpenNetworkSuggestion() {
        WiFiWrapper wifi = wifi(Barcode.WiFi.TYPE_OPEN, null);
        mSuggestion = WifiConnectionHelper.createSuggestion(wifi);
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(wifi)) {
            onView(withId(R.id.ib_action)).perform(click());
            message(R.string.wifi_suggestion_submitted);
            // A duplicate status confirms that the first request reached the real service.
            assertEquals(WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE,
                    mManager.addNetworkSuggestions(Collections.singletonList(mSuggestion)));
        }
    }

    @Test
    public void historySubmitsWpaNetworkSuggestion() {
        WiFiWrapper wifi = wifi(Barcode.WiFi.TYPE_WPA, "password123");
        mSuggestion = WifiConnectionHelper.createSuggestion(wifi);
        try (ActivityScenario<ScanHistoryActivity> scenario = ActivityScenario.launch(ScanHistoryActivity.class)) {
            onView(withId(R.id.top_action_title)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> new ActionHandler(activity, barcode(wifi)).connectToWifi());
            message(R.string.wifi_suggestion_submitted);
            assertEquals(WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE,
                    mManager.addNetworkSuggestions(Collections.singletonList(mSuggestion)));
        }
    }

    @Test
    public void repeatingConnectExplainsDuplicateSuggestion() {
        WiFiWrapper wifi = wifi(Barcode.WiFi.TYPE_WPA, "password123");
        mSuggestion = WifiConnectionHelper.createSuggestion(wifi);
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(wifi)) {
            onView(withId(R.id.ib_action)).perform(click());
            message(R.string.wifi_suggestion_submitted);
            onView(withText(R.string.cancel)).inRoot(isDialog()).perform(click());
            onView(withId(R.id.ib_action)).perform(click());
            message(R.string.wifi_suggestion_exists);
        }
    }

    @Test
    public void deniedWifiControlShowsSettingsFallback() throws Exception {
        AppOpsManager appOps = mContext.getSystemService(AppOpsManager.class);
        int mode = appOps.unsafeCheckOpNoThrow("android:change_wifi_state", Process.myUid(), mContext.getPackageName());
        switch (mode) {
            case AppOpsManager.MODE_ALLOWED:
                mOriginalAppOp = "allow";
                break;
            case AppOpsManager.MODE_IGNORED:
                mOriginalAppOp = "ignore";
                break;
            case AppOpsManager.MODE_ERRORED:
                mOriginalAppOp = "deny";
                break;
            default:
                mOriginalAppOp = "default";
        }
        mChangedAppOp = true;
        setWifiAppOp("ignore");
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(wifi(Barcode.WiFi.TYPE_WPA, "password123"))) {
            onView(withId(R.id.ib_action)).perform(click());
            message(R.string.wifi_permission_denied);
            onView(withText(R.string.wifi_open_settings)).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void wepShowsManualConnectionFallback() {
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(wifi(Barcode.WiFi.TYPE_WEP, "abcde"))) {
            onView(withId(R.id.ib_action)).perform(click());
            message(R.string.wifi_unsupported_security);
            onView(withText(R.string.wifi_copy_password)).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    private void setWifiAppOp(String mode) throws Exception {
        ParcelFileDescriptor output = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("cmd appops set " + mContext.getPackageName() + " CHANGE_WIFI_STATE " + mode);
        try (InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(output)) {
            byte[] buffer = new byte[1024];
            while (stream.read(buffer) != -1) {
                // Wait for the command to finish.
            }
        }
    }

    private void message(int resource) {
        onView(withText(resource)).inRoot(isDialog()).check(matches(isDisplayed()));
    }

    private WiFiWrapper wifi(int security, String password) {
        return new WiFiWrapper(security, password, mSsid);
    }

    private BarcodeWrapper barcode(WiFiWrapper wifi) {
        BarcodeWrapper barcode = new BarcodeWrapper(Barcode.TYPE_WIFI, mSsid, "WIFI:S:" + mSsid + ";;");
        barcode.wifiWrapper = wifi;
        return barcode;
    }

    private ActivityScenario<BarcodeResultActivity> scan(WiFiWrapper wifi) {
        return ActivityScenario.launch(new Intent(mContext, BarcodeResultActivity.class)
                .putExtra("RESULT", new Gson().toJson(barcode(wifi)))
                .putExtra("FORMAT", Barcode.FORMAT_QR_CODE));
    }
}
