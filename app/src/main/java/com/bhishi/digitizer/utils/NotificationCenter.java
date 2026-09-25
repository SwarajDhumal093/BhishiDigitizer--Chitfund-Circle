package com.bhishi.digitizer.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bhishi.digitizer.NotificationsActivity;
import com.bhishi.digitizer.R;

public final class NotificationCenter {
    public static final String CHANNEL_ID = "bhishi_rounds";

    private NotificationCenter() { }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bhishi draw & auction updates",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Lucky draw and sealed auction start/end updates");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static void show(Context context, String title, String body) {
        ensureChannel(context);
        Intent intent = new Intent(context, NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) (System.currentTimeMillis() & 0x7fffffff),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title == null || title.trim().isEmpty() ? "Bhishi Digitizer" : title)
                .setContentText(body == null ? "" : body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body == null ? "" : body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context)
                    .notify((int) (System.currentTimeMillis() & 0x7fffffff), builder.build());
        } catch (SecurityException ignored) {
            // Android 13+ permission may have been declined; the in-app notification still exists.
        }
    }
}
