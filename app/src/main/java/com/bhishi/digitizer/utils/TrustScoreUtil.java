package com.bhishi.digitizer.utils;

/**
 * Section 6.3 of the brief: "Group Trustworthiness Score" -- an on-time
 * payment percentage across all cycles a member has taken part in.
 */
public class TrustScoreUtil {

    public static int calculate(int totalPaymentsExpected, int paymentsOnTime) {
        if (totalPaymentsExpected <= 0) return 100;
        return Math.round((paymentsOnTime / (float) totalPaymentsExpected) * 100);
    }

    public static String badgeForScore(int score) {
        if (score >= 90) return "Gold saver";
        if (score >= 75) return "Silver saver";
        if (score >= 50) return "Bronze saver";
        return "Building trust";
    }
}
