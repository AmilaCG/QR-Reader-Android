package com.auroid.qrscanner.wifi;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiNetworkSuggestion;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import com.auroid.qrscanner.serializable.WiFiWrapper;
import com.google.mlkit.vision.barcode.common.Barcode;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class WifiConfigurationTest {
    @Test
    public void legacyOpenNetworkHasNoCredentialsAndUsesNoneKeyManagement() {
        WifiConfiguration config = legacy(Barcode.WiFi.TYPE_OPEN, null);
        assertEquals(1, config.allowedKeyManagement.cardinality());
        assertTrue(config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE));
        assertNull(config.preSharedKey);
        assertNull(config.wepKeys[0]);
        assertEquals("\"Test\"", config.SSID);
    }

    @Test
    public void legacyWpaQuotesPassphrasesAndPreservesLiteralCharacters() {
        WifiConfiguration config = legacy(Barcode.WiFi.TYPE_WPA, "abc\"def\\ghi");
        assertEquals(1, config.allowedKeyManagement.cardinality());
        assertTrue(config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK));
        assertEquals("\"abc\"def\\ghi\"", config.preSharedKey);
        assertNull(config.wepKeys[0]);
    }

    @Test
    public void legacySsidAndWepPreserveLiteralQuotesAndBackslashes() {
        String ssid = "Café \"Q\"\\";
        String key = "a\"b\\c";
        WifiConfiguration config = WifiConnectionHelper.createLegacyConfiguration(
                new WiFiWrapper(Barcode.WiFi.TYPE_WEP, key, ssid));
        assertEquals(ssid, config.SSID.substring(1, config.SSID.length() - 1));
        assertEquals(key, config.wepKeys[0].substring(1, config.wepKeys[0].length() - 1));
    }

    @Test
    public void legacyRawWpaKeyIsNotQuoted() {
        String rawKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        assertEquals(rawKey, legacy(Barcode.WiFi.TYPE_WPA, rawKey).preSharedKey);
    }

    @Test
    public void legacyWepUsesWepFieldsAndDistinguishesHexKeys() {
        WifiConfiguration ascii = legacy(Barcode.WiFi.TYPE_WEP, "abcde");
        assertEquals("\"abcde\"", ascii.wepKeys[0]);
        assertEquals(0, ascii.wepTxKeyIndex);
        assertTrue(ascii.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE));
        assertTrue(ascii.allowedAuthAlgorithms.get(WifiConfiguration.AuthAlgorithm.OPEN));
        assertTrue(ascii.allowedAuthAlgorithms.get(WifiConfiguration.AuthAlgorithm.SHARED));
        assertNull(ascii.preSharedKey);
        assertEquals("012345abcd", legacy(Barcode.WiFi.TYPE_WEP, "012345abcd").wepKeys[0]);
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    public void modernOpenAndWpaSuggestionsHaveCorrectSecurityAndCredentials() {
        WifiNetworkSuggestion open = WifiConnectionHelper.createSuggestion(
                new WiFiWrapper(Barcode.WiFi.TYPE_OPEN, null, "Test"));
        assertEquals("Test", open.getSsid());
        assertNull(open.getPassphrase());
        WifiNetworkSuggestion wpa = WifiConnectionHelper.createSuggestion(
                new WiFiWrapper(Barcode.WiFi.TYPE_WPA, "password123", "Test"));
        assertEquals("Test", wpa.getSsid());
        assertEquals("password123", wpa.getPassphrase());
        assertFalse(open.equals(wpa));
    }

    private WifiConfiguration legacy(int security, String password) {
        return WifiConnectionHelper.createLegacyConfiguration(new WiFiWrapper(security, password, "Test"));
    }
}
