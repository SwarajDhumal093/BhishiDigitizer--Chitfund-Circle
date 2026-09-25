package com.bhishi.digitizer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bhishi.digitizer.models.Group;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class GroupCompletionActivity extends BaseActivity {
    public static final String EXTRA_GROUP_ID = "group_id";
    private String groupId;
    private TextView tvGroupName, tvClosureStatus, tvClosureDetail, btnArchiveGroup;
    private boolean readyToArchive;
    private boolean alreadyArchived;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_completion);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        tvGroupName = findViewById(R.id.tvGroupName);
        tvClosureStatus = findViewById(R.id.tvClosureStatus);
        tvClosureDetail = findViewById(R.id.tvClosureDetail);
        btnArchiveGroup = findViewById(R.id.btnArchiveGroup);
        btnArchiveGroup.setOnClickListener(v -> archiveGroup());
        loadSummary();
    }

    private void loadSummary() {
        if (groupId == null) return;
        readyToArchive = false;
        btnArchiveGroup.setEnabled(false);
        btnArchiveGroup.setAlpha(0.55f);
        FirebasePaths.group(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Group group = snapshot.getValue(Group.class);
                if (group == null) return;
                String uid = new PrefsManager(GroupCompletionActivity.this).getUid();
                if (uid == null || !uid.equals(group.adminId)) {
                    tvClosureStatus.setText(R.string.admin_only_closure);
                    return;
                }
                tvGroupName.setText(group.groupName);
                int memberCount = (int) snapshot.child("members").getChildrenCount();
                alreadyArchived = group.archivedAt > 0 || group.cycleCompleted;
                if (alreadyArchived) {
                    tvClosureStatus.setText(R.string.group_already_archived);
                    tvClosureDetail.setText(getString(R.string.group_completion_archived_detail, memberCount));
                    btnArchiveGroup.setText(R.string.group_already_archived);
                    return;
                }

                final int requiredCycles = Math.max(1, group.durationMonths > 0 ? group.durationMonths : memberCount);
                final int[] completedPayouts = {0};
                final int[] openDisputes = {0};
                final int[] callbacks = {0};
                Runnable apply = () -> {
                    if (callbacks[0] < 2) return;
                    readyToArchive = completedPayouts[0] >= requiredCycles && openDisputes[0] == 0;
                    tvClosureStatus.setText(readyToArchive ? R.string.group_ready_to_archive : R.string.group_not_ready_to_archive);
                    tvClosureDetail.setText(getString(R.string.group_completion_preflight,
                            completedPayouts[0], requiredCycles, openDisputes[0], memberCount));
                    btnArchiveGroup.setEnabled(readyToArchive);
                    btnArchiveGroup.setAlpha(readyToArchive ? 1f : 0.55f);
                    btnArchiveGroup.setText(readyToArchive ? R.string.archive_group : R.string.resolve_before_archive);
                };

                FirebasePaths.payoutsGroup(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot payoutSnap) {
                        for (DataSnapshot p : payoutSnap.getChildren()) {
                            if (Boolean.TRUE.equals(p.child("lockedResult").getValue(Boolean.class))) completedPayouts[0]++;
                        }
                        callbacks[0]++;
                        apply.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { callbacks[0]++; apply.run(); }
                });

                FirebasePaths.disputes(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot disputeSnap) {
                        for (DataSnapshot d : disputeSnap.getChildren()) {
                            if (!"resolved".equalsIgnoreCase(d.child("status").getValue(String.class))) openDisputes[0]++;
                        }
                        callbacks[0]++;
                        apply.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { callbacks[0]++; apply.run(); }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void archiveGroup() {
        if (groupId == null || alreadyArchived) return;
        if (!readyToArchive) {
            Toast.makeText(this, R.string.group_not_ready_to_archive, Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.archive_group)
                .setMessage(R.string.archive_group_confirmation)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.archive_group, (dialog, which) -> performArchive())
                .show();
    }

    private void performArchive() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("cycleCompleted", true);
        updates.put("closureRequested", true);
        updates.put("archivedAt", System.currentTimeMillis());
        FirebasePaths.group(groupId).updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, R.string.group_archived, Toast.LENGTH_SHORT).show();
                    loadSummary();
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
