package com.bhishi.digitizer;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class TrustScoreActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trust_score);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        loadExplainableScore();
    }

    private void loadExplainableScore() {
        String uid = new PrefsManager(this).getUid();
        if (uid == null) return;
        TextView tvScore = findViewById(R.id.tvScore);
        TextView tvSummary = findViewById(R.id.tvSummary);
        TextView tvReasonOne = findViewById(R.id.tvReasonOne);
        TextView tvReasonTwo = findViewById(R.id.tvReasonTwo);
        TextView tvReasonThree = findViewById(R.id.tvReasonThree);

        FirebasePaths.user(uid).child("groupsJoined").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                int groupCount = (int) snapshot.getChildrenCount();
                if (groupCount == 0) {
                    applyScore(tvScore, tvSummary, tvReasonOne, tvReasonTwo, tvReasonThree, 45, 0, 0, 0, 0, 0);
                    return;
                }

                final int expectedCallbacks = groupCount * 2;
                final int[] callbacks = {0};
                final int[] records = {0};
                final int[] locked = {0};
                final int[] onTime = {0};
                final int[] disputes = {0};
                final int[] openDisputes = {0};

                Runnable finishIfReady = () -> {
                    if (callbacks[0] < expectedCallbacks) return;
                    int participation = Math.min(10, groupCount * 3);
                    int lockedPoints = records[0] == 0 ? 0 : Math.round(30f * locked[0] / records[0]);
                    int onTimePoints = records[0] == 0 ? 0 : Math.round(20f * onTime[0] / records[0]);
                    int penalty = Math.min(20, openDisputes[0] * 8 + Math.max(0, disputes[0] - openDisputes[0]) * 2);
                    int score = Math.max(30, Math.min(100, 40 + participation + lockedPoints + onTimePoints - penalty));
                    applyScore(tvScore, tvSummary, tvReasonOne, tvReasonTwo, tvReasonThree,
                            score, groupCount, records[0], locked[0], onTime[0], openDisputes[0]);
                };

                for (DataSnapshot joined : snapshot.getChildren()) {
                    String groupId = joined.getKey();
                    FirebasePaths.contributionsGroup(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot months) {
                            for (DataSnapshot month : months.getChildren()) {
                                DataSnapshot contribution = month.child(uid);
                                if (!contribution.exists()) continue;
                                records[0]++;
                                boolean member = Boolean.TRUE.equals(contribution.child("confirmedByMember").getValue(Boolean.class));
                                boolean admin = Boolean.TRUE.equals(contribution.child("confirmedByAdmin").getValue(Boolean.class));
                                if (member && admin) locked[0]++;
                                if (Boolean.TRUE.equals(contribution.child("onTime").getValue(Boolean.class))) onTime[0]++;
                            }
                            callbacks[0]++;
                            finishIfReady.run();
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { callbacks[0]++; finishIfReady.run(); }
                    });

                    FirebasePaths.disputes(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot disputeSnap) {
                            for (DataSnapshot child : disputeSnap.getChildren()) {
                                if (!uid.equals(child.child("raisedByUid").getValue(String.class))) continue;
                                disputes[0]++;
                                if (!"resolved".equalsIgnoreCase(child.child("status").getValue(String.class))) openDisputes[0]++;
                            }
                            callbacks[0]++;
                            finishIfReady.run();
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { callbacks[0]++; finishIfReady.run(); }
                    });
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                tvSummary.setText(R.string.trust_score_load_failed);
            }
        });
    }

    private void applyScore(TextView tvScore, TextView tvSummary, TextView one, TextView two, TextView three,
                            int score, int groups, int records, int locked, int onTime, int openDisputes) {
        tvScore.setText(String.valueOf(score));
        tvSummary.setText(getString(R.string.trust_score_summary, score));
        one.setText("• " + getString(R.string.trust_reason_verified, locked, records));
        two.setText("• " + getString(R.string.trust_reason_ontime, onTime, records, groups));
        three.setText("• " + getString(R.string.trust_reason_disputes, openDisputes));
    }
}
