package com.bhishi.digitizer.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lightweight session store. Firebase Auth already persists the logged-in
 * session on its own; this just remembers whether the person has opted
 * into fingerprint login on THIS device, and their role, so the splash
 * screen and login screen can decide what to show.
 */
public class PrefsManager {

    private static final String PREFS = "bhishi_prefs";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_ROLE = "role";
    private static final String KEY_UID = "uid";
    private static final String KEY_NAME = "name";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void saveSession(String uid, String name, String role) {
        prefs.edit()
                .putString(KEY_UID, uid)
                .putString(KEY_NAME, name)
                .putString(KEY_ROLE, role)
                .apply();
    }

    public String getUid() {
        return prefs.getString(KEY_UID, null);
    }

    public String getName() {
        return prefs.getString(KEY_NAME, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "member");
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
