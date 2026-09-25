package com.bhishi.digitizer.models;

import java.util.HashMap;
import java.util.Map;

/** Maps to /users/{uid} */
public class AppUser {

    public String uid;
    public String name;
    public String phone;
    public String role;            // "admin" or "member"
    public int trustScore;
    public Map<String, Boolean> groupsJoined;

    public AppUser() {
    }

    public AppUser(String uid, String name, String phone, String role) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.trustScore = 100;
        this.groupsJoined = new HashMap<>();
    }
}
