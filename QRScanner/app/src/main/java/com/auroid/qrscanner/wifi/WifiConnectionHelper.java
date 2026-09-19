package com.auroid.qrscanner.wifi;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.auroid.qrscanner.R;
import com.auroid.qrscanner.serializable.WiFiWrapper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.ArrayList;
import java.util.Collections;

/** One instance per result/history activity, registered before the activity starts. */
public final class WifiConnectionHelper implements DefaultLifecycleObserver {

    public interface Host {
        WifiConnectionHelper getWifiConnectionHelper();
    }

    private final AppCompatActivity mActivity;
    private final ActivityResultLauncher<Intent> mSaveNetworkLauncher;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver mWifiStateReceiver;
    private Runnable mEnableTimeout;
    private AlertDialog mDialog;
    private WiFiWrapper mCurrentWifi;

    public WifiConnectionHelper(AppCompatActivity activity) {
        mActivity = activity;
        mSaveNetworkLauncher = mActivity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::onSaveResult);
        mActivity.getLifecycle().addObserver(this);
    }

    public static void connect(Context context, WiFiWrapper wifi) {
        Context owner = context;
        while (!(owner instanceof Host) && owner instanceof ContextWrapper) {
            Context base = ((ContextWrapper) owner).getBaseContext();
            if (base == owner) {
                break;
            }
            owner = base;
        }
        if (owner instanceof Host) {
            ((Host) owner).getWifiConnectionHelper().connect(wifi);
        } else {
            Toast.makeText(context, R.string.wifi_request_failed, Toast.LENGTH_LONG).show();
        }
    }

    public void connect(WiFiWrapper wifi) {
        stopWaitingForWifi();
        mCurrentWifi = wifi;
        WifiCredentials.Problem problem = WifiCredentials.validate(wifi);
        if (problem != WifiCredentials.Problem.NONE) {
            int message = problem == WifiCredentials.Problem.INVALID_SSID
                    ? R.string.wifi_invalid_ssid
                    : problem == WifiCredentials.Problem.INVALID_PASSWORD
                    ? R.string.wifi_invalid_password : R.string.wifi_unsupported_security;
            showOptions(message);
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (wifi.encryptionType == Barcode.WiFi.TYPE_WEP
                        || (wifi.encryptionType == Barcode.WiFi.TYPE_WPA
                        && WifiCredentials.isRawPsk(wifi.password))) {
                    showOptions(R.string.wifi_unsupported_security);
                    return;
                }
                WifiNetworkSuggestion suggestion = createSuggestion(wifi);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ArrayList<WifiNetworkSuggestion> networks = new ArrayList<>();
                    networks.add(suggestion);
                    mSaveNetworkLauncher.launch(new Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
                            .putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, networks));
                } else {
                    suggestNetwork(suggestion);
                }
            } else {
                connectLegacy(wifi);
            }
        } catch (IllegalArgumentException e) {
            // Do not log exceptions that might contain scanned credentials.
            showOptions(R.string.wifi_invalid_credentials);
        } catch (SecurityException e) {
            showOptions(R.string.wifi_permission_denied);
        } catch (ActivityNotFoundException | UnsupportedOperationException e) {
            showOptions(R.string.wifi_request_failed);
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    static WifiNetworkSuggestion createSuggestion(WiFiWrapper wifi) {
        WifiNetworkSuggestion.Builder builder = new WifiNetworkSuggestion.Builder().setSsid(wifi.ssid);
        if (wifi.encryptionType == Barcode.WiFi.TYPE_WPA) {
            builder.setWpa2Passphrase(wifi.password);
        }
        return builder.build();
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private void suggestNetwork(WifiNetworkSuggestion suggestion) {
        WifiManager manager = mActivity.getApplicationContext().getSystemService(WifiManager.class);
        if (manager == null) {
            showOptions(R.string.wifi_unavailable);
            return;
        }
        int status = manager.addNetworkSuggestions(Collections.singletonList(suggestion));
        switch (status) {
            case WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS:
                showOptions(R.string.wifi_suggestion_submitted);
                break;
            case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE:
                showOptions(R.string.wifi_suggestion_exists);
                break;
            case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED:
                showOptions(R.string.wifi_permission_denied);
                break;
            default:
                showOptions(R.string.wifi_request_failed);
        }
    }

    private void onSaveResult(ActivityResult result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        if (result.getResultCode() == Activity.RESULT_CANCELED) {
            Toast.makeText(mActivity, R.string.wifi_save_cancelled, Toast.LENGTH_LONG).show();
            return;
        }
        Intent data = result.getData();
        ArrayList<Integer> results = data == null ? null
                : data.getIntegerArrayListExtra(Settings.EXTRA_WIFI_NETWORK_RESULT_LIST);
        if (result.getResultCode() != Activity.RESULT_OK || results == null || results.size() != 1) {
            showOptions(R.string.wifi_save_failed);
        } else if (Integer.valueOf(Settings.ADD_WIFI_RESULT_SUCCESS).equals(results.get(0))) {
            showOptions(R.string.wifi_network_saved);
        } else if (Integer.valueOf(Settings.ADD_WIFI_RESULT_ALREADY_EXISTS).equals(results.get(0))) {
            showOptions(R.string.wifi_network_exists);
        } else {
            showOptions(R.string.wifi_save_failed);
        }
    }

    @SuppressWarnings("deprecation")
    private void connectLegacy(WiFiWrapper wifi) {
        WifiManager manager = mActivity.getApplicationContext().getSystemService(WifiManager.class);
        if (manager == null) {
            showOptions(R.string.wifi_unavailable);
            return;
        }
        if (manager.isWifiEnabled()) {
            addLegacyNetwork(manager, wifi);
            return;
        }
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mWifiStateReceiver != this) {
                    return;
                }
                if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                        == WifiManager.WIFI_STATE_ENABLED) {
                    stopWaitingForWifi();
                    addLegacyNetwork(manager, wifi);
                }
            }
        };
        mActivity.registerReceiver(receiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        mWifiStateReceiver = receiver;
        mEnableTimeout = () -> {
            stopWaitingForWifi();
            showOptions(R.string.wifi_enable_failed);
        };
        mHandler.postDelayed(mEnableTimeout, 15000);
        try {
            if (!manager.setWifiEnabled(true)) {
                stopWaitingForWifi();
                showOptions(R.string.wifi_enable_failed);
            } else {
                Toast.makeText(mActivity, R.string.wifi_enabling, Toast.LENGTH_SHORT).show();
                // Handle Wi-Fi becoming enabled between the initial check and registration.
                if (manager.isWifiEnabled()) {
                    stopWaitingForWifi();
                    addLegacyNetwork(manager, wifi);
                }
            }
        } catch (SecurityException | IllegalArgumentException e) {
            stopWaitingForWifi();
            showOptions(R.string.wifi_permission_denied);
        }
    }

    @SuppressWarnings("deprecation")
    static WifiConfiguration createLegacyConfiguration(WiFiWrapper wifi) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = WifiCredentials.quoteForLegacyConfig(wifi.ssid);
        config.allowedKeyManagement.clear();
        if (wifi.encryptionType == Barcode.WiFi.TYPE_WPA) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.preSharedKey = WifiCredentials.isRawPsk(wifi.password)
                    ? wifi.password : WifiCredentials.quoteForLegacyConfig(wifi.password);
        } else {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            if (wifi.encryptionType == Barcode.WiFi.TYPE_WEP) {
                config.wepKeys[0] = WifiCredentials.isHexWepKey(wifi.password)
                        ? wifi.password : WifiCredentials.quoteForLegacyConfig(wifi.password);
                config.wepTxKeyIndex = 0;
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            }
        }
        return config;
    }

    @SuppressWarnings("deprecation")
    private void addLegacyNetwork(WifiManager manager, WiFiWrapper wifi) {
        try {
            int networkId = manager.addNetwork(createLegacyConfiguration(wifi));
            if (networkId == -1 || !manager.enableNetwork(networkId, true)) {
                showOptions(R.string.wifi_request_failed);
                return;
            }
            showOptions(R.string.wifi_connection_requested);
        } catch (SecurityException e) {
            showOptions(R.string.wifi_permission_denied);
        } catch (IllegalArgumentException e) {
            showOptions(R.string.wifi_invalid_credentials);
        }
    }

    private void showOptions(int message) {
        if (mActivity.isFinishing() || mActivity.isDestroyed()) {
            return;
        }
        if (mDialog != null) {
            mDialog.dismiss();
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mActivity)
                .setTitle(R.string.type_wifi)
                .setMessage(message)
                .setPositiveButton(R.string.wifi_open_settings, (d, which) -> openSettings())
                .setNegativeButton(R.string.close, null);
        final String password = mCurrentWifi == null ? null : mCurrentWifi.password;
        if (password != null && !password.isEmpty()) {
            builder.setNeutralButton(R.string.wifi_copy_password, null);
        }
        mDialog = builder.create();
        mDialog.show();
        if (password != null && !password.isEmpty()) {
            // Keep the settings action available after copying.
            mDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> copyPassword(password));
        }
    }

    private void copyPassword(String password) {
        ClipboardManager clipboard = mActivity.getSystemService(ClipboardManager.class);
        if (clipboard == null) {
            return;
        }
        ClipData clip = ClipData.newPlainText(mActivity.getString(R.string.wifi_copy_password), password);
        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
        clip.getDescription().setExtras(extras);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(mActivity, R.string.confirm_copy_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    private void openSettings() {
        try {
            mActivity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(mActivity, R.string.wifi_settings_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void stopWaitingForWifi() {
        if (mEnableTimeout != null) {
            mHandler.removeCallbacks(mEnableTimeout);
            mEnableTimeout = null;
        }
        if (mWifiStateReceiver != null) {
            mActivity.unregisterReceiver(mWifiStateReceiver);
            mWifiStateReceiver = null;
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        stopWaitingForWifi();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        stopWaitingForWifi();
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mCurrentWifi = null;
    }
}
