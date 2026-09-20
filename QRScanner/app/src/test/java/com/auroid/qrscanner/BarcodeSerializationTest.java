package com.auroid.qrscanner;

import com.auroid.qrscanner.serializable.BarcodeWrapper;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BarcodeSerializationTest {
    @Test
    public void existingHistoryJsonRemainsReadableAfterGsonUpgrade() {
        String savedResult = "{\"valueFormat\":9,\"displayValue\":\"Example WiFi\","
                + "\"rawValue\":\"WIFI:S:Example;T:WPA;P:secret;;\","
                + "\"wifiWrapper\":{\"encryptionType\":2,\"password\":\"secret\","
                + "\"ssid\":\"Example\"}}";
        Gson gson = new Gson();
        BarcodeWrapper barcode = gson.fromJson(savedResult, BarcodeWrapper.class);

        assertEquals(9, barcode.valueFormat);
        org.junit.Assert.assertNull(barcode.barcodeFormat);
        assertNotNull(barcode.wifiWrapper);
        assertEquals("Example", barcode.wifiWrapper.ssid);
        assertEquals("secret", barcode.wifiWrapper.password);
        assertEquals(JsonParser.parseString(savedResult),
                JsonParser.parseString(gson.toJson(barcode)));
    }

    @Test
    public void barcodeFormatSurvivesHistoryRoundTrip() {
        BarcodeWrapper barcode = new BarcodeWrapper(8, "example", "example");
        barcode.barcodeFormat = 256;
        Gson gson = new Gson();
        assertEquals(Integer.valueOf(256), gson.fromJson(gson.toJson(barcode),
                BarcodeWrapper.class).barcodeFormat);
    }
}
