package com.bhishi.digitizer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.bhishi.digitizer.models.Receipt;
import com.bhishi.digitizer.utils.ReceiptPdfGenerator;

import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptDetailActivity extends BaseActivity {
    private Receipt receipt;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_detail);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        receipt = fromIntent();
        bind();
        findViewById(R.id.btnDownload).setOnClickListener(v -> createPdf(false));
        findViewById(R.id.btnShare).setOnClickListener(v -> createPdf(true));
    }

    private Receipt fromIntent() {
        Intent i = getIntent();
        Receipt r = new Receipt();
        r.receiptId = i.getStringExtra("receiptId");
        r.groupId = i.getStringExtra("groupId");
        r.groupName = i.getStringExtra("groupName");
        r.monthKey = i.getStringExtra("monthKey");
        r.memberName = i.getStringExtra("memberName");
        r.amount = i.getDoubleExtra("amount", 0);
        r.timestamp = i.getLongExtra("timestamp", 0);
        r.paymentMethod = i.getStringExtra("paymentMethod");
        r.paymentStatus = i.getStringExtra("paymentStatus");
        r.paymentId = i.getStringExtra("paymentId");
        r.confirmedByMember = i.getBooleanExtra("memberVerified", false);
        r.confirmedByAdmin = i.getBooleanExtra("adminVerified", false);
        return r;
    }

    private void bind() {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currency.setMaximumFractionDigits(0);
        ((TextView)findViewById(R.id.tvAmount)).setText(currency.format(receipt.amount));
        TextView status = findViewById(R.id.tvStatus);
        status.setText(receipt.isVerified() ? "✓ VERIFIED CONTRIBUTION" : "● PENDING DUAL VERIFICATION");
        status.setTextColor(getColor(receipt.isVerified() ? R.color.brand_gold : R.color.warning));
        ((TextView)findViewById(R.id.tvReceiptId)).setText("Receipt ID\n" + safe(receipt.receiptId));
        ((TextView)findViewById(R.id.tvGroup)).setText("Group\n" + safe(receipt.groupName));
        ((TextView)findViewById(R.id.tvMember)).setText("Member\n" + safe(receipt.memberName));
        ((TextView)findViewById(R.id.tvCycle)).setText("Cycle\n" + safe(receipt.monthKey));
        ((TextView)findViewById(R.id.tvMethod)).setText("Payment method\n" + method());
        ((TextView)findViewById(R.id.tvReference)).setText("Transaction reference\n" + (empty(receipt.paymentId) ? "Offline / not applicable" : receipt.paymentId));
        String date = receipt.timestamp > 0 ? new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(receipt.timestamp)) : "—";
        ((TextView)findViewById(R.id.tvDate)).setText("Recorded on\n" + date);
    }

    private void createPdf(boolean share) {
        try {
            File file = ReceiptPdfGenerator.generate(this, receipt);
            if (!share) {
                Toast.makeText(this, getString(R.string.receipt_generated) + "\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/pdf");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            send.putExtra(Intent.EXTRA_TEXT, "Bhishi Digitizer receipt " + safe(receipt.receiptId));
            startActivity(Intent.createChooser(send, getString(R.string.share_receipt)));
        } catch (Exception e) {
            Toast.makeText(this, "Could not create receipt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String method() {
        if ("gateway".equalsIgnoreCase(receipt.paymentMethod)) return "Online gateway";
        if ("offline".equalsIgnoreCase(receipt.paymentMethod)) return "Cash / offline";
        return "Contribution";
    }
    private boolean empty(String s) { return s == null || s.trim().isEmpty(); }
    private String safe(String s) { return empty(s) ? "—" : s; }
}
