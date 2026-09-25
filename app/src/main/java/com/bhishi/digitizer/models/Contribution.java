package com.bhishi.digitizer.models;

/**
 * Maps to /contributions/{groupId}/{monthKey}/{uid}
 * Dual-verification (confirmedByAdmin + confirmedByMember) makes the
 * record immutable once both flags are true -- covers Section 6.2
 * (Immutable Transaction Ledger with Dual Verification) of the brief.
 */
public class Contribution {

    public String uid;
    public String memberName;
    public double amount;
    public boolean paid;
    public boolean confirmedByAdmin;
    public boolean confirmedByMember;
    public boolean onTime;
    public long timestamp;
    public boolean paymentVerified;
    public String paymentMethod;
    public String paymentStatus;
    public String paymentId;

    public Contribution() {
    }

    public Contribution(String uid, String memberName, double amount) {
        this.uid = uid;
        this.memberName = memberName;
        this.amount = amount;
        this.paid = false;
        this.confirmedByAdmin = false;
        this.confirmedByMember = false;
        this.onTime = true;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isLocked() {
        return confirmedByAdmin && confirmedByMember;
    }
}
