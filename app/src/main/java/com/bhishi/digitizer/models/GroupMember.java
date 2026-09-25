package com.bhishi.digitizer.models;

/** Maps to /groups/{groupId}/members/{uid} */
public class GroupMember {

    public String uid;
    public String name;
    public String phone;
    public String joinedOn;
    public boolean hasReceivedPayout;
    public int trustScore;

    public GroupMember() {
    }

    public GroupMember(String uid, String name, String phone, String joinedOn) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.joinedOn = joinedOn;
        this.hasReceivedPayout = false;
        this.trustScore = 100;
    }
}
