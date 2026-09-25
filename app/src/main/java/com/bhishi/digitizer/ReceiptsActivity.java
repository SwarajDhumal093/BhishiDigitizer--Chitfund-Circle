package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.adapters.ReceiptAdapter;
import com.bhishi.digitizer.models.Contribution;
import com.bhishi.digitizer.models.Group;
import com.bhishi.digitizer.models.Receipt;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReceiptsActivity extends BaseActivity {
    private ReceiptAdapter adapter;
    private final List<Receipt> receipts = new ArrayList<>();
    private String uid;
    private TextView tvSummary;
    private View emptyState;
    private int groupsExpected;
    private int groupsFinished;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipts);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tvSummary = findViewById(R.id.tvReceiptSummary);
        emptyState = findViewById(R.id.emptyState);
        RecyclerView rv = findViewById(R.id.rvReceipts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReceiptAdapter(this::openReceipt);
        rv.setAdapter(adapter);
        uid = new PrefsManager(this).getUid();
    }

    @Override protected void onResume() {
        super.onResume();
        loadReceipts();
    }

    private void loadReceipts() {
        receipts.clear();
        adapter.setItems(receipts);
        emptyState.setVisibility(View.GONE);
        tvSummary.setText("Loading your contribution receipts…");
        if (uid == null) return;
        FirebasePaths.user(uid).child("groupsJoined").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot joined) {
                groupsExpected = (int) joined.getChildrenCount();
                groupsFinished = 0;
                if (groupsExpected == 0) { finishLoading(); return; }
                for (DataSnapshot child : joined.getChildren()) loadGroupReceipts(child.getKey());
            }
            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { finishLoading(); }
        });
    }

    private void loadGroupReceipts(String groupId) {
        if (groupId == null) { groupFinished(); return; }
        FirebasePaths.group(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot groupSnap) {
                Group group = groupSnap.getValue(Group.class);
                if (group == null) { groupFinished(); return; }
                FirebasePaths.contributionsGroup(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot months) {
                        for (DataSnapshot month : months.getChildren()) {
                            Contribution c = month.child(uid).getValue(Contribution.class);
                            if (c == null || (!c.paid && !c.confirmedByMember && !c.paymentVerified)) continue;
                            Receipt r = new Receipt();
                            r.groupId = groupId;
                            r.groupName = group.groupName;
                            r.monthKey = month.getKey();
                            r.memberName = c.memberName == null ? new PrefsManager(ReceiptsActivity.this).getName() : c.memberName;
                            r.amount = c.amount > 0 ? c.amount : group.monthlyAmount;
                            r.timestamp = c.timestamp;
                            r.paymentMethod = c.paymentMethod;
                            r.paymentStatus = c.paymentStatus;
                            r.paymentId = c.paymentId;
                            r.confirmedByAdmin = c.confirmedByAdmin;
                            r.confirmedByMember = c.confirmedByMember;
                            r.receiptId = buildReceiptId(groupId, r.monthKey, uid);
                            receipts.add(r);
                        }
                        groupFinished();
                    }
                    @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { groupFinished(); }
                });
            }
            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { groupFinished(); }
        });
    }

    private void groupFinished() {
        groupsFinished++;
        if (groupsFinished >= groupsExpected) finishLoading();
    }

    private void finishLoading() {
        Collections.sort(receipts, (a, b) -> Long.compare(b.timestamp, a.timestamp));
        adapter.setItems(new ArrayList<>(receipts));
        emptyState.setVisibility(receipts.isEmpty() ? View.VISIBLE : View.GONE);
        tvSummary.setText(receipts.isEmpty() ? "No contribution receipts yet" : receipts.size() + (receipts.size() == 1 ? " receipt ready" : " receipts ready"));
    }

    private void openReceipt(Receipt r) {
        Intent i = new Intent(this, ReceiptDetailActivity.class);
        i.putExtra("receiptId", r.receiptId);
        i.putExtra("groupId", r.groupId);
        i.putExtra("groupName", r.groupName);
        i.putExtra("monthKey", r.monthKey);
        i.putExtra("memberName", r.memberName);
        i.putExtra("amount", r.amount);
        i.putExtra("timestamp", r.timestamp);
        i.putExtra("paymentMethod", r.paymentMethod);
        i.putExtra("paymentStatus", r.paymentStatus);
        i.putExtra("paymentId", r.paymentId);
        i.putExtra("memberVerified", r.confirmedByMember);
        i.putExtra("adminVerified", r.confirmedByAdmin);
        startActivity(i);
    }

    private String buildReceiptId(String groupId, String month, String userId) {
        String g = groupId.length() > 5 ? groupId.substring(groupId.length() - 5) : groupId;
        String u = userId.length() > 4 ? userId.substring(userId.length() - 4) : userId;
        return "BD-" + (month == null ? "CYCLE" : month.replace("-", "")) + "-" + g.toUpperCase() + "-" + u.toUpperCase();
    }
}
