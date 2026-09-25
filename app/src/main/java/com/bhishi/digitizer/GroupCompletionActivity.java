package com.bhishi.digitizer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bhishi.digitizer.models.Group;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class GroupCompletionActivity extends BaseActivity {
    public static final String EXTRA_GROUP_ID = "group_id";
    private String groupId;
    private TextView tvGroupName, tvClosureStatus, tvClosureDetail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_completion);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        tvGroupName = findViewById(R.id.tvGroupName);
        tvClosureStatus = findViewById(R.id.tvClosureStatus);
        tvClosureDetail = findViewById(R.id.tvClosureDetail);
        findViewById(R.id.btnArchiveGroup).setOnClickListener(v -> archiveGroup());
        loadSummary();
    }

    private void loadSummary() {
        if (groupId == null) return;
        FirebasePaths.group(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Group group = snapshot.getValue(Group.class);
                if (group == null) return;
                tvGroupName.setText(group.groupName);
                int memberCount = (int) snapshot.child("members").getChildrenCount();
                boolean archived = group.archivedAt > 0 || group.cycleCompleted;
                tvClosureStatus.setText(archived ? getString(R.string.group_already_archived) : getString(R.string.group_ready_for_review));
                tvClosureDetail.setText(getString(R.string.group_completion_detail, memberCount, group.durationMonths));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void archiveGroup() {
        if (groupId == null) return;
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
