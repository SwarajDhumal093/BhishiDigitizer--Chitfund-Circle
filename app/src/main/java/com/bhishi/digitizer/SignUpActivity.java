package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bhishi.digitizer.models.AppUser;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends BaseActivity {

    private EditText etName, etPhone, etPassword;
    private RadioGroup radioRole;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        radioRole = findViewById(R.id.radioRole);

        TextView btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView tvGoLogin = findViewById(R.id.tvGoLogin);

        btnCreateAccount.setOnClickListener(v -> createAccount());
        tvGoLogin.setOnClickListener(v -> finish());
    }

    private void createAccount() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name required");
            return;
        }
        if (phone.length() != 10) {
            etPhone.setError("Enter a valid 10-digit phone number");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Minimum 6 characters");
            return;
        }

        String role = radioRole.getCheckedRadioButtonId() == R.id.radioAdmin ? "admin" : "member";
        String syntheticEmail = phone + "@bhishi.app";

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(syntheticEmail, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) return;

                    AppUser appUser = new AppUser(user.getUid(), name, phone, role);
                    FirebasePaths.user(user.getUid()).setValue(appUser)
                            .addOnSuccessListener(unused -> {
                                new PrefsManager(this).saveSession(user.getUid(), name, role);
                                Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();
                                Intent intent = "admin".equals(role)
                                        ? new Intent(this, AdminDashboardActivity.class)
                                        : new Intent(this, MemberDashboardActivity.class);
                                startActivity(intent);
                                finishAffinity();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Sign up failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
