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
import com.bhishi.digitizer.models.Payout;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Creates a shareable branded certificate for a locked payout result. */
public final class PayoutCertificatePdfGenerator {
    private PayoutCertificatePdfGenerator() { }

    public static File generate(Context context, Payout payout) throws Exception {
        if (payout == null || !payout.lockedResult) {
            throw new IllegalStateException("Only locked payout results can be certified");
        }
        File directory = new File(context.getExternalFilesDir(null), "payout-certificates");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create certificate folder");
        }
        String cycle = safe(payout.monthKey).replaceAll("[^A-Za-z0-9_-]", "_");
        File file = new File(directory, "Bhishi_Payout_" + cycle + ".pdf");

        PdfDocument document = new PdfDocument();
        PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(842, 595, 1).create());
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // warm premium background
        paint.setColor(Color.rgb(255, 249, 242));
        canvas.drawRect(0, 0, 842, 595, paint);
        // navy frame
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.rgb(14, 42, 57));
        canvas.drawRoundRect(28, 28, 814, 567, 24, 24, paint);
        paint.setStyle(Paint.Style.FILL);

        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.bhishi_logo_mark);
        if (logo != null) {
            android.graphics.Rect src = new android.graphics.Rect(0, 0, logo.getWidth(), logo.getHeight());
            android.graphics.RectF dst = new android.graphics.RectF(58, 55, 180, 151);
            canvas.drawBitmap(logo, src, dst, paint);
        }

        paint.setColor(Color.rgb(14, 42, 57));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(30f);
        canvas.drawText("Bhishi Digitizer", 205, 84, paint);
        paint.setTextSize(15f);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("Transparent payout certificate", 205, 111, paint);

        paint.setColor(Color.rgb(255, 122, 31));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(20f);
        canvas.drawText("LOCKED & AUDITABLE RESULT", 58, 192, paint);

        paint.setColor(Color.rgb(25, 52, 67));
        paint.setTextSize(21f);
        canvas.drawText("Winner", 58, 244, paint);
        paint.setTextSize(36f);
        canvas.drawText(safe(payout.winnerName), 58, 286, paint);

        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currency.setMaximumFractionDigits(0);
        int x1 = 58, x2 = 315, x3 = 570, y = 354;
        field(canvas, paint, x1, y, "CYCLE", safe(payout.monthKey));
        field(canvas, paint, x2, y, "MODE", "auction".equalsIgnoreCase(payout.mode) ? "Sealed auction" : "Lucky draw");
        field(canvas, paint, x3, y, "POOL", currency.format(payout.poolAmount));
        field(canvas, paint, x1, y + 78, "ELIGIBLE", String.valueOf(payout.eligibleCount));
        String amountLabel = "auction".equalsIgnoreCase(payout.mode) ? currency.format(payout.bidAmount) : currency.format(payout.poolAmount);
        field(canvas, paint, x2, y + 78, "PAYOUT", amountLabel);
        field(canvas, paint, x3, y + 78, "AUDIT CODE", empty(payout.auditHash) ? "Legacy" : payout.auditHash);

        String when = payout.timestamp > 0
                ? new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(payout.timestamp))
                : "Recorded result";
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(11f);
        paint.setColor(Color.rgb(91, 109, 118));
        canvas.drawText("Recorded " + when + " • Result is write-once after server finalisation.", 58, 529, paint);

        document.finishPage(page);
        try (FileOutputStream out = new FileOutputStream(file)) {
            document.writeTo(out);
        } finally {
            document.close();
        }
        return file;
    }

    private static void field(Canvas canvas, Paint paint, int x, int y, String label, String value) {
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(10.5f);
        paint.setColor(Color.rgb(91, 109, 118));
        canvas.drawText(label, x, y, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(16f);
        paint.setColor(Color.rgb(25, 52, 67));
        canvas.drawText(value, x, y + 24, paint);
    }

    private static boolean empty(String s) { return s == null || s.trim().isEmpty(); }
    private static String safe(String s) { return empty(s) ? "—" : s; }
}
