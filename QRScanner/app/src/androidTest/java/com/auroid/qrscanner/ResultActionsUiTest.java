package com.auroid.qrscanner;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.auroid.qrscanner.resultdb.Result;
import com.auroid.qrscanner.resultdb.ResultListAdapter;
import com.auroid.qrscanner.serializable.*;
import com.google.gson.Gson;
import com.google.mlkit.vision.barcode.common.Barcode;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class ResultActionsUiTest {
    @Test public void emailHistoryFallbackCopiesOnlyAddressAndBuildsDraft() {
        BarcodeWrapper b = new BarcodeWrapper(Barcode.TYPE_EMAIL, "old email", "mailto:a+tag@example.org?subject=Hello%20there&body=A%26B");
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(b)) {
            onView(withText(R.string.compose_email)).check(matches(isDisplayed()));
            onView(withText(R.string.copy_address)).check(matches(isDisplayed()));
            onView(withId(R.id.ib_search)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            onView(withId(R.id.ib_copy)).perform(click());
            scenario.onActivity(activity -> {
                assertEquals("a+tag@example.org", clipboard(activity));
                ActionHandler handler = new ActionHandler(activity, b);
                Intent draft = handler.createComposeIntent(ResultActions.Action.EMAIL);
                assertEquals(Intent.ACTION_SENDTO, draft.getAction());
                assertTrue(draft.getDataString().startsWith("mailto:a+tag@example.org?"));
                assertEquals("Hello there", draft.getStringExtra(Intent.EXTRA_SUBJECT));
                assertEquals("A&B", draft.getStringExtra(Intent.EXTRA_TEXT));
            });
        }
    }

    @Test public void smsCopiesMessageAndNumberSeparately() {
        BarcodeWrapper b = new BarcodeWrapper(Barcode.TYPE_SMS, "old SMS", "SMSTO:+14165550123:Hi: there");
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(b)) {
            onView(withId(R.id.ib_copy)).perform(click());
            scenario.onActivity(activity -> assertEquals("+14165550123", clipboard(activity)));
            onView(withId(R.id.ib_search)).perform(click());
            scenario.onActivity(activity -> {
                assertEquals("Hi: there", clipboard(activity));
                Intent draft = new ActionHandler(activity, b).createComposeIntent(ResultActions.Action.COMPOSE_SMS);
                assertEquals("+14165550123", draft.getData().getSchemeSpecificPart());
                assertEquals("Hi: there", draft.getStringExtra("sms_body"));
            });
        }
    }

    @Test public void oneActionAndNoActionLayoutsKeepDetailsVisible() {
        BarcodeWrapper b = new BarcodeWrapper(Barcode.TYPE_SMS, "Message only", "");
        b.message = "Message only";
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(b)) {
            onView(withText(R.string.copy_message)).check(matches(isDisplayed()));
            onView(withId(R.id.ib_action)).perform(click());
            scenario.onActivity(activity -> {
                assertEquals("Message only", clipboard(activity));
                View details = activity.findViewById(R.id.barcode_result);
                View button = activity.findViewById(R.id.ib_action);
                assertTrue(details.getHeight() > 0);
                assertTrue(details.getBottom() <= button.getTop());
                assertEquals(View.GONE, activity.findViewById(R.id.ib_copy).getVisibility());
            });
        }
        b.message = null;
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(b)) {
            onView(withId(R.id.barcode_result)).check(matches(isDisplayed()));
            onView(withId(R.id.ib_action)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }

    @Test public void historyRowOpensResultWithoutRequiringStoredFormat() {
        BarcodeWrapper b = new BarcodeWrapper(Barcode.TYPE_URL, "Friendly title", "URL:raw payload");
        b.url = "https://example.org/path?q=a%26b";
        try (ActivityScenario<BarcodeResultActivity> scenario = scan(b)) {
            onView(withId(R.id.ib_copy)).perform(click());
            scenario.onActivity(activity -> assertEquals(b.url, clipboard(activity)));
            onView(withText(R.string.share_link)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> {
                BarcodeWrapper phone = new BarcodeWrapper(Barcode.TYPE_PHONE, "raw phone payload", "tel:+14165550123");
                phone.phoneNumber = "+14165550123";
                RecyclerView list = new RecyclerView(activity);
                list.setLayoutManager(new LinearLayoutManager(activity));
                ResultListAdapter adapter = new ResultListAdapter();
                list.setAdapter(adapter);
                activity.setContentView(list);
                com.auroid.qrscanner.utils.Utils.applySystemBarInsets(activity);
                adapter.submitList(java.util.Collections.singletonList(new Result(new Gson().toJson(phone), new java.util.Date())));
            });
            onView(withText(R.string.send_sms)).check(doesNotExist());
            onView(withText(R.string.action_web_search)).check(doesNotExist());
            onView(withText(R.string.copy_number)).check(doesNotExist());
            onView(withText(R.string.view_more)).check(doesNotExist());
            onView(withId(R.id.textViewResult)).perform(click());
            onView(withText(R.string.send_sms)).check(matches(isDisplayed()));
            onView(withText(R.string.copy_number)).check(matches(isDisplayed()));
            onView(withId(R.id.barcode_format)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            androidx.test.espresso.Espresso.pressBack();
            onView(withId(R.id.textViewResult)).perform(click());
            onView(withText(R.string.copy_number)).check(matches(isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
            onView(withId(R.id.textViewResult)).check(matches(isDisplayed()));
        }
    }

    @Test public void savedBarcodeFormatIsShownWithoutIntentFormatExtra() {
        BarcodeWrapper b = new BarcodeWrapper(Barcode.TYPE_PRODUCT, "12345678", "12345678");
        b.barcodeFormat = Barcode.FORMAT_EAN_8;
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (ActivityScenario<BarcodeResultActivity> scenario = ActivityScenario.launch(
                new Intent(context, BarcodeResultActivity.class).putExtra("RESULT", new Gson().toJson(b)))) {
            onView(withId(R.id.barcode_format)).check(matches(withText("EAN-8")));
        }
    }

    private String clipboard(Context context) {
        return context.getSystemService(ClipboardManager.class).getPrimaryClip().getItemAt(0).getText().toString();
    }

    private ActivityScenario<BarcodeResultActivity> scan(BarcodeWrapper barcode) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return ActivityScenario.launch(new Intent(context, BarcodeResultActivity.class)
                .putExtra("RESULT", new Gson().toJson(barcode)).putExtra("FORMAT", Barcode.FORMAT_QR_CODE));
    }
}
