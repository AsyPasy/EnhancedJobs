package com.enhancedjobs.jobs;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.DataManager;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.quests.QuestManager;
import com.enhancedjobs.utils.XPUtils;
import org.bukkit.entity.Player;

/**
 * Manages players joining/leaving jobs, and provides admin-facing helpers
 * for setting levels and XP.
 */
public class JobManager {

    private final EnhancedJobSystem plugin;
    private final DataManager dataManager;

    public JobManager(EnhancedJobSystem plugin, DataManager dataManager) {
        this.plugin      = plugin;
        this.dataManager = dataManager;
    }

    // ── Join / Leave ──────────────────────────────────────────────────────────

    /** Adds a job to a player. Returns false if they already have it. */
    public boolean joinJob(Player player, JobType type) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.hasJob(type.name())) return false;

        data.addJob(type.name());

        // Immediately assign today's quests if they haven't been reset yet
        QuestManager qm = plugin.getQuestManager();
        qm.checkAndReset(player);

        // If reset didn't assign quests (no reset was due), assign quests now for new job
        if (data.getActiveQuestsForJob(type.name()).isEmpty() && data.canTakeQuest()) {
            int slots = data.getDailyQuestsRemaining();
            qm.assignQuestsForJob(player, data, type.name(), slots);
        }

        dataManager.savePlayerData(player.getUniqueId());
        player.sendMessage("§6§l[Jobs] §r§aYou are now a §e" + type.getDisplayName() + "§a!");
        return true;
    }

    /** Removes a job from a player. Returns false if they don't have it. */
    public boolean leaveJob(Player player, JobType type) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (!data.hasJob(type.name())) return false;

        data.removeJob(type.name());
        dataManager.savePlayerData(player.getUniqueId());
        player.sendMessage("§6§l[Jobs] §r§cYou have left the §e" + type.getDisplayName() + "§c job.");
        return true;
    }

    // ── Admin helpers ─────────────────────────────────────────────────────────

    /** Admin: set a player's level for a job directly. */
    public void adminSetLevel(Player target, JobType type, int level) {
        PlayerData data = dataManager.getPlayerData(target.getUniqueId());
        if (!data.hasJob(type.name())) data.addJob(type.name());
        level = Math.max(1, Math.min(level, XPUtils.MAX_LEVEL));
        data.setLevel(type.name(), level);
        data.setXP(type.name(), 0.0);
        dataManager.savePlayerData(target.getUniqueId());
    }

    /** Admin: add XP to a player's job. */
    public void adminAddXP(Player target, JobType type, double xp) {
        PlayerData data = dataManager.getPlayerData(target.getUniqueId());
        if (!data.hasJob(type.name())) data.addJob(type.name());
        boolean leveled = data.addXP(type.name(), xp);
        if (leveled) {
            plugin.getRewardManager().checkAndGrantLevelReward(
                    target, type.name(), data.getLevel(type.name()));
        }
        dataManager.savePlayerData(target.getUniqueId());
    }

    /** Admin: reset a player's daily quests for all jobs. */
    public void adminResetQuests(Player target) {
        PlayerData data = dataManager.getPlayerData(target.getUniqueId());
        int dailyCount  = plugin.getConfig().getInt("daily-quests", 3);
        data.performDailyReset(dailyCount);
        for (String job : data.getActiveJobs()) {
            plugin.getQuestManager().assignQuestsForJob(target, data, job, dailyCount);
        }
        dataManager.savePlayerData(target.getUniqueId());
    }
}
