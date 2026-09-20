package com.auroid.qrscanner;

import com.auroid.qrscanner.serializable.*;
import com.google.gson.Gson;
import com.google.mlkit.vision.barcode.common.Barcode;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResultContentTest {
    @Test public void oldEmailHistoryPreservesPlusAndDecodesFields() {
        BarcodeWrapper b = new Gson().fromJson("{\"valueFormat\":2,\"rawValue\":"
                + "\"mailto:person+tag@example.org?subject=Hello%20there&body=A%26B%0ASecond%20line\"}", BarcodeWrapper.class);
        ResultContent.restoreMessageFields(b);
        assertEquals("person+tag@example.org", b.recipient);
        assertEquals("Hello there", b.subject);
        assertEquals("A&B\nSecond line", b.message);
        b.message = "saved parsed message";
        ResultContent.restoreMessageFields(b);
        assertEquals("saved parsed message", b.message);
    }

    @Test public void matmsgUnescapesDelimitersWithoutSplittingBody() {
        BarcodeWrapper b = barcode(Barcode.TYPE_EMAIL, "MATMSG:TO:a@example.org;SUB:Hi;BODY:One\\; two\\\\three;;");
        ResultContent.restoreMessageFields(b);
        assertEquals("a@example.org", b.recipient);
        assertEquals("One; two\\three", b.message);
    }

    @Test public void smsHistoryRetainsInternationalNumberAndFullMessage() {
        BarcodeWrapper b = barcode(Barcode.TYPE_SMS, "SMSTO:+14165550123:Time: 10:30 + 1");
        ResultContent.restoreMessageFields(b);
        assertEquals("+14165550123", b.recipient);
        assertEquals("Time: 10:30 + 1", b.message);
        b = barcode(Barcode.TYPE_SMS, "sms:%2B14165550123?body=Hello%0Athere");
        ResultContent.restoreMessageFields(b);
        assertEquals("+14165550123", b.recipient);
        assertEquals("Hello\nthere", b.message);
    }

    @Test public void sparseContactPreservesAllAddressLinesAndOriginalPhone() {
        ContactWrapper c = new ContactWrapper(null, null, null, null,
                new PhoneWrapper[]{null, new PhoneWrapper("+44 20 1234 5678", 0)}, null,
                new AddressWrapper[]{null, new AddressWrapper(new String[]{"10 Main St", null, "London"}, 0)});
        String details = ResultContent.contactDetails(c);
        assertTrue(details.contains("+44 20 1234 5678"));
        assertTrue(details.contains("10 Main St\nLondon"));
        assertFalse(details.contains("null"));
        assertFalse(details.contains("Company:"));
    }

    @Test public void eventWithoutEndOrOptionalFieldsIsReadableAndActionable() {
        BarcodeWrapper b = barcode(Barcode.TYPE_CALENDAR_EVENT, "event");
        b.eventWrapper = new EventWrapper(null, null, null, null, "Meeting", "2026/09/20 10:00", null);
        assertTrue(ResultActions.forBarcode(b).contains(ResultActions.Action.CALENDAR));
        assertFalse(ResultContent.eventDetails(b.eventWrapper).contains("null"));
        assertFalse(ResultContent.eventDetails(b.eventWrapper).contains("End:"));
        b.eventWrapper.start = "2026/02/30 10:00";
        assertFalse(ResultActions.forBarcode(b).contains(ResultActions.Action.CALENDAR));
    }

    @Test public void wifiSummaryNeverFallsBackToCredentialPayload() {
        BarcodeWrapper b = barcode(Barcode.TYPE_WIFI, "WIFI:S:Test;P:secret;;");
        assertFalse(ResultContent.summary(b).contains("secret"));
        b.wifiWrapper = new WiFiWrapper(Barcode.WiFi.TYPE_OPEN, "secret", "Test");
        assertEquals("Test", ResultContent.summary(b));
        assertFalse(ResultActions.forBarcode(b).contains(ResultActions.Action.COPY_PASSWORD));
    }

    @Test public void structuredTypesNeverOfferWholePayloadSearchOrCopy() {
        for (int type : new int[]{Barcode.TYPE_URL, Barcode.TYPE_PHONE, Barcode.TYPE_GEO,
                Barcode.TYPE_CONTACT_INFO, Barcode.TYPE_CALENDAR_EVENT, Barcode.TYPE_EMAIL, Barcode.TYPE_SMS, Barcode.TYPE_WIFI}) {
            java.util.List<ResultActions.Action> actions = ResultActions.forBarcode(barcode(type, ""));
            assertTrue(actions.size() <= 3);
            assertFalse(actions.contains(ResultActions.Action.SEARCH));
            assertFalse(actions.contains(ResultActions.Action.COPY));
        }
        for (int type : new int[]{Barcode.TYPE_PRODUCT, Barcode.TYPE_ISBN, Barcode.TYPE_TEXT}) {
            assertEquals(java.util.Arrays.asList(ResultActions.Action.COPY, ResultActions.Action.SEARCH),
                    ResultActions.forBarcode(barcode(type, "123")));
        }
    }

    private BarcodeWrapper barcode(int type, String raw) { return new BarcodeWrapper(type, raw, raw); }
}
