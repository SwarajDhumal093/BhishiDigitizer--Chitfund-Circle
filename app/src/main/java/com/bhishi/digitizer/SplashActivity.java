package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.bhishi.digitizer.utils.PrefsManager;
import com.bhishi.digitizer.utils.UiMotion;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        playBrandIntro();
        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, 2300);
    }

    private void playBrandIntro() {
        View logo = findViewById(R.id.logoContainer);
        View title = findViewById(R.id.tvSplashTitle);
        View headline = findViewById(R.id.tvSplashHeadline);
        View body = findViewById(R.id.tvSplashBody);
        View features = findViewById(R.id.featureRow);
        View progress = findViewById(R.id.splashProgress);
        View footer = findViewById(R.id.splashFooter);

        logo.setAlpha(0f);
        logo.setScaleX(0.72f);
        logo.setScaleY(0.72f);
        logo.setRotation(-7f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(0f)
                .setDuration(620).start();

        UiMotion.animateIn(title, 250);
        UiMotion.animateIn(headline, 430);
        UiMotion.animateIn(body, 600);
        UiMotion.animateIn(features, 790);
        UiMotion.animateIn(progress, 980);
        UiMotion.animateIn(footer, 1120);
    }

    private void routeNext() {
        boolean isLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        if (isLoggedIn) {
            PrefsManager prefs = new PrefsManager(this);
            String role = prefs.getRole();
            Intent intent = "admin".equals(role)
                    ? new Intent(this, AdminDashboardActivity.class)
                    : new Intent(this, MemberDashboardActivity.class);
            startActivity(intent);
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
