package com.bhishi.digitizer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bhishi.digitizer.utils.LocaleManager;
import com.bhishi.digitizer.utils.NetworkMonitor;
import com.bhishi.digitizer.utils.ThemeManager;
import com.bhishi.digitizer.utils.UiMotion;
import com.google.android.material.snackbar.Snackbar;

/** Base activity that applies the saved theme and subtle screen/button motion consistently. */
public abstract class BaseActivity extends AppCompatActivity {

    private NetworkMonitor networkMonitor;
    private Snackbar offlineSnackbar;
    private Boolean lastOnlineState;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
        networkMonitor = new NetworkMonitor(this,
                online -> runOnUiThread(() -> handleConnectivityChange(online)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (networkMonitor != null) networkMonitor.start();
    }

    @Override
    protected void onStop() {
        if (networkMonitor != null) networkMonitor.stop();
        if (offlineSnackbar != null) {
            offlineSnackbar.dismiss();
            offlineSnackbar = null;
        }
        super.onStop();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ViewGroup content = findViewById(android.R.id.content);
        if (content != null && content.getChildCount() > 0) {
            View root = content.getChildAt(0);
            UiMotion.animateScreen(root);
            UiMotion.installPressFeedback(root);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
    }

    /** Shows a persistent, app-wide offline indicator and a short recovery message. */
    private void handleConnectivityChange(boolean online) {
        View content = findViewById(android.R.id.content);
        if (content == null) return;

        if (!online) {
            showOfflineSnackbar(content);
        } else {
            boolean recovered = Boolean.FALSE.equals(lastOnlineState);
            if (offlineSnackbar != null) {
                offlineSnackbar.dismiss();
                offlineSnackbar = null;
            }
            if (recovered) showBackOnlineSnackbar(content);
        }
        lastOnlineState = online;
    }

    private void showOfflineSnackbar(View content) {
        if (offlineSnackbar != null && offlineSnackbar.isShown()) return;
        offlineSnackbar = Snackbar.make(content, R.string.no_internet_message, Snackbar.LENGTH_INDEFINITE)
                .setBackgroundTint(getColor(R.color.brand_orange_dark))
                .setTextColor(getColor(R.color.white))
                .setActionTextColor(getColor(R.color.white))
                .setAction(R.string.network_settings, v -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    } catch (Exception ignored) {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    }
                });
        View anchor = findViewById(R.id.bottomNav);
        if (anchor != null) offlineSnackbar.setAnchorView(anchor);
        offlineSnackbar.show();
    }

    private void showBackOnlineSnackbar(View content) {
        Snackbar snackbar = Snackbar.make(content, R.string.back_online_message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(getColor(R.color.brand_teal_dark))
                .setTextColor(getColor(R.color.white));
        View anchor = findViewById(R.id.bottomNav);
        if (anchor != null) snackbar.setAnchorView(anchor);
        snackbar.show();
    }
}
