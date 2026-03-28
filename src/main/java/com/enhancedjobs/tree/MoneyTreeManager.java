package com.enhancedjobs.tree;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.api.GoldEconomyHook;
import com.enhancedjobs.data.DataManager;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.items.CustomItems;
import com.enhancedjobs.utils.XPUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Tracks every planted Money Tree seed.
 *
 * – Planting: player right-clicks farmland with a Money Tree Seed item.
 * – Growth:   virtual, 15 real-time hours (configurable).
 * – Harvest:  player breaks the sapling block; if fully grown they receive
 *             5–15 gold coins + 1–4 Money Tree Seeds back, else nothing.
 *
 * The planted sapling uses the vanilla OAK_SAPLING block so it looks natural.
 * We track it by location so we can identify it on break.
 */
public class MoneyTreeManager {

    private static final long GROW_MS = 15L * 60 * 60 * 1000; // 15 hours
    private static final Random RANDOM = new Random();

    private final EnhancedJobSystem plugin;
    private final DataManager dataManager;
    private final GoldEconomyHook economy;

    /** world+coords → {ownerUUID, plantTime} */
    private final Map<String, TreeRecord> trees = new HashMap<>();

    public MoneyTreeManager(EnhancedJobSystem plugin, DataManager dataManager, GoldEconomyHook economy) {
        this.plugin      = plugin;
        this.dataManager = dataManager;
        this.economy     = economy;
        long configHours = plugin.getConfig().getLong("money-tree-grow-hours", 15);
        // Allow override via config (stored in GROW_MS effectively — we'll compute inline)
    }

    // ── Plant ─────────────────────────────────────────────────────────────────

    /**
     * Called when a player right-clicks farmland while holding a Money Tree Seed.
     * Places an oak sapling at the block above the farmland and registers it.
     */
    public boolean plant(Player player, Location farmlandLoc) {
        Block sapling = farmlandLoc.getBlock().getRelative(0, 1, 0);
        if (sapling.getType() != Material.AIR) {
            player.sendMessage("§c[Jobs] There's no room to plant there.");
            return false;
        }

        sapling.setType(Material.OAK_SAPLING);
        String key = encodeKey(sapling.getLocation());
        long configHours = plugin.getConfig().getLong("money-tree-grow-hours", 15);
        trees.put(key, new TreeRecord(player.getUniqueId(), System.currentTimeMillis(), configHours * 3_600_000L));

        // Persist
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        data.getMoneyTreeData().add(encodeTree(sapling.getLocation(), player.getUniqueId()));
        dataManager.savePlayerData(player.getUniqueId());

        player.sendMessage("§2§l[Jobs] §r§aMoney Tree planted! It will be ready in §e"
                + configHours + " hours§a.");
        return true;
    }

    // ── Harvest ───────────────────────────────────────────────────────────────

    /**
     * Called when a player breaks an OAK_SAPLING block.
     * Returns true if this was a registered Money Tree (cancels vanilla drop).
     */
    public boolean onBreak(Player player, Location loc) {
        String key = encodeKey(loc);
        TreeRecord record = trees.get(key);
        if (record == null) return false;

        long elapsed = System.currentTimeMillis() - record.plantTime;
        if (elapsed < record.growMs) {
            long remaining = record.growMs - elapsed;
            player.sendMessage("§c[Jobs] This Money Tree hasn't fully grown yet. ("
                    + XPUtils.formatDurationShort(remaining) + " remaining)");
            // Don't remove – tree stays planted
            return true;
        }

        // Fully grown – give rewards
        int coins = 5 + RANDOM.nextInt(11);   // 5–15
        int seeds = 1 + RANDOM.nextInt(4);    // 1–4

        if (economy.isEnabled()) {
            economy.deposit(player, coins);
            player.sendMessage("§6§l[Jobs] §r§aMoney Tree harvested! §e+" + coins + " gold coins §aand §e"
                    + seeds + " Money Tree Seeds§a returned.");
        } else {
            player.sendMessage("§6§l[Jobs] §r§aMoney Tree harvested! "
                    + "§c(GoldEconomy not available – coins not given.) §e"
                    + seeds + " Money Tree Seeds§a returned.");
        }

        ItemStack seedReturn = CustomItems.createMoneyTreeSeeds(seeds);
        player.getInventory().addItem(seedReturn);

        // Remove tree
        trees.remove(key);
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        data.getMoneyTreeData().removeIf(s -> s.startsWith(key));
        dataManager.savePlayerData(player.getUniqueId());

        return true;
    }

    // ── Check if location is a money tree ────────────────────────────────────

    public boolean isMoneyTree(Location loc) {
        return trees.containsKey(encodeKey(loc));
    }

    /** Returns remaining grow time for display, or 0 if ready / not a tree. */
    public long getRemainingGrowMs(Location loc) {
        TreeRecord r = trees.get(encodeKey(loc));
        if (r == null) return 0;
        long elapsed = System.currentTimeMillis() - r.plantTime;
        return Math.max(0, r.growMs - elapsed);
    }

    // ── Load from player data ─────────────────────────────────────────────────

    public void loadTreesForPlayer(UUID uuid) {
        PlayerData data = dataManager.getPlayerData(uuid);
        long configHours = plugin.getConfig().getLong("money-tree-grow-hours", 15);
        for (String encoded : data.getMoneyTreeData()) {
            try {
                String[] p = encoded.split(",");
                World w = Bukkit.getWorld(p[0]);
                if (w == null) continue;
                Location loc = new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                long plantTime = Long.parseLong(p[4]);
                UUID ownerUUID = UUID.fromString(p[5]);
                trees.put(encodeKey(loc), new TreeRecord(ownerUUID, plantTime, configHours * 3_600_000L));
            } catch (Exception ignored) {}
        }
    }

    // ── Encoding ──────────────────────────────────────────────────────────────

    private String encodeKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private String encodeTree(Location loc, UUID owner) {
        return encodeKey(loc) + "," + System.currentTimeMillis() + "," + owner;
    }

    // ── Inner record ──────────────────────────────────────────────────────────

    private static class TreeRecord {
        UUID ownerUUID;
        long plantTime;
        long growMs;

        TreeRecord(UUID ownerUUID, long plantTime, long growMs) {
            this.ownerUUID = ownerUUID;
            this.plantTime = plantTime;
            this.growMs    = growMs;
        }
    }
}
