package com.enhancedjobs.listeners;

import com.enhancedjobs.EnhancedJobSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Ensures player data, AutoFarms, and MoneyTrees are loaded on join
 * and saved+unloaded on quit.
 */
public class PlayerSessionListener implements Listener {

    private final EnhancedJobSystem plugin;

    public PlayerSessionListener(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        // Load data into cache (if not already loaded)
        plugin.getDataManager().getPlayerData(uuid);
        // Hydrate AutoFarm and MoneyTree records
        plugin.getAutoFarmManager().loadFarmsForPlayer(uuid);
        plugin.getMoneyTreeManager().loadTreesForPlayer(uuid);
        // Check for pending daily reset
        plugin.getQuestManager().checkAndReset(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        plugin.getDataManager().unload(uuid);
    }
}
