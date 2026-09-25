package com.bhishi.digitizer.models;

/**
 * One sealed bid for a monthly auction round.
 * Stored under /auctionBids/{groupId}/{monthKey}/{uid}.
 * Members can read only their own bid; the group admin can read all bids when closing the round.
 */
public class AuctionBid {
    public String uid;
    public String memberName;
    public double amount;
    public long timestamp;

    public AuctionBid() { }

    public AuctionBid(String uid, String memberName, double amount) {
        this.uid = uid;
        this.memberName = memberName;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }
}
