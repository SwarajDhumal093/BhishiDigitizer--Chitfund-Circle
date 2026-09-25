package com.bhishi.digitizer.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.widget.Toast;

import com.bhishi.digitizer.models.Contribution;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Section 3.5 / 3.8: "Downloadable PDF statement for each member" and
 * "Digital Member Passbook ... downloaded as a PDF like a traditional
 * bank passbook." Uses Android's built-in PdfDocument API (PDFKit's
 * equivalent on Android) -- no third-party PDF library required.
 */
public class PdfPassbookGenerator {

    public static void generate(Context context, String memberName, String groupName,
                                 List<Contribution> history) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint title = new Paint();
        title.setTextSize(20);
        title.setColor(Color.BLACK);
        title.setFakeBoldText(true);

        Paint body = new Paint();
        body.setTextSize(13);
        body.setColor(Color.DKGRAY);

        canvas.drawText("Bhishi Digitizer - Member Passbook", 40, 50, title);
        canvas.drawText("Group: " + groupName, 40, 80, body);
        canvas.drawText("Member: " + memberName, 40, 100, body);

        int y = 140;
        canvas.drawText("Month", 40, y, body);
        canvas.drawText("Amount", 220, y, body);
        canvas.drawText("Status", 380, y, body);
        y += 20;

        for (Contribution c : history) {
            canvas.drawText(String.valueOf(c.timestamp), 40, y, body);
            canvas.drawText("Rs. " + c.amount, 220, y, body);
            canvas.drawText(c.paid ? (c.onTime ? "On time" : "Paid late") : "Unpaid", 380, y, body);
            y += 20;
        }

        document.finishPage(page);

        File dir = context.getExternalFilesDir(null);
        File file = new File(dir, "passbook_" + memberName.replace(" ", "_") + ".pdf");
        try (FileOutputStream out = new FileOutputStream(file)) {
            document.writeTo(out);
            Toast.makeText(context, "Passbook saved: " + file.getName(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(context, "Could not save PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            document.close();
        }
    }
}
