package com.auroid.qrscanner.wifi;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.provider.Settings;

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

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WifiConnectionFlowTest {
    @Test
    public void malformedScanShowsErrorWithoutCrashing() {
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(
                new WiFiWrapper(Barcode.WiFi.TYPE_WPA, "short", "Test"))) {
            onView(withId(R.id.ib_action)).perform(click());
            onView(withText(R.string.wifi_invalid_password)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void missingWifiPayloadCanBeDisplayedAndRejected() {
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(null)) {
            onView(withId(R.id.ib_action)).perform(click());
            onView(withText(R.string.wifi_invalid_ssid)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void historyResolvesWrappedContextAndOffersSeparatePasswordCopy() {
        try (ActivityScenario<ScanHistoryActivity> scenario = ActivityScenario.launch(ScanHistoryActivity.class)) {
            onView(withId(R.id.top_action_title)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> new ActionHandler(new ContextWrapper(activity),
                    barcode(new WiFiWrapper(999, "test-secret", "Test"))).connectToWifi());
            onView(withText(R.string.wifi_unsupported_security)).inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withText(R.string.wifi_copy_password)).inRoot(isDialog()).perform(click());
            onView(withText(R.string.wifi_open_settings)).inRoot(isDialog()).check(matches(isDisplayed()));
            scenario.onActivity(activity -> {
                ClipboardManager clipboard = activity.getSystemService(ClipboardManager.class);
                assertNotNull(clipboard.getPrimaryClip());
                assertEquals("Clipboard source: " + clipboard.getPrimaryClipDescription().getLabel(),
                        "test-secret", clipboard.getPrimaryClip().getItemAt(0).getText().toString());
                // Android 13+ uses this flag to hide the password in clipboard previews.
                // Older clipboard services may discard the extras when copying the description.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    assertNotNull(clipboard.getPrimaryClipDescription().getExtras());
                    assertTrue(clipboard.getPrimaryClipDescription().getExtras()
                            .getBoolean("android.content.extra.IS_SENSITIVE"));
                }
            });
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void openNetworkLaunchesSystemSaveAndReportsSaved() {
        assertSaveResult(Barcode.WiFi.TYPE_OPEN, Activity.RESULT_OK,
                Settings.ADD_WIFI_RESULT_SUCCESS, R.string.wifi_network_saved);
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void wpaNetworkReportsExistingConfiguration() {
        assertSaveResult(Barcode.WiFi.TYPE_WPA, Activity.RESULT_OK,
                Settings.ADD_WIFI_RESULT_ALREADY_EXISTS, R.string.wifi_network_exists);
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void rejectedSaveShowsFailure() {
        assertSaveResult(Barcode.WiFi.TYPE_WPA, Activity.RESULT_OK,
                Settings.ADD_WIFI_RESULT_ADD_OR_UPDATE_FAILED, R.string.wifi_save_failed);
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void missingSaveResultDoesNotClaimSuccess() {
        assertSaveResult(Barcode.WiFi.TYPE_WPA, Activity.RESULT_OK, null, R.string.wifi_save_failed);
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void cancellationDoesNotShowSavedOrFailureDialog() {
        assertSaveResult(Barcode.WiFi.TYPE_WPA, Activity.RESULT_CANCELED, null, 0);
    }

    @SdkSuppress(minSdkVersion = 30)
    private void assertSaveResult(int security, int resultCode, Integer networkResult, int expectedMessage) {
        Intent data = new Intent();
        if (networkResult != null) {
            data.putIntegerArrayListExtra(Settings.EXTRA_WIFI_NETWORK_RESULT_LIST,
                    new ArrayList<>(Collections.singletonList(networkResult)));
        }
        AtomicReference<Intent> launched = new AtomicReference<>();
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override
            public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                if (Settings.ACTION_WIFI_ADD_NETWORKS.equals(intent.getAction())) {
                    launched.set(intent);
                    return new Instrumentation.ActivityResult(resultCode, data);
                }
                return null;
            }
        };
        instrumentation.addMonitor(monitor);
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(new WiFiWrapper(
                security, security == Barcode.WiFi.TYPE_OPEN ? null : "password123", "Test"))) {
            onView(withId(R.id.ib_action)).perform(click());
            if (expectedMessage != 0) {
                onView(withText(expectedMessage)).check(matches(isDisplayed()));
            } else {
                onView(withText(R.string.wifi_network_saved)).check(doesNotExist());
                onView(withText(R.string.wifi_save_failed)).check(doesNotExist());
            }
            assertNotNull(launched.get());
            ArrayList<WifiNetworkSuggestion> networks = launched.get()
                    .getParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST);
            assertNotNull(networks);
            assertEquals(1, networks.size());
            assertEquals("Test", networks.get(0).getSsid());
            assertEquals(security == Barcode.WiFi.TYPE_OPEN ? null : "password123",
                    networks.get(0).getPassphrase());
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    private ActivityScenario<BarcodeResultActivity> scan(WiFiWrapper wifi) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return ActivityScenario.launch(new Intent(context, BarcodeResultActivity.class)
                .putExtra("RESULT", new Gson().toJson(barcode(wifi)))
                .putExtra("FORMAT", Barcode.FORMAT_QR_CODE));
    }

    private BarcodeWrapper barcode(WiFiWrapper wifi) {
        BarcodeWrapper result = new BarcodeWrapper(Barcode.TYPE_WIFI, "Test", "WIFI:S:Test;;");
        result.wifiWrapper = wifi;
        return result;
    }
}
