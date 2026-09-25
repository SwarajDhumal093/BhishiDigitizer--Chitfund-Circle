package com.bhishi.digitizer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.razorpay.Checkout;
import com.razorpay.PaymentData;
import com.razorpay.PaymentResultWithDataListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Professional payment flow for the Java Android app.
 *
 * Security rule: the Android app NEVER contains the gateway secret. It asks the HTTPS backend
 * to create an order, opens Razorpay Checkout with the returned public key/order id, then sends
 * the success payload back to the backend for mandatory signature verification.
 */
public class PaymentActivity extends BaseActivity implements PaymentResultWithDataListener {

    public static final String EXTRA_GROUP_ID = "payment_group_id";
    public static final String EXTRA_GROUP_NAME = "payment_group_name";
    public static final String EXTRA_AMOUNT = "payment_amount";

    private String groupId;
    private String groupName;
    private double amount;
    private TextView btnPaySecurely, btnRecordOffline, tvPaymentStatus;
    private ProgressBar progress;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        groupName = getIntent().getStringExtra(EXTRA_GROUP_NAME);
        amount = getIntent().getDoubleExtra(EXTRA_AMOUNT, 0);

        if (groupId == null || amount <= 0) {
            Toast.makeText(this, "Invalid payment request", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currency.setMaximumFractionDigits(0);
        ((TextView) findViewById(R.id.tvGroupName)).setText(groupName == null ? "Bhishi contribution" : groupName);
        ((TextView) findViewById(R.id.tvAmount)).setText(currency.format(amount));
        ((TextView) findViewById(R.id.tvMonth)).setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new java.util.Date()));

        btnPaySecurely = findViewById(R.id.btnPaySecurely);
        btnRecordOffline = findViewById(R.id.btnRecordOffline);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        progress = findViewById(R.id.progressPayment);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        String baseUrl = getString(R.string.payment_backend_base_url).trim();
        boolean gatewayReady = baseUrl.startsWith("https://") && !baseUrl.contains("YOUR_BACKEND_DOMAIN");
        btnPaySecurely.setEnabled(gatewayReady);
        btnPaySecurely.setAlpha(gatewayReady ? 1f : 0.65f);
        btnPaySecurely.setText(gatewayReady ? "Pay digitally" : "Digital payment - coming later");
        tvPaymentStatus.setText(gatewayReady
                ? "Payment is recorded only after server-side verification."
                : "Gateway integration is intentionally kept for a later version. For now, record the payment and let the admin verify it.");
        btnPaySecurely.setOnClickListener(v -> {
            if (gatewayReady) createGatewayOrder();
        });
        btnRecordOffline.setOnClickListener(v -> confirmOfflinePayment());

        Checkout.preload(getApplicationContext());
    }

    private void createGatewayOrder() {
        String baseUrl = getString(R.string.payment_backend_base_url).trim();
        if (!baseUrl.startsWith("https://") || baseUrl.contains("YOUR_BACKEND_DOMAIN")) {
            new AlertDialog.Builder(this)
                    .setTitle("Payment backend not configured")
                    .setMessage("The professional payment flow is installed, but you must set payment_backend_base_url in strings.xml to your HTTPS Node/Express backend. The gateway secret must never be stored inside the Android app.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        setLoading(true, "Creating a secure payment order…");
        withFirebaseToken(token -> executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("groupId", groupId);
                body.put("monthKey", FirebasePaths.currentMonthKey());
                body.put("amount", Math.round(amount * 100));
                body.put("currency", "INR");

                JSONObject response = postJson(baseUrl + "/payments/create-order", body, token);
                String orderId = response.getString("orderId");
                String keyId = response.getString("keyId");
                long amountSubunits = response.optLong("amount", Math.round(amount * 100));

                main.post(() -> openCheckout(orderId, keyId, amountSubunits));
            } catch (Exception e) {
                main.post(() -> paymentError("Could not create payment order: " + safeMessage(e)));
            }
        }));
    }

    private void openCheckout(String orderId, String keyId, long amountSubunits) {
        setLoading(false, "Opening secure checkout…");
        try {
            Checkout checkout = new Checkout();
            checkout.setKeyID(keyId);

            JSONObject options = new JSONObject();
            options.put("name", "Bhishi Digitizer");
            options.put("description", (groupName == null ? "Bhishi" : groupName) + " • " + FirebasePaths.currentMonthKey());
            options.put("currency", "INR");
            options.put("amount", amountSubunits);
            options.put("order_id", orderId);
            options.put("theme.color", "#0B6B50");
            options.put("retry", new JSONObject().put("enabled", true).put("max_count", 2));

            PrefsManager prefs = new PrefsManager(this);
            JSONObject prefill = new JSONObject();
            if (prefs.getName() != null) prefill.put("name", prefs.getName());
            options.put("prefill", prefill);

            checkout.open(this, options);
        } catch (Exception e) {
            paymentError("Could not open payment checkout: " + safeMessage(e));
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentId, PaymentData paymentData) {
        if (paymentData == null) {
            paymentError("Payment returned without verification data.");
            return;
        }
        verifyPaymentOnServer(paymentData);
    }

    @Override
    public void onPaymentError(int code, String response, PaymentData paymentData) {
        setLoading(false, "Payment was not completed. You can safely try again.");
        Toast.makeText(this, "Payment not completed", Toast.LENGTH_LONG).show();
    }

    private void verifyPaymentOnServer(PaymentData data) {
        String baseUrl = getString(R.string.payment_backend_base_url).trim();
        setLoading(true, "Payment received. Verifying securely…");
        withFirebaseToken(token -> executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("groupId", groupId);
                body.put("monthKey", FirebasePaths.currentMonthKey());
                body.put("razorpay_payment_id", data.getPaymentId());
                body.put("razorpay_order_id", data.getOrderId());
                body.put("razorpay_signature", data.getSignature());

                JSONObject response = postJson(baseUrl + "/payments/verify", body, token);
                if (!response.optBoolean("verified", false)) throw new IllegalStateException("Server could not verify the payment");
                if (!response.optBoolean("captured", false)) {
                    main.post(() -> {
                        setLoading(false, "Payment authorized. Waiting for secure gateway capture…");
                        btnPaySecurely.setEnabled(false);
                        btnRecordOffline.setEnabled(false);
                        new AlertDialog.Builder(PaymentActivity.this)
                                .setTitle("Payment is being confirmed")
                                .setMessage("The gateway has authenticated your payment, but it is not captured yet. The server webhook will update your ledger automatically after capture. Do not pay again.")
                                .setPositiveButton("Done", (dialog, which) -> finish())
                                .show();
                    });
                    return;
                }
                main.post(() -> showGatewayVerified(data.getPaymentId()));
            } catch (Exception e) {
                main.post(() -> paymentVerificationPending("We could not complete the server verification right now. " + safeMessage(e)));
            }
        }));
    }

    private void paymentVerificationPending(String detail) {
        setLoading(false, "Payment callback received • verification pending");
        btnPaySecurely.setEnabled(false);
        btnRecordOffline.setEnabled(false);
        new AlertDialog.Builder(this)
                .setTitle("Do not pay again yet")
                .setMessage(detail + "\n\nThe signed server webhook may still reconcile this payment. Return to the dashboard and check the ledger before attempting another payment.")
                .setPositiveButton("Back to dashboard", (dialog, which) -> finish())
                .show();
    }

    private void showGatewayVerified(String paymentId) {
        // The backend has already written the verified gateway record using the Admin SDK.
        // The Android client deliberately does not self-assert paymentVerified/paymentId.
        setLoading(false, "Payment captured and verified ✓  Ledger updated securely.");
        btnPaySecurely.setEnabled(false);
        btnRecordOffline.setEnabled(false);
        Toast.makeText(this, "Payment verified successfully", Toast.LENGTH_LONG).show();
    }

    private void confirmOfflinePayment() {
        new AlertDialog.Builder(this)
                .setTitle("Submit offline payment?")
                .setMessage("Use this only after paying the group admin outside the app. Your entry will remain pending until the admin confirms it.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit", (dialog, which) -> recordOfflinePayment())
                .show();
    }

    private void recordOfflinePayment() {
        String uid = new PrefsManager(this).getUid();
        if (uid == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("confirmedByMember", true);
        updates.put("paid", true);
        updates.put("paymentMethod", "offline");
        updates.put("paymentStatus", "pending_admin");
        updates.put("timestamp", System.currentTimeMillis());
        applyMemberPaymentUpdate(uid, updates, () -> {
            Toast.makeText(this, "Submitted for admin verification", Toast.LENGTH_LONG).show();
            finish();
        }, "Could not submit offline payment");
    }

    private interface SuccessAction { void run(); }

    private void applyMemberPaymentUpdate(String uid, Map<String, Object> updates, SuccessAction onSuccess, String failureMessage) {
        com.google.firebase.database.DatabaseReference ref = FirebasePaths.contributions(groupId, FirebasePaths.currentMonthKey()).child(uid);
        ref.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    updates.put("uid", uid);
                    updates.put("memberName", new PrefsManager(PaymentActivity.this).getName());
                    updates.put("amount", amount);
                    updates.put("onTime", true);
                }
                ref.updateChildren(updates)
                        .addOnSuccessListener(unused -> onSuccess.run())
                        .addOnFailureListener(e -> paymentError(failureMessage + ": " + safeMessage(e)));
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                paymentError(failureMessage + ": " + error.getMessage());
            }
        });
    }

    private interface TokenCallback { void onToken(String token); }

    private void withFirebaseToken(TokenCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            paymentError("Your session has expired. Please sign in again.");
            return;
        }
        user.getIdToken(false)
                .addOnSuccessListener(result -> callback.onToken(result.getToken()))
                .addOnFailureListener(e -> paymentError("Could not authorize payment request."));
    }

    private JSONObject postJson(String endpoint, JSONObject body, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder text = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) text.append(line);
            }
        }
        connection.disconnect();
        if (status < 200 || status >= 300) throw new IllegalStateException("Server error " + status + (text.length() > 0 ? ": " + text : ""));
        return new JSONObject(text.toString());
    }

    private void setLoading(boolean loading, String status) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPaySecurely.setEnabled(!loading);
        btnRecordOffline.setEnabled(!loading);
        tvPaymentStatus.setText(status);
    }

    private void paymentError(String message) {
        setLoading(false, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? "Unknown error" : e.getMessage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
