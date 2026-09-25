package com.bhishi.digitizer;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.Nullable;
import android.os.Bundle;

import com.bhishi.digitizer.utils.BiometricHelper;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

/**
 * Login screen. Two ways in:
 *  1. Phone + password -> Firebase Auth signInWithEmailAndPassword (phone
 *     number is mapped to a synthetic email internally, e.g. 9876543210@bhishi.app,
 *     since Firebase Auth's email/password provider is the simplest to wire
 *     up for a student project; swap for PhoneAuthProvider + OtpActivity if
 *     you want real SMS OTP sign-in).
 *  2. Fingerprint -> only shown once the person has logged in with a
 *     password at least once on this device and opted in. Confirms identity
 *     locally via BiometricPrompt, then reuses the FirebaseAuth session that
 *     is already cached on the device -- no password re-entry needed.
 */
public class LoginActivity extends BaseActivity {

    private EditText etPhone, etPassword;
    private LinearLayout layoutFingerprint;
    private ImageView btnFingerprint;
    private PrefsManager prefsManager;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        prefsManager = new PrefsManager(this);

        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        layoutFingerprint = findViewById(R.id.layoutFingerprint);
        btnFingerprint = findViewById(R.id.btnFingerprint);

        TextView btnLogin = findViewById(R.id.btnLogin);
        TextView tvGoSignUp = findViewById(R.id.tvGoSignUp);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> loginWithPassword());
        tvGoSignUp.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));
        tvForgotPassword.setOnClickListener(v -> Toast.makeText(this,
                "Password reset link would be emailed/SMS'd here", Toast.LENGTH_SHORT).show());

        setUpFingerprintOption();
    }

    private void setUpFingerprintOption() {
        boolean hasSession = auth.getCurrentUser() != null;
        boolean biometricEnabled = prefsManager.isBiometricEnabled();
        boolean deviceSupportsIt = BiometricHelper.isBiometricAvailable(this);

        // Only surface the fingerprint button when all three are true:
        // a cached session exists, the user opted in earlier, and the
        // device actually has an enrolled fingerprint/face.
        if (hasSession && biometricEnabled && deviceSupportsIt) {
            layoutFingerprint.setVisibility(View.VISIBLE);
            btnFingerprint.setOnClickListener(v -> authenticateWithFingerprint());
        } else {
            layoutFingerprint.setVisibility(View.GONE);
        }
    }

    private void authenticateWithFingerprint() {
        BiometricHelper.authenticate(this, new BiometricHelper.Callback() {
            @Override
            public void onSuccess() {
                Toast.makeText(LoginActivity.this, "Fingerprint verified", Toast.LENGTH_SHORT).show();
                routeToDashboard();
            }

            @Override
            public void onFailed(String reason) {
                Toast.makeText(LoginActivity.this, "Fingerprint login failed: " + reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginWithPassword() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(phone) || phone.length() != 10) {
            etPhone.setError("Enter a valid 10-digit phone number");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            return;
        }

        String syntheticEmail = phone + "@bhishi.app";

        auth.signInWithEmailAndPassword(syntheticEmail, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        loadProfileAndOfferBiometric(user.getUid());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadProfileAndOfferBiometric(String uid) {
        FirebasePaths.user(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String role = snapshot.child("role").getValue(String.class);
                prefsManager.saveSession(uid, name, role != null ? role : "member");

                if (!prefsManager.isBiometricEnabled() && BiometricHelper.isBiometricAvailable(LoginActivity.this)) {
                    offerBiometricOptIn();
                } else {
                    routeToDashboard();
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                routeToDashboard();
            }
        });
    }

    /** Section CO4 UX detail: fingerprint is opt-in, offered only AFTER a normal login succeeds. */
    private void offerBiometricOptIn() {
        new AlertDialog.Builder(this)
                .setTitle("Enable fingerprint login?")
                .setMessage("Next time, unlock Bhishi Digitizer with just your fingerprint instead of typing your password.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    prefsManager.setBiometricEnabled(true);
                    routeToDashboard();
                })
                .setNegativeButton("Not now", (dialog, which) -> routeToDashboard())
                .setCancelable(false)
                .show();
    }

    private void routeToDashboard() {
        String role = prefsManager.getRole();
        Intent intent = "admin".equals(role)
                ? new Intent(this, AdminDashboardActivity.class)
                : new Intent(this, MemberDashboardActivity.class);
        startActivity(intent);
        finish();
    }
}
