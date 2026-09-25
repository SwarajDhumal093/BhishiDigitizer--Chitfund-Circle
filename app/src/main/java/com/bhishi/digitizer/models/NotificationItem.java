package com.bhishi.digitizer.models;

/** Maps to /notifications/{uid}/{notifId} */
public class NotificationItem {

    public String notifId;
    public String text;
    public String groupId;
    public boolean seen;
    public long timestamp;

    public NotificationItem() {
    }

    public NotificationItem(String text, String groupId) {
        this.text = text;
        this.groupId = groupId;
        this.seen = false;
        this.timestamp = System.currentTimeMillis();
    }
}
