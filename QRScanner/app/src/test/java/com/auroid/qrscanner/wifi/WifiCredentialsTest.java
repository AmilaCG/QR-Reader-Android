package com.auroid.qrscanner.wifi;

import com.auroid.qrscanner.serializable.WiFiWrapper;
import com.google.mlkit.vision.barcode.common.Barcode;

import org.junit.Test;

import static com.auroid.qrscanner.wifi.WifiCredentials.Problem.*;
import static org.junit.Assert.*;

public class WifiCredentialsTest {
    @Test
    public void rejectsMissingPayloadAndInvalidSsid() {
        assertEquals(INVALID_SSID, WifiCredentials.validate(null));
        for (String ssid : new String[]{null, "", "a".repeat(33), "bad\0ssid", "\ud800"}) {
            assertEquals(INVALID_SSID, WifiCredentials.validate(
                    new WiFiWrapper(Barcode.WiFi.TYPE_OPEN, null, ssid)));
        }
    }

    @Test
    public void ssidLimitUsesUtf8BytesWithoutTrimming() {
        assertEquals(NONE, WifiCredentials.validate(
                new WiFiWrapper(Barcode.WiFi.TYPE_OPEN, null, "é".repeat(16))));
        assertEquals(INVALID_SSID, WifiCredentials.validate(
                new WiFiWrapper(Barcode.WiFi.TYPE_OPEN, null, "é".repeat(17))));
        assertEquals(NONE, WifiCredentials.validate(
                new WiFiWrapper(Barcode.WiFi.TYPE_OPEN, null, " ")));
    }

    @Test
    public void openNetworkDoesNotRequirePassword() {
        assertEquals(NONE, validate(Barcode.WiFi.TYPE_OPEN, null));
        assertEquals(NONE, validate(Barcode.WiFi.TYPE_OPEN, ""));
    }

    @Test
    public void wpaAcceptsPassphraseBoundariesAndLiteralWhitespace() {
        for (String password : new String[]{"12345678", "x".repeat(63), "        ", "a\"b\\cdef"}) {
            assertEquals(NONE, validate(Barcode.WiFi.TYPE_WPA, password));
        }
    }

    @Test
    public void wpaRejectsMissingShortNonAsciiAndNonHex64CharacterPasswords() {
        for (String password : new String[]{null, "", "1234567", "pässword", "abcd\nefgh",
                "z".repeat(64), "a".repeat(65)}) {
            assertEquals(INVALID_PASSWORD, validate(Barcode.WiFi.TYPE_WPA, password));
        }
    }

    @Test
    public void rawPskIsAcceptedForLegacyAndDistinguishedFromPassphrase() {
        String key = "aB01".repeat(16);
        assertEquals(NONE, validate(Barcode.WiFi.TYPE_WPA, key));
        assertTrue(WifiCredentials.isRawPsk(key));
        assertFalse(WifiCredentials.isRawPsk("password"));
        assertFalse(WifiCredentials.isRawPsk(null));
    }

    @Test
    public void wepAcceptsAsciiAndHexKeysButRejectsOtherLengths() {
        for (String password : new String[]{"abcde", "abcdefghijklm", "012345AbCd",
                "0123456789abcdef0123456789"}) {
            assertEquals(NONE, validate(Barcode.WiFi.TYPE_WEP, password));
        }
        for (String password : new String[]{null, "", "abcd", "password", "abcdefghij", "éabcd"}) {
            assertEquals(INVALID_PASSWORD, validate(Barcode.WiFi.TYPE_WEP, password));
        }
    }

    @Test
    public void unknownSecurityIsNotTreatedAsOpenNetwork() {
        assertEquals(UNSUPPORTED_SECURITY, validate(0, "password"));
        assertEquals(UNSUPPORTED_SECURITY, validate(999, "password"));
    }

    @Test
    public void legacyQuotingPreservesCharactersAfterRemovingEnclosingQuotes() {
        for (String value : new String[]{"Café \"Q\"\\", "\\".repeat(32), "\"".repeat(63)}) {
            String quoted = WifiCredentials.quoteForLegacyConfig(value);
            assertEquals('"', quoted.charAt(0));
            assertEquals('"', quoted.charAt(quoted.length() - 1));
            assertEquals(value, quoted.substring(1, quoted.length() - 1));
        }
    }

    private WifiCredentials.Problem validate(int encryption, String password) {
        return WifiCredentials.validate(new WiFiWrapper(encryption, password, "Test network"));
    }
}
