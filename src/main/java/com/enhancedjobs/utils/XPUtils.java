package com.enhancedjobs.utils;

public class XPUtils {

    private static final double[] XP_TABLE = {
        0,       // Level 1 (no XP needed to be level 1)
        100,     // Level 2
        200,     // Level 3
        400,     // Level 4
        1000,    // Level 5
        2500,    // Level 6
        5000,    // Level 7
        8000,    // Level 8
        12000,   // Level 9
        15000,   // Level 10
        20000    // Max
    };

    private XPUtils() {}

    /** Returns how much XP is needed to advance from the given level to the next. */
    public static double xpRequired(int currentLevel) {
        if (currentLevel >= 10) return Double.MAX_VALUE;
        return XP_TABLE[currentLevel];
    }

    /** Returns total XP required to reach a target level from level 1. */
    public static double totalXPForLevel(int targetLevel) {
        double total = 0;
        for (int i = 1; i < targetLevel && i < XP_TABLE.length; i++)
            total += XP_TABLE[i];
        return total;
    }

    /** Returns a percentage 0-100 representing progress to the next level. */
    public static int progressPercent(int level, double currentXP) {
        if (level >= 10) return 100;
        double required = xpRequired(level);
        return (int) Math.min(100, (currentXP / required) * 100);
    }

    /** Builds a visual XP progress bar. */
    public static String buildXPBar(int level, double currentXP) {
        int pct    = progressPercent(level, currentXP);
        int filled = pct / 5; // 20 segments
        return "§a" + "█".repeat(filled) + "§8" + "█".repeat(20 - filled)
             + " §e" + (int) currentXP + "§7/§e" + (int) xpRequired(level);
    }
}
