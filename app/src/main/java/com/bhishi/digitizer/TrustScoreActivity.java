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
                int base = 50 + Math.min(25, groupCount * 8);
                int score = Math.min(98, base + 12);
                tvScore.setText(String.valueOf(score));
                tvSummary.setText(getString(R.string.trust_score_summary, score));
                tvReasonOne.setText("• " + getString(R.string.trust_reason_payments, Math.max(1, groupCount)));
                tvReasonTwo.setText("• " + getString(R.string.trust_reason_transparency));
                tvReasonThree.setText("• " + getString(R.string.trust_reason_consistency));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
}
