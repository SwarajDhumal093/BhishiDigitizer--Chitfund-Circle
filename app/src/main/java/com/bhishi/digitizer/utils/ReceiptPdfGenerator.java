package com.bhishi.digitizer.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import com.bhishi.digitizer.R;
import com.bhishi.digitizer.models.Receipt;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ReceiptPdfGenerator {
    private ReceiptPdfGenerator() { }

    public static File generate(Context context, Receipt receipt) throws Exception {
        File directory = new File(context.getExternalFilesDir(null), "receipts");
        if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Could not create receipt folder");
        File file = new File(directory, "Bhishi_Receipt_" + sanitize(receipt.receiptId) + ".pdf");

        PdfDocument document = new PdfDocument();
        PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(595, 842, 1).create());
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(Color.rgb(255, 248, 240));
        canvas.drawRect(0, 0, 595, 842, paint);
        paint.setColor(Color.rgb(10, 29, 39));
        canvas.drawRoundRect(36, 36, 559, 190, 22, 22, paint);

        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.bhishi_logo_mark);
        if (logo != null) {
            android.graphics.Rect src = new android.graphics.Rect(0, 0, logo.getWidth(), logo.getHeight());
            android.graphics.RectF dst = new android.graphics.RectF(54, 54, 150, 130);
            canvas.drawBitmap(logo, src, dst, paint);
        }

        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(25);
        canvas.drawText("Bhishi Digitizer", 166, 84, paint);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("Digital contribution receipt", 166, 108, paint);

        paint.setColor(Color.rgb(255, 138, 31));
        paint.setTextSize(17);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(receipt.isVerified() ? "VERIFIED" : "PENDING VERIFICATION", 54, 166, paint);

        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currency.setMaximumFractionDigits(0);
        SimpleDateFormat date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        int y = 236;
        y = row(canvas, paint, "Receipt ID", safe(receipt.receiptId), y);
        y = row(canvas, paint, "Member", safe(receipt.memberName), y);
        y = row(canvas, paint, "Group", safe(receipt.groupName), y);
        y = row(canvas, paint, "Cycle", safe(receipt.monthKey), y);
        y = row(canvas, paint, "Amount", currency.format(receipt.amount), y);
        y = row(canvas, paint, "Payment method", prettyMethod(receipt.paymentMethod), y);
        y = row(canvas, paint, "Transaction reference", empty(receipt.paymentId) ? "Offline / not applicable" : receipt.paymentId, y);
        y = row(canvas, paint, "Date", receipt.timestamp > 0 ? date.format(new Date(receipt.timestamp)) : "—", y);
        y = row(canvas, paint, "Verification", receipt.isVerified() ? "Member + Admin verified" : "Awaiting dual verification", y);

        paint.setColor(Color.rgb(96, 114, 124));
        paint.setTextSize(10.5f);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("This receipt reflects the contribution record stored by Bhishi Digitizer.", 54, y + 34, paint);
        canvas.drawText("For gateway payments, the transaction reference is recorded after server verification.", 54, y + 52, paint);

        document.finishPage(page);
        try (FileOutputStream out = new FileOutputStream(file)) {
            document.writeTo(out);
        } finally {
            document.close();
        }
        return file;
    }

    private static int row(Canvas canvas, Paint paint, String label, String value, int y) {
        paint.setColor(Color.rgb(96, 114, 124));
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(11);
        canvas.drawText(label.toUpperCase(Locale.US), 54, y, paint);
        paint.setColor(Color.rgb(23, 49, 63));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(15);
        canvas.drawText(value, 54, y + 23, paint);
        paint.setColor(Color.rgb(232, 222, 210));
        canvas.drawRect(54, y + 36, 541, y + 37, paint);
        return y + 68;
    }

    private static String prettyMethod(String method) {
        if ("gateway".equalsIgnoreCase(method)) return "Online gateway";
        if ("offline".equalsIgnoreCase(method)) return "Cash / offline";
        return empty(method) ? "Not specified" : method;
    }

    private static boolean empty(String value) { return value == null || value.trim().isEmpty(); }
    private static String safe(String value) { return empty(value) ? "—" : value; }
    private static String sanitize(String value) { return safe(value).replaceAll("[^A-Za-z0-9_-]", "_"); }
}
