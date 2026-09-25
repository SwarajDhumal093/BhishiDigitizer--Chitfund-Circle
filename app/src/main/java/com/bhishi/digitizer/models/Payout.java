package com.bhishi.digitizer.models;

/**
 * Maps to /payouts/{groupId}/{monthKey}.
 * Results are immutable once lockedResult=true.
 * auditSeed/auditHash make the selection/tie-break reproducible for the group.
 */
public class Payout {

    public String monthKey;
    public String winnerUid;
    public String winnerName;
    public String mode;
    public double bidAmount;
    public double dividendPerMember;
    public double poolAmount;
    public int eligibleCount;
    public long auditSeed;      // legacy/client seed kept for backwards compatibility
    public String auditNonce;   // server-generated cryptographic nonce for new rounds
    public String auditHash;
    public boolean lockedResult;
    public long timestamp;

    public Payout() { }

    public static Payout forDraw(String winnerUid, String winnerName, double poolAmount,
                                 int eligibleCount, long auditSeed, String auditHash) {
        Payout p = new Payout();
        p.winnerUid = winnerUid;
        p.winnerName = winnerName;
        p.mode = "draw";
        p.poolAmount = poolAmount;
        p.eligibleCount = eligibleCount;
        p.auditSeed = auditSeed;
        p.auditHash = auditHash;
        p.lockedResult = true;
        p.timestamp = System.currentTimeMillis();
        return p;
    }

    public static Payout forAuction(String winnerUid, String winnerName, double bidAmount,
                                    double dividendPerMember, double poolAmount, int eligibleCount,
                                    long auditSeed, String auditHash) {
        Payout p = new Payout();
        p.winnerUid = winnerUid;
        p.winnerName = winnerName;
        p.mode = "auction";
        p.bidAmount = bidAmount;
        p.dividendPerMember = dividendPerMember;
        p.poolAmount = poolAmount;
        p.eligibleCount = eligibleCount;
        p.auditSeed = auditSeed;
        p.auditHash = auditHash;
        p.lockedResult = true;
        p.timestamp = System.currentTimeMillis();
        return p;
    }
}
