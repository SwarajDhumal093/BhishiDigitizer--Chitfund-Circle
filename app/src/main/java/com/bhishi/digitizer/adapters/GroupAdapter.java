package com.bhishi.digitizer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.R;
import com.bhishi.digitizer.models.Group;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    private final List<Group> groups = new ArrayList<>();
    private final OnGroupClickListener listener;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public GroupAdapter(OnGroupClickListener listener) {
        this.listener = listener;
        currency.setMaximumFractionDigits(0);
    }

    public void setGroups(List<Group> newGroups) {
        groups.clear();
        groups.addAll(newGroups);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_card, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groups.get(position);
        int progress = group.progressPercent();
        holder.tvGroupName.setText(group.groupName);
        holder.tvMemberCount.setText(group.memberCount + (group.memberCount == 1 ? " member" : " members") + " • " + group.durationMonths + " months");
        holder.tvMonthlyAmount.setText(currency.format(group.monthlyAmount) + " / month");
        holder.tvModeBadge.setText("auction".equalsIgnoreCase(group.mode) ? "SEALED AUCTION" : "LUCKY DRAW");
        holder.progressCollection.setProgress(progress);
        holder.tvProgressPercent.setText(progress + "%");
        holder.tvCollectionStatus.setText(group.paidThisMonth + " of " + group.memberCount + " contributions recorded this month");
        holder.itemView.setOnClickListener(v -> listener.onGroupClick(group));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupName, tvMemberCount, tvCollectionStatus, tvModeBadge, tvMonthlyAmount, tvProgressPercent;
        ProgressBar progressCollection;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            tvCollectionStatus = itemView.findViewById(R.id.tvCollectionStatus);
            tvModeBadge = itemView.findViewById(R.id.tvModeBadge);
            tvMonthlyAmount = itemView.findViewById(R.id.tvMonthlyAmount);
            tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
            progressCollection = itemView.findViewById(R.id.progressCollection);
        }
    }
}
