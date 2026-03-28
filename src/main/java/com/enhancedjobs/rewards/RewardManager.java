package com.enhancedjobs.rewards;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.DataManager;
import org.bukkit.entity.Player;

/**
 * Central reward dispatcher.
 * When a player levels up in any job, QuestManager calls
 * {@link #checkAndGrantLevelReward(Player, String, int)} here, which routes
 * to the appropriate job-specific reward handler.
 *
 * Adding a new job: create a new XxxRewards class and add a case below.
 */
public class RewardManager {

    private final FarmerRewards farmerRewards;

    public RewardManager(EnhancedJobSystem plugin, DataManager dataManager) {
        this.farmerRewards = new FarmerRewards(plugin, dataManager, plugin.getEconomyHook());
    }

    /**
     * Called whenever a player levels up in any job.
     *
     * @param player   the player who leveled up
     * @param jobName  uppercase job name, e.g. "FARMER"
     * @param newLevel the level they just reached (2–10)
     */
    public void checkAndGrantLevelReward(Player player, String jobName, int newLevel) {
        switch (jobName.toUpperCase()) {
            case "FARMER" -> farmerRewards.onLevelUp(player, newLevel);
            // Future jobs: add cases here
        }
    }

    // ── Expose individual reward managers for GUI claim buttons ───────────────

    public FarmerRewards getFarmerRewards() { return farmerRewards; }
}
