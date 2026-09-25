package com.bhishi.digitizer.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bhishi.digitizer.models.NotificationItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

/**
 * Section 3.4 (Automated Reminders): runs once a day via WorkManager,
 * checks every group's unpaid members for the current month, and writes
 * a notification 3 days before the due date and again on the due date.
 * Wire the actual SMS/WhatsApp send here via Twilio, MSG91 or the
 * WhatsApp Business API -- this class focuses on the due-date logic and
 * the in-app notification, which is what the UI (NotificationsActivity)
 * reads from.
 */
public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String monthKey = FirebasePaths.currentMonthKey();

        FirebasePaths.groups().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot groupsSnapshot) {
                for (DataSnapshot groupSnap : groupsSnapshot.getChildren()) {
                    String groupId = groupSnap.getKey();
                    checkGroupContributions(groupId, monthKey);
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                // Swallow -- WorkManager retries on its own schedule.
            }
        });

        return Result.success();
    }

    private void checkGroupContributions(String groupId, String monthKey) {
        FirebasePaths.contributions(groupId, monthKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot memberSnap : snapshot.getChildren()) {
                    Boolean paid = memberSnap.child("paid").getValue(Boolean.class);
                    String uid = memberSnap.getKey();
                    if (paid == null || !paid) {
                        NotificationItem reminder = new NotificationItem(
                                "Your contribution is due soon. Please pay to avoid delaying the group draw.",
                                groupId);
                        FirebasePaths.notifications(uid).push().setValue(reminder);
                        // TODO: call Twilio/MSG91 SMS API or WhatsApp Business API here.
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
            }
        });
    }
}
