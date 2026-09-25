package com.bhishi.digitizer.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebasePaths {

    private static final FirebaseDatabase DB = FirebaseDatabase.getInstance();

    public static DatabaseReference root() { return DB.getReference(); }
    public static DatabaseReference users() { return root().child("users"); }
    public static DatabaseReference user(String uid) { return users().child(uid); }
    public static DatabaseReference groups() { return root().child("groups"); }
    public static DatabaseReference group(String groupId) { return groups().child(groupId); }
    public static DatabaseReference groupMembers(String groupId) { return group(groupId).child("members"); }
    public static DatabaseReference contributionsGroup(String groupId) { return root().child("contributions").child(groupId); }
    public static DatabaseReference contributions(String groupId, String monthKey) { return contributionsGroup(groupId).child(monthKey); }
    public static DatabaseReference payoutsGroup(String groupId) { return root().child("payouts").child(groupId); }
    public static DatabaseReference payout(String groupId, String monthKey) { return payoutsGroup(groupId).child(monthKey); }
    public static DatabaseReference disputes(String groupId) { return root().child("disputes").child(groupId); }
    public static DatabaseReference notifications(String uid) { return root().child("notifications").child(uid); }
    public static DatabaseReference auctionBids(String groupId, String monthKey) { return root().child("auctionBids").child(groupId).child(monthKey); }
    public static DatabaseReference roundSeed(String groupId, String monthKey) { return root().child("roundSeeds").child(groupId).child(monthKey); }
    public static DatabaseReference roundConfig(String groupId, String monthKey) { return root().child("roundConfigs").child(groupId).child(monthKey); }
    public static DatabaseReference payments(String groupId, String monthKey) { return root().child("payments").child(groupId).child(monthKey); }

    public static String currentMonthKey() {
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US);
        return fmt.format(new java.util.Date());
    }
}
