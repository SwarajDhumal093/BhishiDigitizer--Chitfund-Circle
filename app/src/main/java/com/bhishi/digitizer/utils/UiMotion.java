package com.bhishi.digitizer.utils;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.EditText;

/** Small, dependency-free motion helpers for consistent professional micro-interactions. */
public final class UiMotion {
    private UiMotion() { }

    public static void animateScreen(View root) {
        if (root == null) return;
        root.setAlpha(0f);
        root.setTranslationY(dp(root, 14));
        root.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(320)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public static void animateIn(View view, long delayMs) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(view, 18));
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(360)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public static void installPressFeedback(View root) {
        if (root == null) return;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                installPressFeedback(group.getChildAt(i));
            }
        }
        if (root.isClickable() && !(root instanceof EditText) && !(root instanceof CompoundButton)) {
            root.setOnTouchListener((v, event) -> {
                if (!v.isEnabled()) return false;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.975f).scaleY(0.975f).setDuration(90).start();
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                }
                return false;
            });
        }
    }

    private static float dp(View view, float dp) {
        return dp * view.getResources().getDisplayMetrics().density;
    }
}
