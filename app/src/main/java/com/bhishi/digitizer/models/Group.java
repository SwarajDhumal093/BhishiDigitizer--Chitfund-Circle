package com.bhishi.digitizer.models;

/**
 * Maps to /groups/{groupId} in Firebase Realtime Database.
 * Covers Section 3.1 (Group & Member Management) of the project brief.
 */
public class Group {

    public String groupId;
    public String groupName;
    public String adminId;
    public String adminName;
    public double monthlyAmount;
    public int durationMonths;
    public String startDate;
    public String mode;          // "draw" or "auction"
    public String groupCode;     // 6-digit shareable join code
    public int memberCount;
    public int paidThisMonth;
    public boolean cycleCompleted;
    public int dueDay;
    public int graceDays;
    public double lateFeeAmount;
    public boolean allowOfflinePayments;
    public boolean excludePreviousWinners;
    public boolean closureRequested;
    public long archivedAt;

    public Group() {
        // Required empty constructor for Firebase deserialization
    }

    public Group(String groupName, String adminId, String adminName, double monthlyAmount,
                 int durationMonths, String startDate, String mode, String groupCode) {
        this.groupName = groupName;
        this.adminId = adminId;
        this.adminName = adminName;
        this.monthlyAmount = monthlyAmount;
        this.durationMonths = durationMonths;
        this.startDate = startDate;
        this.mode = mode;
        this.groupCode = groupCode;
        this.memberCount = 0;
        this.paidThisMonth = 0;
        this.cycleCompleted = false;
        this.dueDay = 5;
        this.graceDays = 3;
        this.lateFeeAmount = 0;
        this.allowOfflinePayments = true;
        this.excludePreviousWinners = true;
        this.closureRequested = false;
        this.archivedAt = 0L;
    }

    public int progressPercent() {
        if (memberCount == 0) return 0;
        return (int) ((paidThisMonth / (float) memberCount) * 100);
    }
}
