package com.bhishi.digitizer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.adapters.PassbookAdapter;
import com.bhishi.digitizer.models.Contribution;
import com.bhishi.digitizer.models.Payout;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PdfPassbookGenerator;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Member-facing immutable-style ledger summary across all joined Bhishi groups. */
public class PassbookActivity extends BaseActivity {

    private final List<Contribution> allContributions = new ArrayList<>();
    private PassbookAdapter adapter;
    private TextView tvTotalContributed, tvTotalReceived;
    private double totalContributed = 0;
    private double totalReceived = 0;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passbook);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        currency.setMaximumFractionDigits(0);
        tvTotalContributed = findViewById(R.id.tvTotalContributed);
        tvTotalReceived = findViewById(R.id.tvTotalReceived);

        RecyclerView rv = findViewById(R.id.rvPassbook);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PassbookAdapter();
        rv.setAdapter(adapter);

        findViewById(R.id.btnDownloadPdf).setOnClickListener(v -> {
            if (allContributions.isEmpty()) {
                Toast.makeText(this, "No passbook entries to export yet", Toast.LENGTH_SHORT).show();
                return;
            }
            String name = new PrefsManager(this).getName();
            PdfPassbookGenerator.generate(this, name != null ? name : "Member", "All groups", allContributions);
        });

        loadPassbook();
    }

    private void loadPassbook() {
        String myUid = new PrefsManager(this).getUid();
        if (myUid == null) return;

        allContributions.clear();
        totalContributed = 0;
        totalReceived = 0;
        renderTotals();

        FirebasePaths.user(myUid).child("groupsJoined").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot joinedSnapshot) {
                if (!joinedSnapshot.exists()) {
                    adapter.setRows(Collections.emptyList());
                    return;
                }
                for (DataSnapshot joined : joinedSnapshot.getChildren()) {
                    String groupId = joined.getKey();
                    if (groupId != null) {
                        loadGroupContributions(groupId, myUid);
                        loadGroupPayouts(groupId, myUid);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PassbookActivity.this, "Could not load passbook", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGroupContributions(String groupId, String myUid) {
        FirebasePaths.contributionsGroup(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot groupNode) {
                for (DataSnapshot monthSnap : groupNode.getChildren()) {
                    Contribution c = monthSnap.child(myUid).getValue(Contribution.class);
                    if (c != null) {
                        c.memberName = readableMonth(monthSnap.getKey());
                        allContributions.add(c);
                        if (c.isLocked()) totalContributed += c.amount;
                    }
                }
                // Newer month labels sort first because the stored key is yyyy-MM.
                Collections.sort(allContributions, (a, b) -> String.valueOf(b.memberName).compareTo(String.valueOf(a.memberName)));
                adapter.setRows(allContributions);
                renderTotals();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void loadGroupPayouts(String groupId, String myUid) {
        FirebasePaths.payoutsGroup(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double groupReceived = 0;
                for (DataSnapshot month : snapshot.getChildren()) {
                    Payout payout = month.getValue(Payout.class);
                    if (payout == null || !payout.lockedResult) continue;
                    if (myUid.equals(payout.winnerUid)) {
                        groupReceived += "auction".equalsIgnoreCase(payout.mode) ? payout.bidAmount : payout.poolAmount;
                    } else if ("auction".equalsIgnoreCase(payout.mode)) {
                        groupReceived += payout.dividendPerMember;
                    }
                }
                totalReceived += groupReceived;
                renderTotals();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void renderTotals() {
        tvTotalContributed.setText(currency.format(totalContributed));
        tvTotalReceived.setText(currency.format(totalReceived));
    }

    private String readableMonth(String monthKey) {
        if (monthKey == null) return "Contribution";
        try {
            java.text.SimpleDateFormat source = new java.text.SimpleDateFormat("yyyy-MM", Locale.US);
            java.text.SimpleDateFormat output = new java.text.SimpleDateFormat("MMM yyyy", Locale.getDefault());
            java.util.Date date = source.parse(monthKey);
            return date == null ? monthKey : output.format(date);
        } catch (Exception ignored) {
            return monthKey;
        }
    }
}
