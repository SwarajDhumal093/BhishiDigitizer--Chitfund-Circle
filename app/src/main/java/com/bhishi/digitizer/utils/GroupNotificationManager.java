package com.bhishi.digitizer.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashSet;
import java.util.Set;

/** Keeps FCM group-topic subscriptions aligned with /users/{uid}/groupsJoined. */
public final class GroupNotificationManager {
    private static final String PREFS = "bhishi_notification_topics";
    private static final String KEY_TOPICS = "topics";
    private static final String KEY_LAST_SYNC = "last_sync";
    private static final long SYNC_INTERVAL_MS = 15L * 60L * 1000L;

    private GroupNotificationManager() { }

    public static String topicForGroup(String groupId) {
        String safe = groupId == null ? "unknown" : groupId.replaceAll("[^A-Za-z0-9\\-_.~%]", "_");
        return "bhishi_group_" + safe;
    }

    public static void subscribeToGroup(Context context, String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) return;
        String topic = topicForGroup(groupId);
        FirebaseMessaging.getInstance().subscribeToTopic(topic);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> stored = new HashSet<>(prefs.getStringSet(KEY_TOPICS, new HashSet<>()));
        stored.add(topic);
        prefs.edit().putStringSet(KEY_TOPICS, stored).apply();
    }

    public static void syncSubscriptions(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long last = prefs.getLong(KEY_LAST_SYNC, 0L);
        if (System.currentTimeMillis() - last < SYNC_INTERVAL_MS) return;
        prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply();

        FirebasePaths.user(user.getUid()).child("groupsJoined")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Set<String> desired = new HashSet<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            if (Boolean.FALSE.equals(child.getValue(Boolean.class))) continue;
                            desired.add(topicForGroup(child.getKey()));
                        }
                        Set<String> previous = new HashSet<>(prefs.getStringSet(KEY_TOPICS, new HashSet<>()));
                        for (String topic : desired) {
                            if (!previous.contains(topic)) FirebaseMessaging.getInstance().subscribeToTopic(topic);
                        }
                        for (String topic : previous) {
                            if (!desired.contains(topic)) FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
                        }
                        prefs.edit().putStringSet(KEY_TOPICS, desired).apply();
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });
    }
}
