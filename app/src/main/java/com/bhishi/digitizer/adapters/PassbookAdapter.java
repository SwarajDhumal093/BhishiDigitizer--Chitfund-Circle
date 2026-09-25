package com.bhishi.digitizer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.R;
import com.bhishi.digitizer.models.Contribution;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PassbookAdapter extends RecyclerView.Adapter<PassbookAdapter.RowViewHolder> {

    private final List<Contribution> rows = new ArrayList<>();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public PassbookAdapter() {
        currency.setMaximumFractionDigits(0);
    }

    public void setRows(List<Contribution> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_passbook_row, parent, false);
        return new RowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        Contribution c = rows.get(position);
        holder.tvMonth.setText(c.memberName); // display label is injected by PassbookActivity
        holder.tvAmount.setText(currency.format(c.amount));

        if (c.isLocked()) {
            holder.tvStatus.setText(c.onTime ? "Verified • on time" : "Verified • paid late");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(c.onTime ? R.color.green : R.color.gold_dark));
        } else if (c.confirmedByMember) {
            holder.tvStatus.setText("Submitted • awaiting admin verification");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.gold_dark));
        } else {
            holder.tvStatus.setText("Not paid");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.red));
        }
    }

    @Override
    public int getItemCount() { return rows.size(); }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMonth, tvStatus, tvAmount;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
