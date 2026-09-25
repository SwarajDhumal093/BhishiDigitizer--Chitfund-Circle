package com.bhishi.digitizer;

import androidx.annotation.NonNull;

import com.bhishi.digitizer.utils.NotificationCenter;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class BhishiMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        String title = null;
        String body = null;
        if (message.getNotification() != null) {
            title = message.getNotification().getTitle();
            body = message.getNotification().getBody();
        }
        if ((title == null || title.isEmpty()) && message.getData().containsKey("title")) {
            title = message.getData().get("title");
        }
        if ((body == null || body.isEmpty()) && message.getData().containsKey("body")) {
            body = message.getData().get("body");
        }
        NotificationCenter.show(this, title, body);
    }
}
