package com.enhancedjobs.farm;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.DataManager;
import com.enhancedjobs.data.PlayerData;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages all placed Automatic Farm blocks for all players.
 *
 * When an AutoFarm block is placed:
 *  1. A 3×3 of FARMLAND is created below the block's level.
 *  2. WHEAT is planted on every farmland tile.
 *  3. A CHEST is placed adjacent (south of the 3×3 center) to receive drops.
 *  4. The farm is registered and persisted in the owning player's data.
 *
 * A scheduler runs every 5 minutes. For each farm whose harvest interval has
 * elapsed, it loads the chunk (if needed), harvests fully-grown wheat, stores
 * drops in the chest, and replants.
 */
public class AutoFarmManager {

    /** Harvest cycle length in milliseconds (default 30 minutes, config-driven). */
    private long harvestIntervalMs;

    private final EnhancedJobSystem plugin;
    private final DataManager dataManager;

    /** farmKey → FarmRecord (loaded at startup, written through to player data). */
    private final Map<String, FarmRecord> activeFarms = new HashMap<>();

    private BukkitTask task;

    public AutoFarmManager(EnhancedJobSystem plugin, DataManager dataManager) {
        this.plugin      = plugin;
        this.dataManager = dataManager;
        this.harvestIntervalMs = plugin.getConfig().getInt("auto-farm-harvest-interval", 30) * 60_000L;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void loadAll() {
        // Called after DataManager is ready; re-hydrate farms from all online players.
        // Offline player farms are loaded lazily when their data is loaded.
    }

    public void startScheduler() {
        // Check every 5 minutes
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20 * 60 * 5L, 20 * 60 * 5L);
    }

    public void stopScheduler() {
        if (task != null) task.cancel();
    }

    // ── Farm placement ────────────────────────────────────────────────────────

    /**
     * Called when a player places the AutoFarm block.
     * Creates the physical 3×3 farm, registers it, and saves.
     *
     * @param player  owning player
     * @param center  the block location where the AutoFarm item was placed
     * @return true if placement succeeded
     */
    public boolean placeFarm(Player player, Location center) {
        World world = center.getWorld();
        if (world == null) return false;

        int cx = center.getBlockX();
        int cy = center.getBlockY();   // ground level; farmland goes here
        int cz = center.getBlockZ();

        // Build 3×3 farmland + wheat one block below the placed block
        int farmY = cy - 1;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block b = world.getBlockAt(cx + dx, farmY, cz + dz);
                b.setType(Material.FARMLAND);
                Block above = world.getBlockAt(cx + dx, farmY + 1, cz + dz);
                above.setType(Material.WHEAT);
                if (above.getBlockData() instanceof Ageable a) { a.setAge(0); above.setBlockData(a); }
            }
        }

        // Place chest south of the 3×3 (at cz+2, same y level as wheat)
        Location chestLoc = new Location(world, cx, farmY + 1, cz + 2);
        chestLoc.getBlock().setType(Material.CHEST);

        // Register
        long now = System.currentTimeMillis();
        String key = encodeKey(center);
        FarmRecord record = new FarmRecord(
                center.clone(), chestLoc.clone(),
                player.getUniqueId(), now
        );
        activeFarms.put(key, record);

        // Persist into player data
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        data.getAutoFarmData().add(encodeFarm(record));
        dataManager.savePlayerData(player.getUniqueId());

        player.sendMessage("§6§l[Jobs] §r§aAutomatic Farm placed! It will harvest every "
                + plugin.getConfig().getInt("auto-farm-harvest-interval", 30) + " minutes.");
        return true;
    }

    /**
     * Removes a farm record (e.g. when the blocks are destroyed).
     */
    public void removeFarm(Location center) {
        String key = encodeKey(center);
        FarmRecord removed = activeFarms.remove(key);
        if (removed == null) return;

        PlayerData data = dataManager.getPlayerData(removed.ownerUUID);
        data.getAutoFarmData().removeIf(s -> s.startsWith(farmLocPrefix(center)));
        dataManager.savePlayerData(removed.ownerUUID);
    }

    public boolean isFarmCenter(Location loc) {
        return activeFarms.containsKey(encodeKey(loc));
    }

    // ── Load farms from a player's data ──────────────────────────────────────

    public void loadFarmsForPlayer(UUID uuid) {
        PlayerData data = dataManager.getPlayerData(uuid);
        for (String encoded : data.getAutoFarmData()) {
            FarmRecord rec = decodeFarm(encoded, uuid);
            if (rec != null) activeFarms.put(encodeKey(rec.center), rec);
        }
    }

    // ── Scheduler tick ────────────────────────────────────────────────────────

    private void tick() {
        long now = System.currentTimeMillis();
        for (FarmRecord farm : activeFarms.values()) {
            if (now - farm.lastHarvest >= harvestIntervalMs) {
                processFarm(farm, now);
            }
        }
    }

    private void processFarm(FarmRecord farm, long now) {
        World world = farm.center.getWorld();
        if (world == null) return;

        int chunkX = farm.center.getBlockX() >> 4;
        int chunkZ = farm.center.getBlockZ() >> 4;

        // Load chunk synchronously if needed (Paper handles this safely on main thread)
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.loadChunk(chunkX, chunkZ, false);
        }

        // Harvest wheat from 3×3 area
        List<ItemStack> drops = new ArrayList<>();
        int cx = farm.center.getBlockX();
        int farmY = farm.center.getBlockY() - 1; // farmland level, wheat is farmY+1
        int cz = farm.center.getBlockZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block wheatBlock = world.getBlockAt(cx + dx, farmY + 1, cz + dz);
                if (wheatBlock.getType() == Material.WHEAT) {
                    if (wheatBlock.getBlockData() instanceof Ageable a
                            && a.getAge() == a.getMaximumAge()) {
                        drops.addAll(wheatBlock.getDrops(new ItemStack(Material.DIAMOND_HOE)));
                        // Replant
                        wheatBlock.setType(Material.WHEAT);
                        if (wheatBlock.getBlockData() instanceof Ageable fresh) {
                            fresh.setAge(0);
                            wheatBlock.setBlockData(fresh);
                        }
                    }
                } else {
                    // Crop was broken externally – replant farmland
                    Block farmland = world.getBlockAt(cx + dx, farmY, cz + dz);
                    if (farmland.getType() == Material.FARMLAND) {
                        wheatBlock.setType(Material.WHEAT);
                        if (wheatBlock.getBlockData() instanceof Ageable fresh) {
                            fresh.setAge(0);
                            wheatBlock.setBlockData(fresh);
                        }
                    }
                }
            }
        }

        // Store drops into chest
        if (!drops.isEmpty()) {
            Block chestBlock = farm.chestLoc.getBlock();
            if (chestBlock.getState() instanceof Chest chest) {
                Inventory inv = chest.getInventory();
                for (ItemStack drop : drops) {
                    Map<Integer, ItemStack> overflow = inv.addItem(drop);
                    if (!overflow.isEmpty()) {
                        // Chest full – drop items on ground near chest
                        for (ItemStack leftover : overflow.values()) {
                            world.dropItemNaturally(farm.chestLoc, leftover);
                        }
                    }
                }
            }
        }

        farm.lastHarvest = now;

        // Update persistence
        PlayerData data = dataManager.getPlayerData(farm.ownerUUID);
        data.getAutoFarmData().replaceAll(s ->
                s.startsWith(farmLocPrefix(farm.center)) ? encodeFarm(farm) : s);
        dataManager.savePlayerData(farm.ownerUUID);
    }

    // ── Encoding helpers ──────────────────────────────────────────────────────

    private String encodeKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private String farmLocPrefix(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ",";
    }

    private String encodeFarm(FarmRecord r) {
        return r.center.getWorld().getName() + "," + r.center.getBlockX() + "," + r.center.getBlockY() + ","
                + r.center.getBlockZ() + "," + r.chestLoc.getBlockX() + "," + r.chestLoc.getBlockY() + ","
                + r.chestLoc.getBlockZ() + "," + r.lastHarvest;
    }

    private FarmRecord decodeFarm(String s, UUID ownerUUID) {
        try {
            String[] p = s.split(",");
            World w    = Bukkit.getWorld(p[0]);
            if (w == null) return null;
            Location center = new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            Location chest  = new Location(w, Integer.parseInt(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]));
            long lastHarvest = Long.parseLong(p[7]);
            FarmRecord rec   = new FarmRecord(center, chest, ownerUUID, lastHarvest);
            return rec;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Inner record ──────────────────────────────────────────────────────────

    public static class FarmRecord {
        public Location center;
        public Location chestLoc;
        public UUID ownerUUID;
        public long lastHarvest;

        public FarmRecord(Location center, Location chestLoc, UUID ownerUUID, long lastHarvest) {
            this.center      = center;
            this.chestLoc    = chestLoc;
            this.ownerUUID   = ownerUUID;
            this.lastHarvest = lastHarvest;
        }
    }
}
