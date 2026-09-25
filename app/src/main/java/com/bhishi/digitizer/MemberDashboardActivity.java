package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.adapters.MemberDueAdapter;
import com.bhishi.digitizer.models.Group;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MemberDashboardActivity extends BaseActivity {

    private MemberDueAdapter adapter;
    private String myUid;
    private TextView tvMemberSummary, tvMemberSubSummary, tvGreeting;
    private TextView tvMemberGroups, tvMemberDue, tvMemberPending;
    private final List<Group> loadedGroups = new ArrayList<>();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_dashboard);

        PrefsManager prefs = new PrefsManager(this);
        myUid = prefs.getUid();
        tvMemberSummary = findViewById(R.id.tvMemberSummary);
        tvMemberSubSummary = findViewById(R.id.tvMemberSubSummary);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvMemberGroups = findViewById(R.id.tvMemberGroups);
        tvMemberDue = findViewById(R.id.tvMemberDue);
        tvMemberPending = findViewById(R.id.tvMemberPending);
        if (prefs.getName() != null && !prefs.getName().trim().isEmpty()) tvGreeting.setText(getString(R.string.welcome_back) + ", " + firstName(prefs.getName()));

        RecyclerView rvGroups = findViewById(R.id.rvGroups);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemberDueAdapter(new MemberDueAdapter.Listener() {
            @Override public void onCardClick(Group group) { openGroup(group); }
            @Override public void onPayNowClick(Group group) { openPayment(group); }
        });
        rvGroups.setAdapter(adapter);
        rvGroups.setNestedScrollingEnabled(false);

        findViewById(R.id.btnJoinGroup).setOnClickListener(v -> startActivity(new Intent(this, JoinGroupActivity.class)));
        findViewById(R.id.quickPay).setOnClickListener(v -> payFirstDue());
        findViewById(R.id.quickReceipts).setOnClickListener(v -> startActivity(new Intent(this, ReceiptsActivity.class)));
        findViewById(R.id.quickTrust).setOnClickListener(v -> startActivity(new Intent(this, TrustScoreActivity.class)));
        findViewById(R.id.quickCalendar).setOnClickListener(v -> startActivity(new Intent(this, FinancialCalendarActivity.class)));
        findViewById(R.id.quickReports).setOnClickListener(v -> startActivity(new Intent(this, AnalyticsReportsActivity.class)));
        findViewById(R.id.btnNotifications).setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.btnProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.navPassbook).setOnClickListener(v -> startActivity(new Intent(this, PassbookActivity.class)));
        findViewById(R.id.navReceipts).setOnClickListener(v -> startActivity(new Intent(this, ReceiptsActivity.class)));
        findViewById(R.id.navDisputes).setOnClickListener(v -> startActivity(new Intent(this, DisputeLogActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override protected void onResume() { super.onResume(); loadMyGroups(); }

    private void openGroup(Group group) {
        Intent intent = new Intent(this, GroupDetailsActivity.class);
        intent.putExtra(GroupDetailsActivity.EXTRA_GROUP_ID, group.groupId);
        intent.putExtra(GroupDetailsActivity.EXTRA_IS_ADMIN, false);
        startActivity(intent);
    }

    private void openPayment(Group group) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_GROUP_ID, group.groupId);
        intent.putExtra(PaymentActivity.EXTRA_GROUP_NAME, group.groupName);
        intent.putExtra(PaymentActivity.EXTRA_AMOUNT, group.monthlyAmount);
        startActivity(intent);
    }

    private void payFirstDue() {
        for (Group group : loadedGroups) {
            if (group.paidThisMonth == 0) { openPayment(group); return; }
        }
        if (loadedGroups.isEmpty()) Toast.makeText(this, R.string.join_bhishi_first, Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, R.string.no_contribution_due, Toast.LENGTH_SHORT).show();
    }

    private void loadMyGroups() {
        if (myUid == null) return;
        loadedGroups.clear();
        adapter.setGroups(new ArrayList<>());
        tvMemberSummary.setText(R.string.member_banner_default);
        FirebasePaths.user(myUid).child("groupsJoined").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot joinedSnapshot) {
                int expected = (int) joinedSnapshot.getChildrenCount();
                if (expected == 0) {
                    updateMemberSummary();
                    tvMemberSubSummary.setText(R.string.scan_invite_first);
                    return;
                }
                final int[] loaded = {0};
                for (DataSnapshot joined : joinedSnapshot.getChildren()) {
                    String groupId = joined.getKey();
                    FirebasePaths.group(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot groupSnap) {
                            Group group = groupSnap.getValue(Group.class);
                            if (group != null) {
                                group.groupId = groupId;
                                group.memberCount = (int) groupSnap.child("members").getChildrenCount();
                                attachMyPaymentState(group, () -> { loaded[0]++; if (loaded[0] >= expected) updateMemberSummary(); });
                            } else { loaded[0]++; if (loaded[0] >= expected) updateMemberSummary(); }
                        }
                        @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { loaded[0]++; if (loaded[0] >= expected) updateMemberSummary(); }
                    });
                }
            }
            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                tvMemberSubSummary.setText("Could not refresh your groups. Check your connection.");
            }
        });
    }

    private interface Done { void run(); }

    private void attachMyPaymentState(Group group, Done done) {
        FirebasePaths.contributions(group.groupId, FirebasePaths.currentMonthKey()).child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean member = Boolean.TRUE.equals(snapshot.child("confirmedByMember").getValue(Boolean.class));
                        boolean admin = Boolean.TRUE.equals(snapshot.child("confirmedByAdmin").getValue(Boolean.class));
                        group.paidThisMonth = member && admin ? 1 : (member ? 2 : 0);
                        loadedGroups.add(group);
                        adapter.setGroups(new ArrayList<>(loadedGroups));
                        done.run();
                    }
                    @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { done.run(); }
                });
    }

    private void updateMemberSummary() {
        int due = 0, pending = 0;
        for (Group group : loadedGroups) {
            if (group.paidThisMonth == 0) due++;
            else if (group.paidThisMonth == 2) pending++;
        }
        tvMemberGroups.setText(String.valueOf(loadedGroups.size()));
        tvMemberDue.setText(String.valueOf(due));
        tvMemberPending.setText(String.valueOf(pending));
        if (loadedGroups.isEmpty()) {
            tvMemberSummary.setText(R.string.first_bhishi_scan);
            tvMemberSubSummary.setText(R.string.use_qr_or_code);
        } else if (due == 0 && pending == 0) {
            tvMemberSummary.setText(R.string.up_to_date);
            tvMemberSubSummary.setText(R.string.all_ready_passbook);
        } else if (due > 0) {
            tvMemberSummary.setText(getString(R.string.due_summary, due));
            tvMemberSubSummary.setText(pending > 0 ? getString(R.string.pending_admin_summary, pending) : getString(R.string.pay_or_offline));
        } else {
            tvMemberSummary.setText(getString(R.string.pending_summary, pending));
            tvMemberSubSummary.setText(R.string.ledger_locks_after_admin);
        }
    }

    private String firstName(String name) {
        String trimmed = name.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }
}
