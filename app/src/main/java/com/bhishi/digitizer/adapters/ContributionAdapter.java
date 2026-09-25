package com.bhishi.digitizer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.R;
import com.bhishi.digitizer.models.Contribution;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContributionAdapter extends RecyclerView.Adapter<ContributionAdapter.ContribViewHolder> {

    public interface OnPaidToggleListener {
        void onAdminConfirm(Contribution contribution, boolean paid);
    }

    private final List<Contribution> contributions = new ArrayList<>();
    private final OnPaidToggleListener listener;
    private final boolean canAdminConfirm;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public ContributionAdapter(boolean canAdminConfirm, OnPaidToggleListener listener) {
        this.canAdminConfirm = canAdminConfirm;
        this.listener = listener;
        currency.setMaximumFractionDigits(0);
    }

    public void setContributions(List<Contribution> newList) {
        contributions.clear();
        contributions.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContribViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contribution_row, parent, false);
        return new ContribViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContribViewHolder holder, int position) {
        Contribution c = contributions.get(position);
        holder.tvMemberName.setText(c.memberName);
        holder.switchPaid.setOnCheckedChangeListener(null);
        holder.switchPaid.setChecked(c.confirmedByAdmin);
        if (c.isLocked()) {
            holder.tvLockIcon.setText("PAID & LOCKED");
            holder.tvStatusDetail.setText("Member payment submitted and admin verified. Locked means the payment is completed and frozen in the ledger.");
        } else if (c.confirmedByMember) {
            holder.tvLockIcon.setText("AWAITING ADMIN");
            holder.tvStatusDetail.setText("The member has submitted payment. Admin verification is still pending.");
        } else {
            holder.tvLockIcon.setText("NOT PAID");
            holder.tvStatusDetail.setText(currency.format(c.amount) + " is due for this member in the current month.");
        }

        // Members can inspect status but only admins can change the admin-confirmation flag.
        holder.switchPaid.setEnabled(canAdminConfirm && !c.isLocked());
        holder.switchPaid.setVisibility(canAdminConfirm ? View.VISIBLE : View.GONE);

        holder.switchPaid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (canAdminConfirm && !c.isLocked() && listener != null) {
                listener.onAdminConfirm(c, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() { return contributions.size(); }

    static class ContribViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberName, tvLockIcon, tvStatusDetail;
        Switch switchPaid;

        ContribViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvLockIcon = itemView.findViewById(R.id.tvLockIcon);
            tvStatusDetail = itemView.findViewById(R.id.tvStatusDetail);
            switchPaid = itemView.findViewById(R.id.switchPaid);
        }
    }
}
