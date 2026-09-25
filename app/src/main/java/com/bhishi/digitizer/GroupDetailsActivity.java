package com.bhishi.digitizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bhishi.digitizer.fragments.GroupPagerAdapter;
import com.bhishi.digitizer.models.Group;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Locale;

public class GroupDetailsActivity extends BaseActivity {

    public static final String EXTRA_GROUP_ID = "extra_group_id";
    public static final String EXTRA_IS_ADMIN = "extra_is_admin";
    public static final String EXTRA_START_TAB = "extra_start_tab";
    private static final String[] TAB_TITLES = {"Contributions", "Payout", "Members", "History"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        String groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        boolean isAdmin = getIntent().getBooleanExtra(EXTRA_IS_ADMIN, false);
        if (groupId == null) { finish(); return; }

        TextView tvGroupName = findViewById(R.id.tvGroupName);
        TextView tvGroupMeta = findViewById(R.id.tvGroupMeta);
        TextView tvRoleBadge = findViewById(R.id.tvRoleBadge);
        TextView tvAmountSummary = findViewById(R.id.tvAmountSummary);
        TextView tvModeSummary = findViewById(R.id.tvModeSummary);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        tvRoleBadge.setText(isAdmin ? "ADMIN" : "MEMBER");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        View btnInviteQr = findViewById(R.id.btnInviteQr);
        btnInviteQr.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        btnInviteQr.setOnClickListener(v -> {
            Intent invite = new Intent(this, QrInviteActivity.class);
            invite.putExtra(QrInviteActivity.EXTRA_GROUP_ID, groupId);
            startActivity(invite);
        });

        FirebasePaths.group(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Group group = snapshot.getValue(Group.class);
                if (group == null) return;

                NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                currency.setMaximumFractionDigits(0);
                group.memberCount = (int) snapshot.child("members").getChildrenCount();
                String modeLabel = "auction".equalsIgnoreCase(group.mode) ? "Sealed auction" : "Lucky draw";
                tvGroupName.setText(group.groupName);
                tvGroupMeta.setText(group.memberCount + " members • " + group.durationMonths + " months • code " + group.groupCode);
                tvAmountSummary.setText(currency.format(group.monthlyAmount));
                tvModeSummary.setText(modeLabel);

                GroupPagerAdapter pagerAdapter = new GroupPagerAdapter(GroupDetailsActivity.this, groupId, group.monthlyAmount, group.mode, isAdmin);
                viewPager.setAdapter(pagerAdapter);
                new TabLayoutMediator(tabLayout, viewPager,
                        (tab, position) -> tab.setText(TAB_TITLES[position])).attach();
                int startTab = Math.max(0, Math.min(3, getIntent().getIntExtra(EXTRA_START_TAB, 0)));
                viewPager.setCurrentItem(startTab, false);
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
        });
    }
}
