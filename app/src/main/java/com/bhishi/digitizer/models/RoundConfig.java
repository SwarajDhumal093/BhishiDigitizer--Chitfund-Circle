package com.bhishi.digitizer.models;

/** Controls the visible sealed-auction window for a group/month. */
public class RoundConfig {
    public String status; // open / closed
    public long openAt;
    public long closeAt;
    public int durationMinutes;

    public RoundConfig() { }

    public RoundConfig(String status, long openAt, long closeAt, int durationMinutes) {
        this.status = status;
        this.openAt = openAt;
        this.closeAt = closeAt;
        this.durationMinutes = durationMinutes;
    }

    public boolean isOpen(long now) {
        return "open".equalsIgnoreCase(status) && closeAt > now;
    }
}
