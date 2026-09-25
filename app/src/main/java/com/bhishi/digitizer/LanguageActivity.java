package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bhishi.digitizer.utils.LocaleManager;

public class LanguageActivity extends BaseActivity {
    private TextView checkEnglish, checkMarathi, checkHindi;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        checkEnglish = findViewById(R.id.checkEnglish);
        checkMarathi = findViewById(R.id.checkMarathi);
        checkHindi = findViewById(R.id.checkHindi);
        findViewById(R.id.rowEnglish).setOnClickListener(v -> choose("en"));
        findViewById(R.id.rowMarathi).setOnClickListener(v -> choose("mr"));
        findViewById(R.id.rowHindi).setOnClickListener(v -> choose("hi"));
        updateChecks();
    }

    private void choose(String code) {
        if (code.equals(LocaleManager.getLanguage(this))) return;
        LocaleManager.setLanguage(this, code);
        Toast.makeText(this, R.string.language_saved, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateChecks() {
        String current = LocaleManager.getLanguage(this);
        checkEnglish.setVisibility("en".equals(current) ? View.VISIBLE : View.INVISIBLE);
        checkMarathi.setVisibility("mr".equals(current) ? View.VISIBLE : View.INVISIBLE);
        checkHindi.setVisibility("hi".equals(current) ? View.VISIBLE : View.INVISIBLE);
    }
}
