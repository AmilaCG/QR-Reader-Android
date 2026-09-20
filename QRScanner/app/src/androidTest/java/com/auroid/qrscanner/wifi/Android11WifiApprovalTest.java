package com.auroid.qrscanner.wifi;

import android.accessibilityservice.AccessibilityService;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.auroid.qrscanner.BarcodeResultActivity;
import com.auroid.qrscanner.R;
import com.auroid.qrscanner.serializable.BarcodeWrapper;
import com.auroid.qrscanner.serializable.WiFiWrapper;
import com.google.gson.Gson;
import com.google.mlkit.vision.barcode.common.Barcode;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.function.BooleanSupplier;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

/** Exercises the actual Settings confirmation, without saving a network on the device. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 30, maxSdkVersion = 30)
public class Android11WifiApprovalTest {
    @Test
    public void systemShowsScannedNetworkAndCancellationReturnsToResult() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        BarcodeWrapper barcode = new BarcodeWrapper(Barcode.TYPE_WIFI, "QRTestApproval", "WIFI:S:QRTestApproval;;");
        barcode.wifiWrapper = new WiFiWrapper(Barcode.WiFi.TYPE_OPEN, null, "QRTestApproval");
        Intent intent = new Intent(context, BarcodeResultActivity.class)
                .putExtra("RESULT", new Gson().toJson(barcode))
                .putExtra("FORMAT", Barcode.FORMAT_QR_CODE);
        try (ActivityScenario<BarcodeResultActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.ib_action)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> activity.findViewById(com.auroid.qrscanner.R.id.ib_action).performClick());
            await("System Wi-Fi approval did not display the scanned SSID", () -> {
                AccessibilityNodeInfo root = automation.getRootInActiveWindow();
                return root != null && "com.android.settings".contentEquals(root.getPackageName())
                        && !root.findAccessibilityNodeInfosByText("QRTestApproval").isEmpty();
            });
            assertTrue(automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK));
            await("Cancellation did not return to the app", () -> {
                AccessibilityNodeInfo root = automation.getRootInActiveWindow();
                return root != null && context.getPackageName().contentEquals(root.getPackageName());
            });
            onView(withId(R.id.ib_action)).check(matches(isDisplayed()));
            onView(withText(R.string.wifi_network_saved)).check(doesNotExist());
            onView(withText(R.string.wifi_save_failed)).check(doesNotExist());
        }
    }

    private void await(String message, BooleanSupplier condition) {
        long deadline = SystemClock.elapsedRealtime() + 15000;
        while (!condition.getAsBoolean() && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(100);
        }
        assertTrue(message, condition.getAsBoolean());
    }
}
