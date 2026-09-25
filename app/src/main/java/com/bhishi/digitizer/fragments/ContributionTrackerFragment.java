package com.bhishi.digitizer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.R;
import com.bhishi.digitizer.PaymentActivity;
import com.bhishi.digitizer.adapters.ContributionAdapter;
import com.bhishi.digitizer.models.Contribution;
import com.bhishi.digitizer.models.GroupMember;
import com.bhishi.digitizer.utils.PrefsManager;
import com.bhishi.digitizer.utils.FirebasePaths;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContributionTrackerFragment extends Fragment {

    private static final String ARG_GROUP_ID = "group_id";
    private static final String ARG_MONTHLY_AMOUNT = "monthly_amount";
    private static final String ARG_IS_ADMIN = "is_admin";

    private String groupId;
    private double monthlyAmount;
    private boolean isAdmin;
    private ContributionAdapter adapter;
    private TextView tvCollectedSummary;
    private ProgressBar progressCollected;
    private TextView btnPayContribution;
    private TextView tvContributionHelp;

    public static ContributionTrackerFragment newInstance(String groupId, double monthlyAmount, boolean isAdmin) {
        ContributionTrackerFragment fragment = new ContributionTrackerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        args.putDouble(ARG_MONTHLY_AMOUNT, monthlyAmount);
        args.putBoolean(ARG_IS_ADMIN, isAdmin);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contribution_tracker, container, false);
        if (getArguments() != null) {
            groupId = getArguments().getString(ARG_GROUP_ID);
            monthlyAmount = getArguments().getDouble(ARG_MONTHLY_AMOUNT);
            isAdmin = getArguments().getBoolean(ARG_IS_ADMIN, false);
        }

        tvCollectedSummary = view.findViewById(R.id.tvCollectedSummary);
        progressCollected = view.findViewById(R.id.progressCollected);
        btnPayContribution = view.findViewById(R.id.btnPayContribution);
        tvContributionHelp = view.findViewById(R.id.tvContributionHelp);
        RecyclerView rv = view.findViewById(R.id.rvContributions);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContributionAdapter(isAdmin, (contribution, confirmed) -> {
            String monthKey = FirebasePaths.currentMonthKey();
            FirebasePaths.contributions(groupId, monthKey).child(contribution.uid)
                    .child("confirmedByAdmin").setValue(confirmed)
                    .addOnSuccessListener(unused -> loadContributions())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Could not update: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
        rv.setAdapter(adapter);
        btnPayContribution.setOnClickListener(v -> openPayment());
        tvContributionHelp.setText(R.string.locked_hint);
        loadContributions();
        return view;
    }

    private void openPayment() {
        Intent intent = new Intent(requireContext(), PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_GROUP_ID, groupId);
        intent.putExtra(PaymentActivity.EXTRA_AMOUNT, monthlyAmount);
        intent.putExtra(PaymentActivity.EXTRA_GROUP_NAME, getString(R.string.monthly_contribution));
        startActivity(intent);
    }

    private void loadContributions() {
        String monthKey = FirebasePaths.currentMonthKey();
        FirebasePaths.groupMembers(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot membersSnapshot) {
                List<GroupMember> members = new ArrayList<>();
                for (DataSnapshot m : membersSnapshot.getChildren()) {
                    GroupMember member = m.getValue(GroupMember.class);
                    if (member != null) members.add(member);
                }
                loadOrCreateContributions(members, monthKey);
            }
            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
        });
    }

    private void loadOrCreateContributions(List<GroupMember> members, String monthKey) {
        FirebasePaths.contributions(groupId, monthKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Contribution> contributions = new ArrayList<>();
                int fullyVerified = 0;
                int submitted = 0;

                for (GroupMember member : members) {
                    DataSnapshot existing = snapshot.child(member.uid);
                    Contribution c = existing.exists() ? existing.getValue(Contribution.class) : new Contribution(member.uid, member.name, monthlyAmount);
                    if (c == null) continue;
                    c.uid = member.uid;
                    c.memberName = member.name;
                    contributions.add(c);
                    if (c.confirmedByMember) submitted++;
                    if (c.isLocked()) fullyVerified++;
                    if (!existing.exists() && isAdmin) {
                        FirebasePaths.contributions(groupId, monthKey).child(member.uid).setValue(c);
                    }
                }

                adapter.setContributions(contributions);
                String myUid = new PrefsManager(requireContext()).getUid();
                boolean myContributionLocked = false;
                boolean myContributionSubmitted = false;
                for (Contribution c : contributions) {
                    if (c.uid != null && c.uid.equals(myUid)) {
                        myContributionLocked = c.isLocked();
                        myContributionSubmitted = c.confirmedByMember;
                        break;
                    }
                }
                btnPayContribution.setEnabled(!myContributionLocked);
                btnPayContribution.setAlpha(myContributionLocked ? 0.65f : 1f);
                btnPayContribution.setText(myContributionLocked
                        ? getString(R.string.contribution_paid)
                        : (myContributionSubmitted ? getString(R.string.submitted_waiting_admin) : getString(R.string.pay_my_contribution)));
                NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                currency.setMaximumFractionDigits(0);
                double verifiedValue = fullyVerified * monthlyAmount;
                double target = members.size() * monthlyAmount;
                tvCollectedSummary.setText(currency.format(verifiedValue) + " verified of " + currency.format(target) + " • " + submitted + "/" + members.size() + " submitted");
                progressCollected.setProgress(members.isEmpty() ? 0 : (int) ((fullyVerified / (float) members.size()) * 100));
            }
            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) { }
        });
    }
}
