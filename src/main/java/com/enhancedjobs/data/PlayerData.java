package com.enhancedjobs.data;

import com.enhancedjobs.jobs.JobType;
import com.enhancedjobs.quests.ActiveQuest;
import com.enhancedjobs.utils.XPUtils;

import java.util.*;

/**
 * Holds all persisted data for a single player.
 * Serialization / deserialization is handled by {@link DataManager}.
 */
public class PlayerData {

    private final UUID playerUUID;

    // ── Job state ─────────────────────────────────────────────────────────────
    /** Active jobs the player currently holds (can hold multiple). */
    private final Set<String> activeJobs = new HashSet<>();

    /** Current level per job name. */
    private final Map<String, Integer> jobLevels = new HashMap<>();

    /** Current XP within the current level, per job name. */
    private final Map<String, Double> jobXP = new HashMap<>();

    /** Rewards already claimed (one-time items), e.g. "FARMER_LEVEL_6_HAT". */
    private final Set<String> claimedRewards = new HashSet<>();

    // ── Quest state ──────────────────────────────────────────────────────────
    /** Timestamp (ms) of the last 24h quest reset for this player. */
    private long lastQuestReset = 0L;

    /** How many free quest slots remain today across ALL jobs combined. */
    private int dailyQuestsRemaining = 3;

    /** All currently active quests, keyed by templateId + jobType combo. */
    private final List<ActiveQuest> activeQuests = new ArrayList<>();

    // ── AutoFarm state ───────────────────────────────────────────────────────
    /** Encoded farm locations: "world,x,y,z,chestX,chestY,chestZ,lastHarvest" */
    private final List<String> autoFarmData = new ArrayList<>();

    // ── MoneyTree state ──────────────────────────────────────────────────────
    /** Encoded money tree locations: "world,x,y,z,plantTime" */
    private final List<String> moneyTreeData = new ArrayList<>();

    /** Whether the player has already received their Level-10 money tree seeds reward. */
    private boolean moneyTreeSeedsClaimed = false;

    // ── Constructor ──────────────────────────────────────────────────────────

    public PlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.lastQuestReset = System.currentTimeMillis();
        this.dailyQuestsRemaining = 3;
    }

    // ── Job helpers ──────────────────────────────────────────────────────────

    public boolean hasJob(String jobName) {
        return activeJobs.contains(jobName.toUpperCase());
    }

    public void addJob(String jobName) {
        activeJobs.add(jobName.toUpperCase());
        jobLevels.putIfAbsent(jobName.toUpperCase(), 1);
        jobXP.putIfAbsent(jobName.toUpperCase(), 0.0);
    }

    public void removeJob(String jobName) {
        activeJobs.remove(jobName.toUpperCase());
    }

    public int getLevel(String jobName) {
        return jobLevels.getOrDefault(jobName.toUpperCase(), 1);
    }

    public void setLevel(String jobName, int level) {
        jobLevels.put(jobName.toUpperCase(), Math.min(level, XPUtils.MAX_LEVEL));
    }

    public double getXP(String jobName) {
        return jobXP.getOrDefault(jobName.toUpperCase(), 0.0);
    }

    public void setXP(String jobName, double xp) {
        jobXP.put(jobName.toUpperCase(), Math.max(0, xp));
    }

    /**
     * Adds XP to the given job, returns true if the player leveled up.
     * Caps at MAX_LEVEL and rolls over extra XP into the new level.
     */
    public boolean addXP(String jobName, double amount) {
        String key = jobName.toUpperCase();
        int currentLevel = getLevel(key);
        if (currentLevel >= XPUtils.MAX_LEVEL) return false;

        double currentXP = getXP(key) + amount;
        double needed    = XPUtils.getXPForLevel(currentLevel);

        if (currentXP >= needed) {
            // Level up – carry over excess XP into new level
            double overflow = currentXP - needed;
            int newLevel    = currentLevel + 1;
            setLevel(key, newLevel);
            setXP(key, overflow);
            return true;
        }
        setXP(key, currentXP);
        return false;
    }

    public boolean hasClaimedReward(String rewardKey) {
        return claimedRewards.contains(rewardKey);
    }

    public void claimReward(String rewardKey) {
        claimedRewards.add(rewardKey);
    }

    // ── Quest helpers ────────────────────────────────────────────────────────

    public boolean isDailyQuestResetDue() {
        return System.currentTimeMillis() - lastQuestReset >= 24L * 60 * 60 * 1000;
    }

    public long getMillisUntilReset() {
        long elapsed = System.currentTimeMillis() - lastQuestReset;
        long cycle   = 24L * 60 * 60 * 1000;
        return Math.max(0, cycle - elapsed);
    }

    public void performDailyReset(int newQuestCount) {
        lastQuestReset      = System.currentTimeMillis();
        dailyQuestsRemaining = newQuestCount;
        activeQuests.clear();
    }

    public boolean canTakeQuest() {
        return dailyQuestsRemaining > 0;
    }

    public void decrementQuestSlot() {
        if (dailyQuestsRemaining > 0) dailyQuestsRemaining--;
    }

    public List<ActiveQuest> getActiveQuestsForJob(String jobName) {
        List<ActiveQuest> result = new ArrayList<>();
        for (ActiveQuest q : activeQuests) {
            if (q.getJobType().equalsIgnoreCase(jobName)) result.add(q);
        }
        return result;
    }

    public void addActiveQuest(ActiveQuest quest) {
        activeQuests.add(quest);
    }

    // ── Raw accessors (for DataManager) ─────────────────────────────────────

    public UUID getPlayerUUID()                { return playerUUID;           }
    public Set<String> getActiveJobs()         { return activeJobs;           }
    public Map<String, Integer> getJobLevels() { return jobLevels;            }
    public Map<String, Double> getJobXP()      { return jobXP;                }
    public Set<String> getClaimedRewards()     { return claimedRewards;       }
    public long getLastQuestReset()            { return lastQuestReset;       }
    public void setLastQuestReset(long t)      { this.lastQuestReset = t;     }
    public int getDailyQuestsRemaining()       { return dailyQuestsRemaining; }
    public void setDailyQuestsRemaining(int n) { this.dailyQuestsRemaining = n;}
    public List<ActiveQuest> getActiveQuests() { return activeQuests;         }
    public List<String> getAutoFarmData()      { return autoFarmData;         }
    public List<String> getMoneyTreeData()     { return moneyTreeData;        }
    public boolean isMoneyTreeSeedsClaimed()   { return moneyTreeSeedsClaimed; }
    public void setMoneyTreeSeedsClaimed(boolean b){ this.moneyTreeSeedsClaimed = b; }
}
