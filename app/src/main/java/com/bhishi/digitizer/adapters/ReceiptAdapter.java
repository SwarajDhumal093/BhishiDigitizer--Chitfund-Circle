package com.bhishi.digitizer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bhishi.digitizer.R;
import com.bhishi.digitizer.models.Receipt;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.Holder> {
    public interface Listener { void onClick(Receipt receipt); }
    private final List<Receipt> items = new ArrayList<>();
    private final Listener listener;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public ReceiptAdapter(Listener listener) {
        this.listener = listener;
        currency.setMaximumFractionDigits(0);
    }

    public void setItems(List<Receipt> receipts) {
        items.clear();
        items.addAll(receipts);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Receipt r = items.get(position);
        h.group.setText(r.groupName == null ? "Bhishi" : r.groupName);
        h.meta.setText(r.monthKey + "  •  " + pretty(r.paymentMethod));
        h.status.setText(r.isVerified() ? "✓ VERIFIED" : "● PENDING VERIFICATION");
        h.status.setTextColor(h.itemView.getContext().getColor(r.isVerified() ? R.color.success : R.color.warning));
        h.amount.setText(currency.format(r.amount));
        h.itemView.setOnClickListener(v -> listener.onClick(r));
    }

    private String pretty(String method) {
        if ("gateway".equalsIgnoreCase(method)) return "Online payment";
        if ("offline".equalsIgnoreCase(method)) return "Offline payment";
        return "Contribution";
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView group, meta, status, amount;
        Holder(View item) {
            super(item);
            group = item.findViewById(R.id.tvReceiptGroup);
            meta = item.findViewById(R.id.tvReceiptMeta);
            status = item.findViewById(R.id.tvReceiptStatus);
            amount = item.findViewById(R.id.tvReceiptAmount);
        }
    }
}
