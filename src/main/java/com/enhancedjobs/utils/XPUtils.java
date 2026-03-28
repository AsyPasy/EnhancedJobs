package com.enhancedjobs.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class XPUtils {

    /** Total XP required to reach a given level (cumulative). */
    private static final Map<Integer, Double> XP_PER_LEVEL = new LinkedHashMap<>();

    static {
        XP_PER_LEVEL.put(1,  100.0);
        XP_PER_LEVEL.put(2,  200.0);
        XP_PER_LEVEL.put(3,  400.0);
        XP_PER_LEVEL.put(4,  1000.0);
        XP_PER_LEVEL.put(5,  2500.0);
        XP_PER_LEVEL.put(6,  5000.0);
        XP_PER_LEVEL.put(7,  8000.0);
        XP_PER_LEVEL.put(8,  12000.0);
        XP_PER_LEVEL.put(9,  15000.0);
        XP_PER_LEVEL.put(10, 20000.0);
    }

    public static final int MAX_LEVEL = 10;

    /**
     * Returns the XP required to complete the given level
     * (i.e., XP needed within that level to advance to the next).
     */
    public static double getXPForLevel(int level) {
        return XP_PER_LEVEL.getOrDefault(level, 99999.0);
    }

    /**
     * Checks whether a player's current XP has reached the threshold for the next level.
     * Returns the new level if leveled up, or the same level if not.
     */
    public static int calculateLevel(int currentLevel, double currentXP) {
        if (currentLevel >= MAX_LEVEL) return MAX_LEVEL;
        double required = getXPForLevel(currentLevel);
        if (currentXP >= required) {
            return currentLevel + 1;
        }
        return currentLevel;
    }

    /**
     * Builds a visual progress bar for XP display.
     * @param current  current XP within the level
     * @param max      XP needed for next level
     * @param length   total bar character length
     */
    public static String buildProgressBar(double current, double max, int length) {
        int filled = (max <= 0) ? length : (int) Math.min(length, (current / max) * length);
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < length; i++) {
            if (i == filled) sb.append("§7");
            sb.append("█");
        }
        return sb.toString();
    }

    /**
     * Formats a duration in milliseconds into a human-readable countdown (HH:MM:SS).
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) return "00:00:00";
        long seconds = millis / 1000;
        long hours   = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs    = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Formats a duration in milliseconds into a human-readable short string (e.g. "14h 32m").
     */
    public static String formatDurationShort(long millis) {
        if (millis <= 0) return "Ready";
        long seconds = millis / 1000;
        long hours   = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
