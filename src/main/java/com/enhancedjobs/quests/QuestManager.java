package com.enhancedjobs.quests;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.DataManager;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.jobs.JobType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Central manager for quest assignment, progress tracking, daily resets,
 * and quest XP reward dispatch.
 */
public class QuestManager {

    private final EnhancedJobSystem plugin;
    private final DataManager dataManager;

    public QuestManager(EnhancedJobSystem plugin, DataManager dataManager) {
        this.plugin      = plugin;
        this.dataManager = dataManager;
    }

    // ── Daily reset ───────────────────────────────────────────────────────────

    /**
     * Checks and performs the 24-hour quest reset for a player if due.
     * Should be called whenever a player opens their quest GUI or logs in.
     */
    public void checkAndReset(Player player) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.isDailyQuestResetDue()) {
            performReset(player, data);
        }
    }

    private void performReset(Player player, PlayerData data) {
        int dailyCount = plugin.getConfig().getInt("daily-quests", 3);
        data.performDailyReset(dailyCount);

        // Auto-assign quests for each active job
        for (String jobName : data.getActiveJobs()) {
            assignQuestsForJob(player, data, jobName, dailyCount);
        }

        dataManager.savePlayerData(player.getUniqueId());
        player.sendMessage(plugin.getServer().getConsoleSender().getName()); // silence
        player.sendActionBar(Component.text("✦ Daily quests have reset! ✦", NamedTextColor.GOLD, TextDecoration.BOLD));
    }

    /**
     * Assigns up to {@code count} new quests for the given job from its pool.
     * Existing quests for that job are NOT removed (they carry over until reset).
     */
    public void assignQuestsForJob(Player player, PlayerData data, String jobName, int count) {
        JobType type = JobType.fromString(jobName);
        if (type == null) return;

        List<QuestTemplate> templates;
        switch (type) {
            case FARMER -> templates = FarmerQuestPool.pickRandom(count);
            // Future jobs: add cases here
            default     -> { return; }
        }

        for (QuestTemplate t : templates) {
            if (!data.canTakeQuest()) break;
            ActiveQuest aq = t.generate();
            data.addActiveQuest(aq);
            data.decrementQuestSlot();
        }
    }

    // ── Progress tracking ─────────────────────────────────────────────────────

    /**
     * Adds progress to all active, incomplete quests matching the given job and task type.
     * Automatically grants XP on completion.
     *
     * @param player   the acting player
     * @param jobName  e.g. "FARMER"
     * @param taskType the type of task completed
     * @param amount   units to add
     */
    public void addProgress(Player player, String jobName, QuestTaskType taskType, int amount) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (!data.hasJob(jobName)) return;

        boolean anyChanged = false;

        for (ActiveQuest aq : data.getActiveQuests()) {
            if (aq.isCompleted()) continue;
            if (!aq.getJobType().equalsIgnoreCase(jobName)) continue;
            if (aq.getTaskType() != taskType) continue;

            boolean justCompleted = aq.addProgress(amount);
            anyChanged = true;

            if (justCompleted) {
                onQuestComplete(player, data, aq);
            }
        }

        if (anyChanged) {
            dataManager.savePlayerData(player.getUniqueId());
        }
    }

    // ── Quest completion ──────────────────────────────────────────────────────

    private void onQuestComplete(Player player, PlayerData data, ActiveQuest quest) {
        // Award XP to the job
        boolean leveledUp = data.addXP(quest.getJobType(), quest.getXpReward());
        quest.setRewardClaimed(true);

        // Notify player
        player.sendMessage(
            "§6§l[Jobs] §r§aQuest complete! §e" + quest.getDescription()
            + " §7| §b+" + quest.getXpReward() + " XP"
        );

        if (leveledUp) {
            int newLevel = data.getLevel(quest.getJobType());
            player.sendMessage(
                "§6§l[Jobs] §r§e§lLEVEL UP! §r§eYour §6"
                + quest.getJobType() + "§e level is now §6" + newLevel + "§e!"
            );

            // Check for level rewards (delegated to RewardManager)
            plugin.getRewardManager().checkAndGrantLevelReward(player, quest.getJobType(), newLevel);
        }

        dataManager.savePlayerData(player.getUniqueId());
    }

    // ── Passive XP (not quest-based) ─────────────────────────────────────────

    /**
     * Grants passive XP directly to the player's job (e.g. 0.2 per crop harvested).
     * Handles level-up notification.
     */
    public void grantPassiveXP(Player player, String jobName, double amount) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (!data.hasJob(jobName)) return;

        boolean leveledUp = data.addXP(jobName, amount);
        if (leveledUp) {
            int newLevel = data.getLevel(jobName);
            player.sendMessage(
                "§6§l[Jobs] §r§e§lLEVEL UP! §r§eYour §6"
                + jobName + "§e level is now §6" + newLevel + "§e!"
            );
            plugin.getRewardManager().checkAndGrantLevelReward(player, jobName, newLevel);
        }

        // Save periodically – save on every 10th passive XP grant to reduce I/O
        // The plugin saves on quit/shutdown anyway
        if (Math.random() < 0.1) dataManager.savePlayerData(player.getUniqueId());
    }
}
