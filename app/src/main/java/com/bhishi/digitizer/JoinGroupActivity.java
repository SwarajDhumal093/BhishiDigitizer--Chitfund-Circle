package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
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
import com.bhishi.digitizer.utils.QrUtils;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JoinGroupActivity extends BaseActivity {

    private EditText etGroupCode;
    private TextView btnJoin;
    private GmsBarcodeScanner scanner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_group);

        etGroupCode = findViewById(R.id.etGroupCode);
        btnJoin = findViewById(R.id.btnJoin);
        scanner = GmsBarcodeScanning.getClient(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnScanQr).setOnClickListener(v -> scanInvite());
        btnJoin.setOnClickListener(v -> verifyCodeAndPreview());
    }

    private void scanInvite() {
        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    QrUtils.InvitePayload payload = QrUtils.parseInvite(barcode.getRawValue());
                    if (payload == null) {
                        Toast.makeText(this, R.string.invalid_invite, Toast.LENGTH_LONG).show();
                        return;
                    }
                    etGroupCode.setText(payload.groupCode);
                    verifySpecificGroup(payload.groupId, payload.groupCode);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Could not scan QR: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void verifyCodeAndPreview() {
        String code = etGroupCode.getText().toString().trim();
        if (!code.matches("\\d{6}")) {
            etGroupCode.setError("Enter the full 6-digit code");
            etGroupCode.requestFocus();
            return;
        }
        setLoading(true);
        Query query = FirebasePaths.groups().orderByChild("groupCode").equalTo(code);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    setLoading(false);
                    etGroupCode.setError("No group found with this code");
                    return;
                }
                DataSnapshot groupSnap = snapshot.getChildren().iterator().next();
                showPreview(groupSnap);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(JoinGroupActivity.this, "Could not verify code: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void verifySpecificGroup(String groupId, String code) {
        setLoading(true);
        FirebasePaths.group(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || !code.equals(snapshot.child("groupCode").getValue(String.class))) {
                    setLoading(false);
                    Toast.makeText(JoinGroupActivity.this, R.string.invalid_invite, Toast.LENGTH_LONG).show();
                    return;
                }
                showPreview(snapshot);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(JoinGroupActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showPreview(DataSnapshot groupSnap) {
        setLoading(false);
        String groupId = groupSnap.getKey();
        Group group = groupSnap.getValue(Group.class);
        if (groupId == null || group == null) return;
        group.groupId = groupId;
        group.memberCount = (int) groupSnap.child("members").getChildrenCount();
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currency.setMaximumFractionDigits(0);
        String mode = "auction".equalsIgnoreCase(group.mode) ? getString(R.string.sealed_auction) : getString(R.string.lucky_draw);
        String message = currency.format(group.monthlyAmount) + " / month\n" +
                group.durationMonths + " months  •  " + group.memberCount + " members\n" +
                "Payout: " + mode + "\nAdmin: " + (group.adminName == null ? "Admin" : group.adminName);
        new AlertDialog.Builder(this)
                .setTitle(group.groupName)
                .setMessage(message)
                .setPositiveButton(R.string.join_now, (d, w) -> checkThenAddMember(groupId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkThenAddMember(String groupId) {
        PrefsManager prefs = new PrefsManager(this);
        String uid = prefs.getUid();
        if (uid == null) {
            Toast.makeText(this, "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show();
            return;
        }
        FirebasePaths.groupMembers(groupId).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    FirebasePaths.user(uid).child("groupsJoined").child(groupId).setValue(true);
                    GroupNotificationManager.subscribeToGroup(JoinGroupActivity.this, groupId);
                    Toast.makeText(JoinGroupActivity.this, "You are already a member of this group", Toast.LENGTH_SHORT).show();
                    openDashboard();
                    return;
                }
                addMemberToGroup(groupId, uid, prefs.getName());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(JoinGroupActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addMemberToGroup(String groupId, String uid, String name) {
        String joinedOn = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        GroupMember member = new GroupMember(uid, name == null ? "Member" : name, "", joinedOn);
        FirebasePaths.groupMembers(groupId).child(uid).setValue(member)
                .addOnSuccessListener(unused -> FirebasePaths.user(uid).child("groupsJoined").child(groupId).setValue(true)
                        .addOnCompleteListener(task -> {
                            GroupNotificationManager.subscribeToGroup(this, groupId);
                            Toast.makeText(this, "Joined group successfully", Toast.LENGTH_SHORT).show();
                            openDashboard();
                        }))
                .addOnFailureListener(e -> Toast.makeText(this, "Could not join: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setLoading(boolean loading) {
        btnJoin.setEnabled(!loading);
        btnJoin.setText(loading ? "Verifying…" : "Verify code & preview");
    }

    private void openDashboard() {
        Intent intent = new Intent(this, MemberDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
