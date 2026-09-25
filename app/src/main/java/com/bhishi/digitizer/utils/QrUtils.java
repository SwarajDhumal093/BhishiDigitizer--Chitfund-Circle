package com.bhishi.digitizer.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/** Small helper for generating/parsing Bhishi invite QR payloads. */
public final class QrUtils {
    private static final String PREFIX = "BHISHI_DIGITIZER|";

    private QrUtils() { }

    public static String invitePayload(String groupId, String groupCode) {
        return PREFIX + safe(groupId) + "|" + safe(groupCode);
    }

    public static InvitePayload parseInvite(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (!value.startsWith(PREFIX)) return null;
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3) return null;
        if (parts[1].trim().isEmpty() || !parts[2].matches("\\d{6}")) return null;
        return new InvitePayload(parts[1], parts[2]);
    }

    public static Bitmap generate(String value, int size) throws WriterException {
        BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? Color.rgb(23, 49, 63) : Color.WHITE);
            }
        }
        return bitmap;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace("|", "");
    }

    public static final class InvitePayload {
        public final String groupId;
        public final String groupCode;

        public InvitePayload(String groupId, String groupCode) {
            this.groupId = groupId;
            this.groupCode = groupCode;
        }
    }
}
