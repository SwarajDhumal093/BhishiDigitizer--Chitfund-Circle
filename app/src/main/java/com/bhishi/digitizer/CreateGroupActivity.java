package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bhishi.digitizer.models.Group;
import com.bhishi.digitizer.models.GroupMember;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.GroupNotificationManager;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Creates a Bhishi group with a unique share code and an explicit payout mechanism. */
public class CreateGroupActivity extends BaseActivity {

    private EditText etGroupName, etAmount, etDuration, etDueDay, etGraceDays;
    private TextView btnModeDraw, btnModeAuction, tvModeHelp, btnCreate;
    private MaterialCardView cardModeDraw, cardModeAuction;
    private CheckBox checkOfflineAllowed;
    private String selectedMode = "draw";
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        etGroupName = findViewById(R.id.etGroupName);
        etAmount = findViewById(R.id.etAmount);
        etDuration = findViewById(R.id.etDuration);
        etDueDay = findViewById(R.id.etDueDay);
        etGraceDays = findViewById(R.id.etGraceDays);
        checkOfflineAllowed = findViewById(R.id.checkOfflineAllowed);
        btnModeDraw = findViewById(R.id.btnModeDraw);
        btnModeAuction = findViewById(R.id.btnModeAuction);
        tvModeHelp = findViewById(R.id.tvModeHelp);
        btnCreate = findViewById(R.id.btnCreate);
        cardModeDraw = findViewById(R.id.cardModeDraw);
        cardModeAuction = findViewById(R.id.cardModeAuction);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        cardModeDraw.setOnClickListener(v -> selectMode("draw"));
        cardModeAuction.setOnClickListener(v -> selectMode("auction"));
        btnModeDraw.setOnClickListener(v -> selectMode("draw"));
        btnModeAuction.setOnClickListener(v -> selectMode("auction"));
        btnCreate.setOnClickListener(v -> validateAndCreate());

        selectMode("draw");
    }

    private void selectMode(String mode) {
        selectedMode = mode;
        boolean draw = "draw".equals(mode);

        cardModeDraw.setCardBackgroundColor(getColor(draw ? R.color.green_surface : R.color.card));
        cardModeDraw.setStrokeColor(getColor(draw ? R.color.green : R.color.line_strong));
        cardModeAuction.setCardBackgroundColor(getColor(draw ? R.color.card : R.color.gold_dim));
        cardModeAuction.setStrokeColor(getColor(draw ? R.color.line_strong : R.color.gold));

        btnModeDraw.setTextColor(getColor(draw ? R.color.green_dark : R.color.text_primary));
        btnModeAuction.setTextColor(getColor(draw ? R.color.text_primary : R.color.gold_dark));
        tvModeHelp.setText(draw
                ? "Lucky draw uses only dual-verified paid members and stores an audit code with the locked result."
                : "Sealed auction keeps bids private until closing. The lowest valid bid wins and the discount is distributed to the other members.");
    }

    private void validateAndCreate() {
        String name = etGroupName.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim().replace(",", "");
        String durationStr = etDuration.getText().toString().trim();
        String dueDayStr = etDueDay.getText().toString().trim();
        String graceDaysStr = etGraceDays.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etGroupName.setError("Enter a group name");
            etGroupName.requestFocus();
            return;
        }
        if (name.length() < 3) {
            etGroupName.setError("Use at least 3 characters");
            etGroupName.requestFocus();
            return;
        }

        final double amount;
        final int duration;
        final int dueDay;
        final int graceDays;
        try {
            amount = Double.parseDouble(amountStr);
            duration = Integer.parseInt(durationStr);
            dueDay = TextUtils.isEmpty(dueDayStr) ? 5 : Integer.parseInt(dueDayStr);
            graceDays = TextUtils.isEmpty(graceDaysStr) ? 3 : Integer.parseInt(graceDaysStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid amount and duration", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount <= 0) {
            etAmount.setError("Amount must be greater than zero");
            etAmount.requestFocus();
            return;
        }
        if (duration < 2) {
            etDuration.setError("A Bhishi cycle needs at least 2 months");
            etDuration.requestFocus();
            return;
        }

        btnCreate.setEnabled(false);
        btnCreate.setText("Creating secure group…");
        createWithUniqueCode(name, amount, duration, dueDay, graceDays, 0);
    }

    private void createWithUniqueCode(String name, double amount, int duration, int dueDay, int graceDays, int attempt) {
        if (attempt >= 8) {
            resetCreateButton();
            Toast.makeText(this, "Could not generate a unique group code. Please retry.", Toast.LENGTH_LONG).show();
            return;
        }

        String groupCode = String.format(Locale.US, "%06d", 100000 + secureRandom.nextInt(900000));
        FirebasePaths.groups().orderByChild("groupCode").equalTo(groupCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            createWithUniqueCode(name, amount, duration, dueDay, graceDays, attempt + 1);
                        } else {
                            persistGroup(name, amount, duration, dueDay, graceDays, groupCode);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        resetCreateButton();
                        Toast.makeText(CreateGroupActivity.this,
                                "Could not verify group code: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void persistGroup(String name, double amount, int duration, int dueDay, int graceDays, String groupCode) {
        PrefsManager prefs = new PrefsManager(this);
        String adminId = prefs.getUid();
        String adminName = prefs.getName();
        if (adminId == null) {
            resetCreateButton();
            Toast.makeText(this, "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show();
            return;
        }

        String startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Group group = new Group(name, adminId, adminName, amount, duration, startDate, selectedMode, groupCode);
        group.memberCount = 1;
        group.dueDay = dueDay;
        group.graceDays = graceDays;
        group.allowOfflinePayments = checkOfflineAllowed.isChecked();

        String groupId = FirebasePaths.groups().push().getKey();
        if (groupId == null) {
            resetCreateButton();
            return;
        }

        FirebasePaths.group(groupId).setValue(group)
                .addOnSuccessListener(unused -> {
                    GroupMember adminAsMember = new GroupMember(adminId,
                            adminName == null ? "Admin" : adminName, "", startDate);
                    FirebasePaths.groupMembers(groupId).child(adminId).setValue(adminAsMember);
                    FirebasePaths.user(adminId).child("groupsJoined").child(groupId).setValue(true);
                    GroupNotificationManager.subscribeToGroup(this, groupId);
                    showGroupCodeDialog(groupId, groupCode);
                })
                .addOnFailureListener(e -> {
                    resetCreateButton();
                    Toast.makeText(this, "Could not create group: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetCreateButton() {
        btnCreate.setEnabled(true);
        btnCreate.setText("Create group securely");
    }

    private void showGroupCodeDialog(String groupId, String code) {
        new AlertDialog.Builder(this)
                .setTitle("Group created")
                .setMessage("Your Bhishi is ready. Share the secure QR invite or this 6-digit code with members:\n\n" + code)
                .setPositiveButton("Show QR invite", (dialog, which) -> {
                    Intent invite = new Intent(this, QrInviteActivity.class);
                    invite.putExtra(QrInviteActivity.EXTRA_GROUP_ID, groupId);
                    startActivity(invite);
                    finish();
                })
                .setNegativeButton("Dashboard", (dialog, which) -> {
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}
