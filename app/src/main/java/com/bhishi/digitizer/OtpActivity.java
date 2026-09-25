package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

/**
 * Optional real-SMS OTP screen using Firebase PhoneAuthProvider.
 * Use this instead of the password field in LoginActivity if you want
 * true OTP-based sign-in rather than phone+password.
 */
public class OtpActivity extends BaseActivity {

    public static final String EXTRA_PHONE = "extra_phone";

    private EditText etOtp;
    private TextView tvResend;
    private String verificationId;
    private String phoneNumber;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        phoneNumber = getIntent().getStringExtra(EXTRA_PHONE);
        etOtp = findViewById(R.id.etOtp);
        tvResend = findViewById(R.id.tvResend);
        TextView tvPhoneSentTo = findViewById(R.id.tvPhoneSentTo);
        TextView btnVerify = findViewById(R.id.btnVerify);

        tvPhoneSentTo.setText("Code sent to " + phoneNumber);

        sendOtp();
        startResendTimer();

        btnVerify.setOnClickListener(v -> {
            String code = etOtp.getText().toString().trim();
            if (code.length() != 6 || verificationId == null) {
                Toast.makeText(this, "Enter the 6-digit code", Toast.LENGTH_SHORT).show();
                return;
            }
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            signInWithCredential(credential);
        });
    }

    private void sendOtp() {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber("+91" + phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Auto-retrieval: some devices verify without the user typing anything.
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(OtpActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verifId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = verifId;
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "Number verified", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Invalid code: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void startResendTimer() {
        new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResend.setText("Resend code in 00:" + (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                tvResend.setText("Resend code");
                tvResend.setOnClickListener(v -> {
                    sendOtp();
                    startResendTimer();
                });
            }
        }.start();
    }
}
