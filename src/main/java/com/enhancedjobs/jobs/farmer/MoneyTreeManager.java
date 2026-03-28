package com.enhancedjobs.jobs.farmer;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.utils.ItemUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MoneyTreeManager implements Listener {

    private final EnhancedJobSystem plugin;
    private final File dataFile;

    // Location -> planted timestamp
    private final Map<Location, Long> plantedTrees = new HashMap<>();

    private static final long GROW_TIME_MS = TimeUnit.HOURS.toMillis(15);

    public MoneyTreeManager(EnhancedJobSystem plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "moneytrees.yml");
        loadData();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ── Plant ─────────────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        if (!ItemUtils.isMoneyTreeSeed(item)) return;

        Location loc = event.getBlockPlaced().getLocation();
        plantedTrees.put(loc, System.currentTimeMillis());
        saveData();
        player.sendMessage("§a🌱 Money Tree planted! It will be ready in §e15 hours§a.");
    }

    // ── Harvest ───────────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!plantedTrees.containsKey(loc)) return;

        event.setDropItems(false);
        Player player = event.getPlayer();
        long planted  = plantedTrees.get(loc);

        if (System.currentTimeMillis() - planted < GROW_TIME_MS) {
            long remaining = GROW_TIME_MS - (System.currentTimeMillis() - planted);
            long hoursLeft = TimeUnit.MILLISECONDS.toHours(remaining);
            long minsLeft  = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
            player.sendMessage("§cThis Money Tree is not ready yet! §e" + hoursLeft + "h " + minsLeft + "m remaining.");
            event.setCancelled(true);
            return;
        }

        plantedTrees.remove(loc);
        saveData();

        Random random = new Random();
        int goldAmount = 5 + random.nextInt(11); // 5–15 gold
        int seedsBack  = 1 + random.nextInt(4);  // 1–4 seeds

        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).addGold(goldAmount);
        player.sendMessage("§6💰 Your Money Tree dropped §e" + goldAmount + " gold coins§6!");

        for (int i = 0; i < seedsBack; i++)
            player.getInventory().addItem(ItemUtils.createMoneyTreeSeed(1).get(0));

        player.sendMessage("§a🌱 You recovered §e" + seedsBack + " Money Tree Seed(s)§a.");
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void saveData() {
        YamlConfiguration cfg = new YamlConfiguration();
        List<String> entries = new ArrayList<>();
        for (Map.Entry<Location, Long> e : plantedTrees.entrySet())
            entries.add(serializeLoc(e.getKey()) + ":" + e.getValue());
        cfg.set("trees", entries);
        try { cfg.save(dataFile); } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void loadData() {
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String s : cfg.getStringList("trees")) {
            String[] parts = s.split(":");
            if (parts.length < 2) continue;
            Location loc = deserializeLoc(parts[0]);
            if (loc != null) plantedTrees.put(loc, Long.parseLong(parts[1]));
        }
    }

    private String serializeLoc(Location l) {
        return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    private Location deserializeLoc(String s) {
        try {
            String[] p = s.split(",");
            World w = Bukkit.getWorld(p[0]);
            return w == null ? null : new Location(w, Integer.parseInt(p[1]),
                                                       Integer.parseInt(p[2]),
                                                       Integer.parseInt(p[3]));
        } catch (Exception e) { return null; }
    }
}
