package com.bhishi.digitizer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.R;
import com.bhishi.digitizer.models.Dispute;

import java.util.ArrayList;
import java.util.List;

public class DisputeAdapter extends RecyclerView.Adapter<DisputeAdapter.DisputeViewHolder> {

    public interface OnDisputeClickListener {
        void onDisputeClick(Dispute dispute);
    }

    private final List<Dispute> disputes = new ArrayList<>();
    private final OnDisputeClickListener listener;

    public DisputeAdapter(OnDisputeClickListener listener) {
        this.listener = listener;
    }

    public void setDisputes(List<Dispute> newDisputes) {
        disputes.clear();
        disputes.addAll(newDisputes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DisputeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dispute_row, parent, false);
        return new DisputeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DisputeViewHolder holder, int position) {
        Dispute d = disputes.get(position);
        holder.tvIssue.setText(d.issue);
        boolean resolved = "resolved".equals(d.status);
        holder.tvStatus.setText(resolved ? "Resolved" : "Open");
        holder.tvStatus.setBackgroundResource(resolved ? R.drawable.bg_pill_green : R.drawable.bg_pill_gold);
        holder.tvStatus.setTextColor(resolved ? 0xFF0F6E56 : 0xFFA9781F);
        holder.itemView.setOnClickListener(v -> listener.onDisputeClick(d));
    }

    @Override
    public int getItemCount() {
        return disputes.size();
    }

    static class DisputeViewHolder extends RecyclerView.ViewHolder {
        TextView tvIssue, tvStatus;

        DisputeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIssue = itemView.findViewById(R.id.tvIssue);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
