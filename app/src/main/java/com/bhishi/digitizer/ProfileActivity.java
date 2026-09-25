package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bhishi.digitizer.utils.BiometricHelper;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.bhishi.digitizer.utils.LocaleManager;
import com.bhishi.digitizer.utils.ThemeManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends BaseActivity {

    private PrefsManager prefsManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        prefsManager = new PrefsManager(this);

        TextView tvName = findViewById(R.id.tvName);
        TextView tvRolePhone = findViewById(R.id.tvRolePhone);
        SwitchMaterial switchBiometric = findViewById(R.id.switchBiometric);
        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        ImageView ivThemeMode = findViewById(R.id.ivThemeMode);
        TextView tvThemeDescription = findViewById(R.id.tvThemeDescription);

        String savedName = prefsManager.getName();
        tvName.setText(savedName == null || savedName.trim().isEmpty() ? "Bhishi member" : savedName);
        loadRoleAndPhone(tvRolePhone);

        boolean dark = ThemeManager.isDarkMode(this);
        switchDarkMode.setChecked(dark);
        updateThemeRow(dark, ivThemeMode, tvThemeDescription);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ThemeManager.isDarkMode(this) == isChecked) return;
            updateThemeRow(isChecked, ivThemeMode, tvThemeDescription);
            ThemeManager.setDarkMode(this, isChecked);
        });

        switchBiometric.setChecked(prefsManager.isBiometricEnabled());
        switchBiometric.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked && !BiometricHelper.isBiometricAvailable(this)) {
                Toast.makeText(this, getString(R.string.biometric_not_available), Toast.LENGTH_LONG).show();
                buttonView.setChecked(false);
                return;
            }
            prefsManager.setBiometricEnabled(isChecked);
            Toast.makeText(this, isChecked ? "Biometric login enabled" : "Biometric login disabled",
                    Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.rowEditProfile).setOnClickListener(v ->
                Toast.makeText(this, "Edit profile form goes here", Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        TextView tvLanguageValue = findViewById(R.id.tvLanguageValue);
        String language = LocaleManager.getLanguage(this);
        tvLanguageValue.setText("mr".equals(language) ? "मराठी" : ("hi".equals(language) ? "हिंदी" : "English"));
        findViewById(R.id.rowLanguage).setOnClickListener(v ->
                startActivity(new Intent(this, LanguageActivity.class)));
        findViewById(R.id.rowChangePassword).setOnClickListener(v ->
                Toast.makeText(this, "Password change form goes here", Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowAboutHelp).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Bhishi Digitizer")
                .setMessage("A transparent digital companion for Bhishi groups — contributions, verified records, passbooks and fair payout rounds in one place.")
                .setPositiveButton("Got it", null)
                .show());

        findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());
    }

    private void updateThemeRow(boolean dark, ImageView icon, TextView description) {
        icon.setImageResource(dark ? R.drawable.ic_moon : R.drawable.ic_sun);
        description.setText(dark
                ? "Dark high-contrast interface is active"
                : "Use a darker high-contrast interface");
    }

    private void loadRoleAndPhone(TextView tvRolePhone) {
        String uid = prefsManager.getUid();
        if (uid == null) return;

        FirebasePaths.user(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.child("role").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);
                String roleLabel = "admin".equals(role) ? "Admin" : "Member";
                tvRolePhone.setText(roleLabel + (phone == null ? "" : " · " + phone));
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setMessage("Your app theme will stay saved on this device, but you'll need to sign in again.")
                .setPositiveButton("Log out", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        prefsManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
