package com.auroid.qrscanner.wifi;

import com.auroid.qrscanner.serializable.WiFiWrapper;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.nio.charset.StandardCharsets;

/** Validation shared by the system save flow and the legacy Wi-Fi configuration. */
public final class WifiCredentials {

    public enum Problem {
        NONE,
        INVALID_SSID,
        INVALID_PASSWORD,
        UNSUPPORTED_SECURITY
    }

    private WifiCredentials() {}

    public static Problem validate(WiFiWrapper wifi) {
        if (wifi == null || wifi.ssid == null || wifi.ssid.isEmpty()
                || !StandardCharsets.UTF_8.newEncoder().canEncode(wifi.ssid)
                || wifi.ssid.getBytes(StandardCharsets.UTF_8).length > 32
                || wifi.ssid.indexOf('\0') >= 0) {
            return Problem.INVALID_SSID;
        }
        switch (wifi.encryptionType) {
            case Barcode.WiFi.TYPE_OPEN:
                return Problem.NONE;
            case Barcode.WiFi.TYPE_WPA:
                return isRawPsk(wifi.password) || (isPrintableAscii(wifi.password)
                        && wifi.password.length() >= 8 && wifi.password.length() <= 63)
                        ? Problem.NONE : Problem.INVALID_PASSWORD;
            case Barcode.WiFi.TYPE_WEP:
                return isHexWepKey(wifi.password) || (isPrintableAscii(wifi.password)
                        && (wifi.password.length() == 5 || wifi.password.length() == 13))
                        ? Problem.NONE : Problem.INVALID_PASSWORD;
            default:
                return Problem.UNSUPPORTED_SECURITY;
        }
    }

    static boolean isRawPsk(String password) {
        return isHex(password, 64);
    }

    static boolean isHexWepKey(String password) {
        return isHex(password, 10) || isHex(password, 26);
    }

    private static boolean isHex(String value, int length) {
        return value != null && value.length() == length && value.matches("[0-9a-fA-F]+");
    }

    private static boolean isPrintableAscii(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < 32 || value.charAt(i) > 126) {
                return false;
            }
        }
        return true;
    }

    /** WifiConfiguration uses enclosing quotes, not backslash-escaped string literals. */
    static String quoteForLegacyConfig(String value) {
        return '"' + value + '"';
    }
}
