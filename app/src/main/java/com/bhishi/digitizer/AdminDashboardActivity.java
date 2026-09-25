package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.adapters.GroupAdapter;
import com.bhishi.digitizer.models.Group;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.bhishi.digitizer.utils.PrefsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminDashboardActivity extends BaseActivity {

    private GroupAdapter adapter;
    private TextView tvAdminSummary, tvAdminSubSummary, tvGreeting;
    private TextView tvMetricGroups, tvMetricDue, tvMetricPool, tvMetricMembers;
    private final List<Group> loadedGroups = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        PrefsManager prefs = new PrefsManager(this);
        tvAdminSummary = findViewById(R.id.tvAdminSummary);
        tvAdminSubSummary = findViewById(R.id.tvAdminSubSummary);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvMetricGroups = findViewById(R.id.tvMetricGroups);
        tvMetricDue = findViewById(R.id.tvMetricDue);
        tvMetricPool = findViewById(R.id.tvMetricPool);
        tvMetricMembers = findViewById(R.id.tvMetricMembers);
        String name = prefs.getName();
        if (name != null && !name.trim().isEmpty()) tvGreeting.setText(getString(R.string.welcome_back) + ", " + firstName(name));

        RecyclerView rvGroups = findViewById(R.id.rvGroups);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroupAdapter(group -> openGroup(group.groupId, 0));
        rvGroups.setAdapter(adapter);
        rvGroups.setNestedScrollingEnabled(false);

        findViewById(R.id.btnProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btnNotifications).setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        findViewById(R.id.quickCreate).setOnClickListener(v -> startActivity(new Intent(this, CreateGroupActivity.class)));
        findViewById(R.id.quickInvite).setOnClickListener(v -> chooseGroup(null, "invite"));
        findViewById(R.id.quickDraw).setOnClickListener(v -> chooseGroup("draw", "payout"));
        findViewById(R.id.quickAuction).setOnClickListener(v -> chooseGroup("auction", "payout"));
        findViewById(R.id.quickReceipts).setOnClickListener(v -> startActivity(new Intent(this, ReceiptsActivity.class)));
        findViewById(R.id.quickReports).setOnClickListener(v -> startActivity(new Intent(this, AnalyticsReportsActivity.class)));
        findViewById(R.id.quickCalendar).setOnClickListener(v -> startActivity(new Intent(this, FinancialCalendarActivity.class)));
        findViewById(R.id.quickRules).setOnClickListener(v -> chooseGroup(null, "rules"));
        findViewById(R.id.quickClosure).setOnClickListener(v -> chooseGroup(null, "closure"));
        findViewById(R.id.navPassbook).setOnClickListener(v -> startActivity(new Intent(this, PassbookActivity.class)));
        findViewById(R.id.navReceipts).setOnClickListener(v -> startActivity(new Intent(this, ReceiptsActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override protected void onResume() {
        super.onResume();
        loadAdminGroups();
    }

    private void openGroup(String groupId, int tab) {
        Intent intent = new Intent(this, GroupDetailsActivity.class);
        intent.putExtra(GroupDetailsActivity.EXTRA_GROUP_ID, groupId);
        intent.putExtra(GroupDetailsActivity.EXTRA_IS_ADMIN, true);
        intent.putExtra(GroupDetailsActivity.EXTRA_START_TAB, tab);
        startActivity(intent);
    }

    private void chooseGroup(String requiredMode, String action) {
        List<Group> options = new ArrayList<>();
        for (Group g : loadedGroups) {
            if (requiredMode == null || requiredMode.equalsIgnoreCase(g.mode)) options.add(g);
        }
        if (options.isEmpty()) {
            Toast.makeText(this, requiredMode == null ? getString(R.string.create_bhishi_first) : "No " + requiredMode + " group is available", Toast.LENGTH_LONG).show();
            return;
        }
        if (options.size() == 1) { runGroupAction(options.get(0), action); return; }
        String[] names = new String[options.size()];
        for (int i = 0; i < options.size(); i++) names[i] = options.get(i).groupName;
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_bhishi)
                .setItems(names, (dialog, which) -> runGroupAction(options.get(which), action))
                .show();
    }

    private void runGroupAction(Group group, String action) {
        if ("invite".equals(action)) {
            Intent i = new Intent(this, QrInviteActivity.class);
            i.putExtra(QrInviteActivity.EXTRA_GROUP_ID, group.groupId);
            startActivity(i);
        } else if ("rules".equals(action)) {
            Intent i = new Intent(this, GroupRulesActivity.class);
            i.putExtra(GroupRulesActivity.EXTRA_GROUP_ID, group.groupId);
            startActivity(i);
        } else if ("closure".equals(action)) {
            Intent i = new Intent(this, GroupCompletionActivity.class);
            i.putExtra(GroupCompletionActivity.EXTRA_GROUP_ID, group.groupId);
            startActivity(i);
        } else {
            openGroup(group.groupId, 1);
        }
    }

    private void loadAdminGroups() {
        String uid = new PrefsManager(this).getUid();
        if (uid == null) return;
        loadedGroups.clear();
        adapter.setGroups(new ArrayList<>());
        Query query = FirebasePaths.groups().orderByChild("adminId").equalTo(uid);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadedGroups.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Group group = child.getValue(Group.class);
                    if (group != null) {
                        group.groupId = child.getKey();
                        group.memberCount = (int) child.child("members").getChildrenCount();
                        group.paidThisMonth = 0;
                        loadedGroups.add(group);
                    }
                }
                adapter.setGroups(new ArrayList<>(loadedGroups));
                refreshMetrics();
                for (Group group : loadedGroups) attachVerifiedCount(group);
            }

            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                tvAdminSubSummary.setText("Could not refresh groups. Check your connection and try again.");
            }
        });
    }

    private void attachVerifiedCount(Group group) {
        FirebasePaths.contributions(group.groupId, FirebasePaths.currentMonthKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int verified = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            boolean member = Boolean.TRUE.equals(child.child("confirmedByMember").getValue(Boolean.class));
                            boolean admin = Boolean.TRUE.equals(child.child("confirmedByAdmin").getValue(Boolean.class));
                            if (member && admin) verified++;
                        }
                        group.paidThisMonth = verified;
                        adapter.setGroups(new ArrayList<>(loadedGroups));
                        refreshMetrics();
                    }
                    @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
                });
    }

    private void refreshMetrics() {
        int groups = loadedGroups.size();
        int members = 0;
        int due = 0;
        double pool = 0;
        for (Group g : loadedGroups) {
            members += Math.max(0, g.memberCount);
            pool += g.monthlyAmount * Math.max(0, g.memberCount);
            due += Math.max(0, g.memberCount - g.paidThisMonth);
        }
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currency.setMaximumFractionDigits(0);
        tvMetricGroups.setText(String.valueOf(groups));
        tvMetricMembers.setText(String.valueOf(members));
        tvMetricDue.setText(String.valueOf(due));
        tvMetricPool.setText(currency.format(pool));

        if (groups == 0) {
            tvAdminSummary.setText(R.string.create_first_bhishi_summary);
            tvAdminSubSummary.setText(R.string.create_first_bhishi_subtitle);
        } else if (due == 0) {
            tvAdminSummary.setText(R.string.all_visible_verified);
            tvAdminSubSummary.setText(getString(R.string.ready_payout_rounds, groups));
        } else {
            tvAdminSummary.setText(getString(R.string.attention_summary, due));
            tvAdminSubSummary.setText(R.string.review_collections);
        }
    }

    private String firstName(String name) {
        String trimmed = name.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }
}
