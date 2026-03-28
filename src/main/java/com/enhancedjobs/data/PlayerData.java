package com.enhancedjobs.data;

import com.enhancedjobs.quests.Quest;
import com.enhancedjobs.utils.GoldManager;

import java.util.*;

public class PlayerData {

    private final UUID uuid;

    private final Set<String>              activeJobs      = new HashSet<>();
    private final Map<String, Integer>     jobLevels       = new HashMap<>();
    private final Map<String, Double>      jobXP           = new HashMap<>();
    private final Map<String, List<Quest>> activeQuests    = new HashMap<>();
    private final Map<String, Integer>     dailyQuestCount = new HashMap<>();
    private final Map<String, Long>        lastQuestReset  = new HashMap<>();
    private final Map<String, Set<String>> claimedRewards  = new HashMap<>();

    // Internal gold — used only when GoldEconomy is NOT present
    private double internalGold = 0.0;

    public PlayerData(UUID uuid) { this.uuid = uuid; }

    public UUID getUuid() { return uuid; }

    // ── Jobs ──────────────────────────────────────────────────────────────────

    public boolean hasJob(String job)  { return activeJobs.contains(job.toUpperCase()); }
    public Set<String> getActiveJobs() { return activeJobs; }

    public void joinJob(String job) {
        String key = job.toUpperCase();
        activeJobs.add(key);
        jobLevels.putIfAbsent(key, 1);
        jobXP.putIfAbsent(key, 0.0);
    }

    public void leaveJob(String job) { activeJobs.remove(job.toUpperCase()); }

    // ── Level & XP ────────────────────────────────────────────────────────────

    public int getJobLevel(String job) {
        return jobLevels.getOrDefault(job.toUpperCase(), 1);
    }

    public void setJobLevel(String job, int level) {
        jobLevels.put(job.toUpperCase(), Math.min(10, Math.max(1, level)));
    }

    public double getJobXP(String job) {
        return jobXP.getOrDefault(job.toUpperCase(), 0.0);
    }

    public void setJobXP(String job, double xp) {
        jobXP.put(job.toUpperCase(), Math.max(0, xp));
    }

    public void addJobXP(String job, double amount) {
        String key = job.toUpperCase();
        jobXP.put(key, jobXP.getOrDefault(key, 0.0) + amount);
    }

    // ── Quests ────────────────────────────────────────────────────────────────

    public List<Quest> getActiveQuests(String job) {
        return activeQuests.computeIfAbsent(job.toUpperCase(), k -> new ArrayList<>());
    }

    public void setActiveQuests(String job, List<Quest> quests) {
        activeQuests.put(job.toUpperCase(), quests);
    }

    public int getDailyQuestCount(String job) {
        return dailyQuestCount.getOrDefault(job.toUpperCase(), 0);
    }

    public void setDailyQuestCount(String job, int count) {
        dailyQuestCount.put(job.toUpperCase(), count);
    }

    public void incrementDailyQuestCount(String job) {
        String key = job.toUpperCase();
        dailyQuestCount.put(key, dailyQuestCount.getOrDefault(key, 0) + 1);
    }

    public long getLastQuestReset(String job) {
        return lastQuestReset.getOrDefault(job.toUpperCase(), 0L);
    }

    public void setLastQuestReset(String job, long time) {
        lastQuestReset.put(job.toUpperCase(), time);
    }

    public void resetDailyQuests(String job) {
        String key = job.toUpperCase();
        dailyQuestCount.put(key, 0);
        lastQuestReset.put(key, System.currentTimeMillis());
    }

    // ── Rewards ───────────────────────────────────────────────────────────────

    public boolean hasClaimedReward(String job, String rewardId) {
        return claimedRewards.computeIfAbsent(job.toUpperCase(), k -> new HashSet<>())
                             .contains(rewardId.toUpperCase());
    }

    public void claimReward(String job, String rewardId) {
        claimedRewards.computeIfAbsent(job.toUpperCase(), k -> new HashSet<>())
                      .add(rewardId.toUpperCase());
    }

    // ── Gold — always routes through GoldManager when available ───────────────

    /**
     * Use this everywhere in the plugin to get a player's gold.
     * Automatically delegates to GoldEconomy if it is loaded.
     */
    public double getGold() {
        GoldManager gm = GoldManager.getInstance();
        if (gm != null) return gm.getBalance(uuid);
        return internalGold;
    }

    public void addGold(double amount) {
        GoldManager gm = GoldManager.getInstance();
        if (gm != null) { gm.addGold(uuid, amount); return; }
        internalGold += amount;
    }

    public boolean removeGold(double amount) {
        GoldManager gm = GoldManager.getInstance();
        if (gm != null) return gm.removeGold(uuid, amount);
        if (internalGold < amount) return false;
        internalGold -= amount;
        return true;
    }

    public void setGold(double amount) {
        GoldManager gm = GoldManager.getInstance();
        if (gm != null) { gm.setGold(uuid, amount); return; }
        internalGold = Math.max(0, amount);
    }

    // ── Internal gold (used only for save/load when GoldEconomy is absent) ────

    public double getInternalGold()            { return internalGold; }
    public void setInternalGold(double amount) { this.internalGold = Math.max(0, amount); }
    public void addInternalGold(double amount) { this.internalGold += amount; }

    public boolean removeInternalGold(double amount) {
        if (internalGold < amount) return false;
        internalGold -= amount;
        return true;
    }

    // ── Raw map access (save/load) ────────────────────────────────────────────

    public Map<String, Integer>     getJobLevelsMap()      { return jobLevels; }
    public Map<String, Double>      getJobXPMap()          { return jobXP; }
    public Map<String, List<Quest>> getActiveQuestsMap()   { return activeQuests; }
    public Map<String, Integer>     getDailyQuestCounts()  { return dailyQuestCount; }
    public Map<String, Long>        getLastQuestResets()   { return lastQuestReset; }
    public Map<String, Set<String>> getClaimedRewardsMap() { return claimedRewards; }
}
