package com.bhishi.digitizer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.R;
import com.bhishi.digitizer.models.Group;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemberDueAdapter extends RecyclerView.Adapter<MemberDueAdapter.DueViewHolder> {

    public interface Listener {
        void onCardClick(Group group);
        void onPayNowClick(Group group);
    }

    private final List<Group> groups = new ArrayList<>();
    private final Listener listener;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public MemberDueAdapter(Listener listener) {
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
    public DueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DueViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_due_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DueViewHolder holder, int position) {
        Group group = groups.get(position);
        holder.tvGroupName.setText(group.groupName);

        // Member dashboard state: 0 = due, 1 = locked/confirmed, 2 = submitted and awaiting admin.
        if (group.paidThisMonth == 1) {
            holder.tvDueStatus.setText("Contribution verified for this month");
            holder.tvDueStatus.setTextColor(holder.itemView.getContext().getColor(R.color.green));
            holder.btnAction.setText("Paid ✓");
            holder.btnAction.setEnabled(false);
            holder.btnAction.setAlpha(0.75f);
        } else if (group.paidThisMonth == 2) {
            holder.tvDueStatus.setText("Payment submitted • awaiting admin verification");
            holder.tvDueStatus.setTextColor(holder.itemView.getContext().getColor(R.color.gold_dark));
            holder.btnAction.setText("Pending");
            holder.btnAction.setEnabled(false);
            holder.btnAction.setAlpha(0.75f);
        } else {
            holder.tvDueStatus.setText(currency.format(group.monthlyAmount) + " due this month");
            holder.tvDueStatus.setTextColor(holder.itemView.getContext().getColor(R.color.red));
            holder.btnAction.setText("Pay now");
            holder.btnAction.setEnabled(true);
            holder.btnAction.setAlpha(1f);
        }

        holder.itemView.setOnClickListener(v -> listener.onCardClick(group));
        holder.btnAction.setOnClickListener(v -> {
            if (holder.btnAction.isEnabled()) listener.onPayNowClick(group);
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class DueViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupName, tvDueStatus, btnAction;

        DueViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvDueStatus = itemView.findViewById(R.id.tvDueStatus);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
