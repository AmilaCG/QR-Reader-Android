package com.auroid.qrscanner;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragmentCompat())
                .commit();

        ((TextView) findViewById(R.id.top_action_title)).setText(R.string.activity_label_settings);
        findViewById(R.id.back_button).setOnClickListener(this);
        findViewById(R.id.top_action_button).setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.back_button) {
            onBackPressed();
        }
    }

    public static class SettingsFragmentCompat extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            String key = preference.getKey();
//            if (key != null && key.equals("rate_me")) {
//                Toast.makeText(getContext(), "clicked on rate", Toast.LENGTH_SHORT).show();
//            }
            return super.onPreferenceTreeClick(preference);
        }
    }
}
