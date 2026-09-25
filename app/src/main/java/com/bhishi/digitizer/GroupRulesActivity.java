package com.bhishi.digitizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.CheckBox;
import android.widget.EditText;
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

public class GroupRulesActivity extends BaseActivity {
    public static final String EXTRA_GROUP_ID = "group_id";

    private String groupId;
    private EditText etDueDay, etGraceDays, etLateFee;
    private CheckBox checkOfflineAllowed, checkExcludeWinners;
    private TextView tvGroupName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_rules);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        etDueDay = findViewById(R.id.etDueDay);
        etGraceDays = findViewById(R.id.etGraceDays);
        etLateFee = findViewById(R.id.etLateFee);
        checkOfflineAllowed = findViewById(R.id.checkOfflineAllowed);
        checkExcludeWinners = findViewById(R.id.checkExcludeWinners);
        tvGroupName = findViewById(R.id.tvGroupName);
        findViewById(R.id.btnSaveRules).setOnClickListener(v -> saveRules());
        loadGroup();
    }

    private void loadGroup() {
        if (groupId == null) return;
        FirebasePaths.group(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Group group = snapshot.getValue(Group.class);
                if (group == null) return;
                tvGroupName.setText(group.groupName);
                etDueDay.setText(String.valueOf(group.dueDay <= 0 ? 5 : group.dueDay));
                etGraceDays.setText(String.valueOf(group.graceDays <= 0 ? 3 : group.graceDays));
                etLateFee.setText(String.valueOf(group.lateFeeAmount));
                checkOfflineAllowed.setChecked(group.allowOfflinePayments);
                checkExcludeWinners.setChecked(group.excludePreviousWinners);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void saveRules() {
        if (groupId == null) return;
        String due = etDueDay.getText().toString().trim();
        String grace = etGraceDays.getText().toString().trim();
        String fee = etLateFee.getText().toString().trim();
        if (TextUtils.isEmpty(due) || TextUtils.isEmpty(grace) || TextUtils.isEmpty(fee)) {
            Toast.makeText(this, R.string.fill_all_rule_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("dueDay", Integer.parseInt(due));
        updates.put("graceDays", Integer.parseInt(grace));
        updates.put("lateFeeAmount", Double.parseDouble(fee));
        updates.put("allowOfflinePayments", checkOfflineAllowed.isChecked());
        updates.put("excludePreviousWinners", checkExcludeWinners.isChecked());
        FirebasePaths.group(groupId).updateChildren(updates)
                .addOnSuccessListener(unused -> Toast.makeText(this, R.string.rules_saved, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
