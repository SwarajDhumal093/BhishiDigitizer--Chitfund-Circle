package com.bhishi.digitizer;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bhishi.digitizer.models.Group;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Locale;

public class AnalyticsReportsActivity extends BaseActivity {
    private TextView tvTotalGroups, tvCompletionRate, tvPoolValue, tvMemberCount, tvInsightOne, tvInsightTwo, tvInsightThree;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics_reports);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvTotalGroups = findViewById(R.id.tvTotalGroups);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvPoolValue = findViewById(R.id.tvPoolValue);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvInsightOne = findViewById(R.id.tvInsightOne);
        tvInsightTwo = findViewById(R.id.tvInsightTwo);
        tvInsightThree = findViewById(R.id.tvInsightThree);
        loadData();
    }

    private void loadData() {
        String uid = new PrefsManager(this).getUid();
        if (uid == null) return;
        FirebasePaths.user(uid).child("groupsJoined").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    applyMetrics(0,0,0,0,0);
                    return;
                }
                final int total = (int) snapshot.getChildrenCount();
                final int[] done = {0};
                final int[] groups = {0};
                final int[] members = {0};
                final int[] paid = {0};
                final int[] expected = {0};
                final double[] pool = {0};
                for (DataSnapshot joined : snapshot.getChildren()) {
                    String groupId = joined.getKey();
                    FirebasePaths.group(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot groupSnap) {
                            Group g = groupSnap.getValue(Group.class);
                            if (g != null) {
                                groups[0]++;
                                int memberCount = (int) groupSnap.child("members").getChildrenCount();
                                if (memberCount <= 0) memberCount = g.memberCount;
                                members[0] += Math.max(0, memberCount);
                                pool[0] += g.monthlyAmount * Math.max(1, memberCount);
                                expected[0] += Math.max(1, memberCount);
                            }
                            FirebasePaths.contributions(groupId, FirebasePaths.currentMonthKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override public void onDataChange(@NonNull DataSnapshot contribSnap) {
                                    for (DataSnapshot child : contribSnap.getChildren()) {
                                        boolean member = Boolean.TRUE.equals(child.child("confirmedByMember").getValue(Boolean.class));
                                        boolean admin = Boolean.TRUE.equals(child.child("confirmedByAdmin").getValue(Boolean.class));
                                        if (member && admin) paid[0]++;
                                    }
                                    done[0]++;
                                    if (done[0] >= total) applyMetrics(groups[0], members[0], paid[0], expected[0], pool[0]);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) { done[0]++; if (done[0] >= total) applyMetrics(groups[0], members[0], paid[0], expected[0], pool[0]); }
                            });
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { done[0]++; if (done[0] >= total) applyMetrics(groups[0], members[0], paid[0], expected[0], pool[0]); }
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void applyMetrics(int groupCount, int members, int paid, int expected, double pool) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currency.setMaximumFractionDigits(0);
        int completion = expected <= 0 ? 0 : Math.round((paid * 100f) / expected);
        tvTotalGroups.setText(String.valueOf(groupCount));
        tvCompletionRate.setText(completion + "%");
        tvPoolValue.setText(currency.format(pool));
        tvMemberCount.setText(String.valueOf(members));
        tvInsightOne.setText("• " + paid + " of " + expected + " expected contributions are fully verified in the current cycle.");
        tvInsightTwo.setText("• Average members per group: " + (groupCount == 0 ? 0 : Math.round(members / (float) groupCount)) + ".");
        tvInsightThree.setText("• Explainable analytics are generated directly from your live groups, contributions and locked entries.");
    }
}
