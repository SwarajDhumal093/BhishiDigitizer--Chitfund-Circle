package com.bhishi.digitizer.models;

public class Receipt {
    public String receiptId;
    public String groupId;
    public String groupName;
    public String monthKey;
    public String memberName;
    public double amount;
    public long timestamp;
    public String paymentMethod;
    public String paymentStatus;
    public String paymentId;
    public boolean confirmedByMember;
    public boolean confirmedByAdmin;

    public Receipt() { }

    public boolean isVerified() {
        return confirmedByMember && confirmedByAdmin;
    }
}
