package com.bhishi.digitizer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/** Persists the app language independently from Firebase/login state. */
public final class LocaleManager {
    private static final String PREFS = "bhishi_ui_preferences";
    private static final String KEY_LANGUAGE = "app_language";

    private LocaleManager() { }

    public static String getLanguage(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, "en");
    }

    public static void setLanguage(Context context, String languageCode) {
        if (!"mr".equals(languageCode) && !"hi".equals(languageCode)) languageCode = "en";
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
    }

    public static Context wrap(Context context) {
        String language = getLanguage(context);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            config.setLayoutDirection(locale);
            return context.createConfigurationContext(config);
        }
        config.locale = locale;
        return context.createConfigurationContext(config);
    }
}
