package com.enhancedjobs.listeners;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.items.CustomItems;
import com.enhancedjobs.quests.QuestManager;
import com.enhancedjobs.quests.QuestTaskType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Handles all gameplay events that:
 *   1. Add progress to active Farmer quests.
 *   2. Grant passive XP (0.2 per crop harvested).
 *
 * Passive XP crops: WHEAT, CARROT, POTATO, BEETROOT, PUMPKIN, MELON,
 * SWEET_BERRIES, GLOW_BERRIES, SUGAR_CANE, COCOA_BEANS, NETHER_WART,
 * TORCHFLOWER, PITCHER_PLANT.
 */
public class FarmerListener implements Listener {

    private static final Set<Material> PLANTABLE_CROPS = Set.of(
            Material.WHEAT_SEEDS, Material.CARROT, Material.POTATO,
            Material.BEETROOT_SEEDS, Material.MELON_SEEDS, Material.PUMPKIN_SEEDS,
            Material.NETHER_WART, Material.SWEET_BERRIES, Material.TORCHFLOWER_SEEDS,
            Material.PITCHER_POD
    );

    private static final Set<Material> EAT_FOODS = Set.of(
            Material.MELON_SLICE, Material.APPLE, Material.CARROT,
            Material.GOLDEN_APPLE, Material.GOLDEN_CARROT
    );

    private static final Set<Material> PASSIVE_XP_CROPS = Set.of(
            Material.WHEAT, Material.CARROT, Material.POTATO,
            Material.BEETROOT, Material.PUMPKIN, Material.MELON,
            Material.SUGAR_CANE, Material.COCOA_BEANS, Material.NETHER_WART,
            Material.SWEET_BERRIES, Material.GLOW_BERRIES,
            Material.TORCHFLOWER, Material.PITCHER_PLANT
    );

    private final EnhancedJobSystem plugin;
    private final QuestManager      questManager;

    public FarmerListener(EnhancedJobSystem plugin) {
        this.plugin       = plugin;
        this.questManager = plugin.getQuestManager();
    }

    // ── Harvest Wheat ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block  block  = event.getBlock();
        Material type = block.getType();

        // Check for money tree break (overrides normal sapling logic)
        if (type == Material.OAK_SAPLING) {
            boolean handled = plugin.getMoneyTreeManager().onBreak(player, block.getLocation());
            if (handled) { event.setCancelled(true); return; }
        }

        // Check for AutoFarm block break
        if (CustomItems.isAutoFarm(player.getInventory().getItemInMainHand())) {
            // handled separately — but if they break the center block, remove the farm
        }
        if (plugin.getAutoFarmManager().isFarmCenter(block.getLocation())) {
            plugin.getAutoFarmManager().removeFarm(block.getLocation());
        }

        // Only count fully-grown crops
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() < ageable.getMaximumAge()) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // ── Passive XP ────────────────────────────────────────────────────────
        if (data.hasJob("FARMER") && PASSIVE_XP_CROPS.contains(type)) {
            questManager.grantPassiveXP(player, "FARMER",
                    plugin.getConfig().getDouble("passive-xp.harvest-crop", 0.2));
        }

        // ── Quest: HARVEST_WHEAT ──────────────────────────────────────────────
        if (type == Material.WHEAT) {
            questManager.addProgress(player, "FARMER", QuestTaskType.HARVEST_WHEAT, 1);
        }

        // ── Quest: HARVEST_CARROTS ────────────────────────────────────────────
        if (type == Material.CARROTS) {
            questManager.addProgress(player, "FARMER", QuestTaskType.HARVEST_CARROTS, 1);
        }

        // ── Hat auto-replant ──────────────────────────────────────────────────
        ItemStack helmet = player.getInventory().getHelmet();
        if (CustomItems.isFarmersHat(helmet)) {
            autoReplant(block);
        }
    }

    // ── Plant Crops ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material placed = event.getBlockPlaced().getType();

        // Detect Money Tree Seed placement on farmland
        ItemStack hand = event.getItemInHand();
        if (CustomItems.isMoneyTreeSeed(hand)) {
            event.setCancelled(true); // prevent normal sapling placement
            Block farmland = event.getBlockAgainst();
            if (farmland.getType() == Material.FARMLAND) {
                plugin.getMoneyTreeManager().plant(player, farmland.getLocation());
                // Remove 1 seed from hand
                hand.setAmount(hand.getAmount() - 1);
            } else {
                player.sendMessage("§c[Jobs] Money Tree Seeds must be planted on farmland.");
            }
            return;
        }

        // Detect AutoFarm block placement
        if (CustomItems.isAutoFarm(hand)) {
            event.setCancelled(true);
            plugin.getAutoFarmManager().placeFarm(player, event.getBlockPlaced().getLocation());
            hand.setAmount(hand.getAmount() - 1);
            return;
        }

        // Plant Crops quest
        if (PLANTABLE_CROPS.contains(placed)) {
            questManager.addProgress(player, "FARMER", QuestTaskType.PLANT_CROPS, 1);
        }
    }

    // ── Breed Cows ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;
        if (event.getEntity().getType() != EntityType.COW) return;
        questManager.addProgress(player, "FARMER", QuestTaskType.BREED_COWS, 1);
    }

    // ── Collect Eggs ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Item pickedUp = event.getItem();
        if (pickedUp.getItemStack().getType() == Material.EGG) {
            int amount = pickedUp.getItemStack().getAmount();
            questManager.addProgress(player, "FARMER", QuestTaskType.COLLECT_EGGS, amount);
        }
    }

    // ── Milk Cows ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player  = event.getPlayer();
        Entity entity  = event.getRightClicked();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (entity.getType() == EntityType.COW && hand.getType() == Material.BUCKET) {
            questManager.addProgress(player, "FARMER", QuestTaskType.MILK_COWS, 1);
        }
    }

    // ── Eat Foods ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (EAT_FOODS.contains(event.getItem().getType())) {
            questManager.addProgress(player, "FARMER", QuestTaskType.EAT_FOOD, 1);
        }
    }

    // ── Create Scarecrow ──────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getCurrentItem();
        if (CustomItems.isScarecrow(result)) {
            questManager.addProgress(player, "FARMER", QuestTaskType.CREATE_SCARECROW, 1);
        }
    }

    // ── Auto-replant helper ───────────────────────────────────────────────────

    private void autoReplant(Block block) {
        Material type    = block.getType();
        Material seed    = getSeedFor(type);
        if (seed == null) return;

        // Schedule 1 tick later so the original harvest finishes first
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (block.getType() == Material.AIR || block.getType() == Material.FARMLAND) {
                block.setType(seed);
            }
        }, 1L);
    }

    private Material getSeedFor(Material crop) {
        return switch (crop) {
            case WHEAT   -> Material.WHEAT;
            case CARROTS -> Material.CARROTS;
            case POTATOES-> Material.POTATOES;
            case BEETROOTS -> Material.BEETROOTS;
            default      -> null;
        };
    }
}
