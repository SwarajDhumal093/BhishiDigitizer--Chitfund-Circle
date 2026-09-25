package com.bhishi.digitizer.models;

/** Maps to /disputes/{groupId}/{disputeId}. Locks permanently once resolved. */
public class Dispute {

    public String disputeId;
    public String raisedByUid;
    public String raisedByName;
    public String monthKey;
    public String issue;
    public String status;          // "open" or "resolved"
    public String resolutionNote;
    public boolean locked;
    public long timestamp;

    public Dispute() {
    }

    public Dispute(String raisedByUid, String raisedByName, String monthKey, String issue) {
        this.raisedByUid = raisedByUid;
        this.raisedByName = raisedByName;
        this.monthKey = monthKey;
        this.issue = issue;
        this.status = "open";
        this.locked = false;
        this.timestamp = System.currentTimeMillis();
    }

    public void resolve(String note) {
        this.resolutionNote = note;
        this.status = "resolved";
        this.locked = true;
    }
}
