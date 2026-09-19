package com.auroid.qrscanner.wifi;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.SystemClock;

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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Exercises the actual legacy Wi-Fi service using temporary, non-broadcast test SSIDs. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(maxSdkVersion = 28)
@SuppressWarnings("deprecation")
public class LegacyWifiConnectionTest {

    private WifiManager mManager;
    private boolean mInitiallyEnabled;
    private int mOriginalNetworkId;
    private String mSsid;
    private final Set<Integer> mOriginalNetworkIds = new HashSet<>();

    @Before
    public void rememberWifiState() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assumeTrue("This device does not provide Wi-Fi hardware",
                context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI));
        mManager = context.getApplicationContext().getSystemService(WifiManager.class);
        assertNotNull(mManager);
        mInitiallyEnabled = mManager.isWifiEnabled();
        mOriginalNetworkId = mManager.getConnectionInfo().getNetworkId();
        mSsid = "QRTest-" + UUID.randomUUID().toString().substring(0, 12);
        assertTrue(mManager.setWifiEnabled(true));
        await("Wi-Fi did not enable for test setup", mManager::isWifiEnabled);
        List<WifiConfiguration> networks = mManager.getConfiguredNetworks();
        assertNotNull(networks);
        for (WifiConfiguration network : networks) {
            mOriginalNetworkIds.add(network.networkId);
        }
    }

    @After
    public void restoreWifiState() {
        if (mManager == null) {
            return;
        }
        try {
            removeTestNetwork();
        } finally {
            if (mOriginalNetworkId >= 0) {
                mManager.enableNetwork(mOriginalNetworkId, true);
            }
            assertTrue(mManager.setWifiEnabled(mInitiallyEnabled));
            await("Wi-Fi state was not restored", () -> mManager.isWifiEnabled() == mInitiallyEnabled);
        }
    }

    @Test
    public void scanSubmitsOpenNetworkToWifiService() {
        submitFromScan(Barcode.WiFi.TYPE_OPEN, null, false);
        assertTrue(findTestNetwork().allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE));
    }

    @Test
    public void scanSubmitsWpaNetworkToWifiService() {
        submitFromScan(Barcode.WiFi.TYPE_WPA, "password123", false);
        assertTrue(findTestNetwork().allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK));
    }

    @Test
    public void scanPreservesLiteralSsidCharactersInWifiService() {
        mSsid += "\"\\";
        submitFromScan(Barcode.WiFi.TYPE_OPEN, null, false);
        assertEquals('"' + mSsid + '"', findTestNetwork().SSID);
    }

    @Test
    public void scanTurnsWifiOnBeforeSubmittingNetwork() {
        submitFromScan(Barcode.WiFi.TYPE_OPEN, null, true);
        assertTrue(mManager.isWifiEnabled());
    }

    @Test
    public void historySubmitsWepNetworkToWifiService() {
        try (ActivityScenario<ScanHistoryActivity> scenario = ActivityScenario.launch(ScanHistoryActivity.class)) {
            onView(withId(R.id.top_action_title)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> new ActionHandler(activity,
                    barcode(Barcode.WiFi.TYPE_WEP, "abcde")).connectToWifi());
            assertRequestAccepted();
            WifiConfiguration config = findTestNetwork();
            assertTrue(config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE));
            assertNotNull(config.wepKeys[0]);
        }
    }

    private void submitFromScan(int security, String password, boolean turnWifiOff) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, BarcodeResultActivity.class)
                .putExtra("RESULT", new Gson().toJson(barcode(security, password)))
                .putExtra("FORMAT", Barcode.FORMAT_QR_CODE);
        try (ActivityScenario<BarcodeResultActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.ib_action)).check(matches(isDisplayed()));
            if (turnWifiOff) {
                assertTrue(mManager.setWifiEnabled(false));
                await("Wi-Fi did not disable", () -> mManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED);
            }
            onView(withId(R.id.ib_action)).perform(click());
            assertRequestAccepted();
        }
    }

    private void assertRequestAccepted() {
        await("The test network was not added", () -> findTestNetwork() != null);
        onView(withText(R.string.wifi_connection_requested)).inRoot(isDialog()).check(matches(isDisplayed()));
    }

    private WifiConfiguration findTestNetwork() {
        List<WifiConfiguration> networks = mManager.getConfiguredNetworks();
        if (networks == null) {
            return null;
        }
        for (WifiConfiguration network : networks) {
            if (WifiCredentials.quoteForLegacyConfig(mSsid).equals(network.SSID)) {
                return network;
            }
        }
        return null;
    }

    private void removeTestNetwork() {
        List<WifiConfiguration> networks = mManager.getConfiguredNetworks();
        if (networks == null) {
            return;
        }
        String currentSsid = mSsid == null ? null : WifiCredentials.quoteForLegacyConfig(mSsid);
        boolean foundCurrentNetwork = false;
        for (WifiConfiguration network : networks) {
            if (currentSsid != null && currentSsid.equals(network.SSID)
                    && !mOriginalNetworkIds.contains(network.networkId)) {
                foundCurrentNetwork = true;
                boolean removed = mManager.removeNetwork(network.networkId);
                assertTrue("Failed to remove current temporary network " + network.networkId, removed);
            }
        }
        if (foundCurrentNetwork) {
            mManager.saveConfiguration();
            await("Current temporary network was not removed", () -> findTestNetwork() == null);
        }
    }

    private BarcodeWrapper barcode(int security, String password) {
        BarcodeWrapper result = new BarcodeWrapper(Barcode.TYPE_WIFI, mSsid, "WIFI:S:" + mSsid + ";;");
        result.wifiWrapper = new WiFiWrapper(security, password, mSsid);
        return result;
    }

    private void await(String failure, BooleanSupplier condition) {
        long deadline = SystemClock.elapsedRealtime() + 20000;
        while (!condition.getAsBoolean() && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(100);
        }
        assertTrue(failure, condition.getAsBoolean());
    }
}
