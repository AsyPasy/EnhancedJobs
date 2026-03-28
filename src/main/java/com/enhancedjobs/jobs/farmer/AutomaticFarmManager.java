package com.enhancedjobs.jobs.farmer;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.utils.ItemUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AutomaticFarmManager implements Listener {

    private final EnhancedJobSystem plugin;
    private final File dataFile;

    private final Map<Location, FarmData>  farms       = new HashMap<>();
    private final Map<Location, Location>  chestToFarm = new HashMap<>();

    private BukkitTask harvestTask;

    public enum CropChoice {
        WHEAT     (Material.WHEAT,     "§fWheat",     Material.WHEAT_SEEDS, Material.WHEAT),
        CARROTS   (Material.CARROTS,   "§6Carrots",   Material.CARROT,      Material.CARROT),
        POTATOES  (Material.POTATOES,  "§ePotatoes",  Material.POTATO,      Material.POTATO),
        BEETROOTS (Material.BEETROOTS, "§cBeetroots", Material.BEETROOT_SEEDS, Material.BEETROOT),
        MONEY_TREE(Material.OAK_SAPLING, "§6Money Tree", null,             Material.OAK_SAPLING);

        public final Material blockMaterial;  // the block placed in the world
        public final String   displayName;
        public final Material seedMaterial;   // null for money tree
        public final Material guiMaterial;    // item shown in the GUI

        CropChoice(Material block, String name, Material seed, Material gui) {
            this.blockMaterial = block;
            this.displayName   = name;
            this.seedMaterial  = seed;
            this.guiMaterial   = gui;
        }
    }

    public static class FarmData {
        public final Location center;
        public CropChoice crop = CropChoice.WHEAT;
        public UUID owner;

        FarmData(Location center, UUID owner) {
            this.center = center.clone();
            this.owner  = owner;
        }
    }

    public AutomaticFarmManager(EnhancedJobSystem plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "autofarms.yml");
        loadData();
        startHarvestTask();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public Location getFarmCenterForChest(Location chestLoc) {
        return chestToFarm.get(chestLoc);
    }

    public FarmData getFarmData(Location center) {
        return farms.get(center);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player    player = event.getPlayer();
        ItemStack item   = event.getItemInHand();
        if (!ItemUtils.isAutoFarmBlock(item)) return;
        spawnFarm(event.getBlockPlaced().getLocation(), player.getUniqueId());
        player.sendMessage("§a✔ Automatic Farm placed! §7Shift+right-click the chest to change the crop.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!farms.containsKey(loc)) return;
        event.setDropItems(false);
        removeFarm(loc);
        event.getPlayer().sendMessage("§cAutomatic Farm removed.");
    }

    public void setCropType(Location farmCenter, CropChoice choice, Player player) {
        FarmData data = farms.get(farmCenter);
        if (data == null) return;
        data.crop = choice;
        replantAll(data);
        saveData();
        player.sendMessage("§a✔ Auto-Farm crop changed to §e" + choice.displayName + "§a!");
    }

    private void spawnFarm(Location center, UUID ownerUuid) {
        World world = center.getWorld();
        if (world == null) return;

        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block soil = world.getBlockAt(cx + x, cy - 1, cz + z);
                Block crop = world.getBlockAt(cx + x, cy,     cz + z);

                soil.setType(Material.FARMLAND);
                Farmland fd = (Farmland) soil.getBlockData();
                fd.setMoisture(fd.getMaximumMoisture());
                soil.setBlockData(fd, false);

                crop.setType(Material.WHEAT);
                if (crop.getBlockData() instanceof Ageable a) { a.setAge(0); crop.setBlockData(a, false); }
            }
        }

        Block chestBlock = world.getBlockAt(cx, cy - 1, cz + 2);
        chestBlock.setType(Material.CHEST);

        Location centerKey = center.clone();
        Location chestLoc  = chestBlock.getLocation().clone();

        FarmData farmData = new FarmData(centerKey, ownerUuid);
        farms.put(centerKey, farmData);
        chestToFarm.put(chestLoc, centerKey);
        saveData();
    }

    private void removeFarm(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();

        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) {
                world.getBlockAt(cx + x, cy,     cz + z).setType(Material.AIR);
                world.getBlockAt(cx + x, cy - 1, cz + z).setType(Material.DIRT);
            }

        Block chestBlock = world.getBlockAt(cx, cy - 1, cz + 2);
        if (chestBlock.getType() == Material.CHEST) chestBlock.setType(Material.AIR);

        chestToFarm.remove(new Location(world, cx, cy - 1, cz + 2));
        farms.remove(center);
        saveData();
    }

    private void replantAll(FarmData data) {
        World world = data.center.getWorld();
        if (world == null) return;

        int cx = data.center.getBlockX(), cy = data.center.getBlockY(), cz = data.center.getBlockZ();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block soil = world.getBlockAt(cx + x, cy - 1, cz + z);
                Block crop = world.getBlockAt(cx + x, cy,     cz + z);

                if (soil.getType() != Material.FARMLAND) soil.setType(Material.FARMLAND);
                Farmland fd = (Farmland) soil.getBlockData();
                fd.setMoisture(fd.getMaximumMoisture());
                soil.setBlockData(fd, false);

                if (data.crop == CropChoice.MONEY_TREE) {
                    crop.setType(Material.AIR);
                } else {
                    crop.setType(data.crop.blockMaterial);
                    if (crop.getBlockData() instanceof Ageable a) { a.setAge(0); crop.setBlockData(a, false); }
                }
            }
        }
    }

    private void startHarvestTask() {
        long interval = plugin.getConfig().getLong("auto-farm-harvest-interval", 10) * 60 * 20L;
        harvestTask = plugin.getServer().getScheduler()
            .runTaskTimer(plugin, this::harvestAll, interval, interval);
    }

    private void harvestAll() {
        for (FarmData data : new ArrayList<>(farms.values())) {
            World world = data.center.getWorld();
            if (world == null || data.crop == CropChoice.MONEY_TREE) continue;

            Chest chest = findChest(data.center);
            if (chest == null) continue;

            int cx = data.center.getBlockX(), cy = data.center.getBlockY(), cz = data.center.getBlockZ();

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Block soil = world.getBlockAt(cx + x, cy - 1, cz + z);
                    Block crop = world.getBlockAt(cx + x, cy,     cz + z);

                    if (soil.getType() == Material.FARMLAND) {
                        Farmland fd = (Farmland) soil.getBlockData();
                        if (fd.getMoisture() < fd.getMaximumMoisture()) {
                            fd.setMoisture(fd.getMaximumMoisture());
                            soil.setBlockData(fd, false);
                        }
                    }

                    if (crop.getBlockData() instanceof Ageable ageable
                            && ageable.getAge() == ageable.getMaximumAge()) {
                        for (ItemStack drop : crop.getDrops()) {
                            Map<Integer, ItemStack> overflow = chest.getInventory().addItem(drop);
                            overflow.values().forEach(i -> world.dropItemNaturally(data.center, i));
                        }
                        crop.setType(data.crop.blockMaterial);
                        if (crop.getBlockData() instanceof Ageable fresh) { fresh.setAge(0); crop.setBlockData(fresh, false); }
                    }
                }
            }
        }
    }

    private Chest findChest(Location center) {
        World world = center.getWorld();
        if (world == null) return null;
        Block b = world.getBlockAt(center.getBlockX(), center.getBlockY() - 1, center.getBlockZ() + 2);
        return (b.getState() instanceof Chest c) ? c : null;
    }

    public void saveData() {
        YamlConfiguration cfg = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (FarmData data : farms.values())
            list.add(serializeLoc(data.center) + "|" + data.crop.name()
                + "|" + (data.owner != null ? data.owner : "none"));
        cfg.set("farms", list);
        try { cfg.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadData() {
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String s : cfg.getStringList("farms")) {
            String[] parts = s.split("\\|");
            Location loc = deserializeLoc(parts[0]);
            if (loc == null) continue;

            CropChoice crop = CropChoice.WHEAT;
            if (parts.length > 1) try { crop = CropChoice.valueOf(parts[1]); } catch (Exception ignored) {}

            UUID owner = null;
            if (parts.length > 2 && !parts[2].equals("none"))
                try { owner = UUID.fromString(parts[2]); } catch (Exception ignored) {}

            FarmData data = new FarmData(loc, owner);
            data.crop = crop;
            farms.put(loc, data);

            World world = loc.getWorld();
            if (world != null)
                chestToFarm.put(new Location(world, loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ() + 2), loc);
        }
    }

    private String serializeLoc(Location l) {
        return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    private Location deserializeLoc(String s) {
        try {
            String[] p = s.split(",");
            World w = Bukkit.getWorld(p[0]);
            return w == null ? null : new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (Exception e) { return null; }
    }
}
