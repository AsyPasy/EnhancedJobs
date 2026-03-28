package com.enhancedjobs.jobs.farmer;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.gui.farmer.AutoFarmCropGUI;
import com.enhancedjobs.quests.QuestType;
import com.enhancedjobs.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class FarmerListener implements Listener {

    private final EnhancedJobSystem plugin;

    private static final Set<Material> AGEABLE_CROPS = Set.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES,
        Material.BEETROOTS, Material.NETHER_WART, Material.COCOA,
        Material.SWEET_BERRY_BUSH
    );
    private static final Set<Material> PLANTABLE = Set.of(
        Material.WHEAT_SEEDS, Material.CARROT, Material.POTATO,
        Material.BEETROOT_SEEDS, Material.MELON_SEEDS, Material.PUMPKIN_SEEDS,
        Material.NETHER_WART, Material.COCOA_BEANS, Material.SWEET_BERRIES,
        Material.SUGAR_CANE
    );
    private static final Set<Material> PRODUCE = Set.of(
        Material.MELON_SLICE, Material.APPLE, Material.CARROT
    );
    private static final Set<Material> TILLABLE = Set.of(
        Material.DIRT, Material.GRASS_BLOCK, Material.DIRT_PATH
    );
    private static final Set<Material> ALL_CROP_BLOCKS = Set.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
        Material.NETHER_WART, Material.COCOA, Material.SWEET_BERRY_BUSH,
        Material.PUMPKIN, Material.MELON, Material.SUGAR_CANE,
        Material.RED_MUSHROOM, Material.BROWN_MUSHROOM,
        Material.RED_MUSHROOM_BLOCK, Material.BROWN_MUSHROOM_BLOCK
    );

    public FarmerListener(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PLAYER INTERACT
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Player    player = event.getPlayer();
        ItemStack hand   = player.getInventory().getItemInMainHand();
        Material  type   = clicked.getType();

        // ── Shift+right-click farm chest → crop GUI ───────────────────────────
        if (type == Material.CHEST && player.isSneaking()) {
            var farmCenter = plugin.getAutomaticFarmManager()
                                   .getFarmCenterForChest(clicked.getLocation());
            if (farmCenter != null) {
                event.setCancelled(true);
                AutomaticFarmManager.FarmData data =
                    plugin.getAutomaticFarmManager().getFarmData(farmCenter);
                if (data != null)
                    AutoFarmCropGUI.open(plugin, player, farmCenter, data.crop);
                return;
            }
        }

        // ── Till-dirt quest ───────────────────────────────────────────────────
        if (TILLABLE.contains(type) && ItemUtils.isHoe(hand.getType())) {
            PlayerData d = getDataIfFarmer(player);
            if (d != null)
                plugin.getQuestManager().updateQuestProgress(player, d, FarmerJob.ID, QuestType.TILL_DIRT, 1);
        }

        // ── Fertilizer ────────────────────────────────────────────────────────
        if (ItemUtils.isFertilizer(hand)) {
            event.setCancelled(true);
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
            if (!data.hasJob(FarmerJob.ID)) { player.sendMessage("§cYou must be a Farmer to use this."); return; }
            FarmerRewardManager rm = plugin.getFarmerRewardManager();
            if (rm.hasFertilizerActive(player)) { player.sendMessage("§cFertilizer is already active!"); return; }
            if (rm.isFertilizerOnCooldown(player)) {
                long ms = rm.getFertilizerCooldownRemaining(player);
                player.sendMessage("§cFertilizer on cooldown! §e" + (ms / 3_600_000) + "h " + ((ms % 3_600_000) / 60_000) + "m remaining.");
                return;
            }
            if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
            else player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            rm.activateFertilizer(player);
            return;
        }

        // ── Right-click auto-harvest ──────────────────────────────────────────
        if (AGEABLE_CROPS.contains(type)) {
            PlayerData data = getDataIfFarmer(player);
            if (data == null) return;
            if (!isFullyGrown(clicked)) return;

            event.setCancelled(true);

            Collection<ItemStack> drops = clicked.getDrops(hand);
            Ageable fresh = (Ageable) Bukkit.createBlockData(type);
            fresh.setAge(0);
            clicked.setBlockData(fresh, false);

            for (ItemStack drop : drops) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
                overflow.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            }

            awardPassiveXP(player, data);
            switch (type) {
                case WHEAT       -> updateQuest(player, data, QuestType.HARVEST_WHEAT, 1);
                case CARROTS     -> updateQuest(player, data, QuestType.HARVEST_CARROTS, 1);
                case NETHER_WART -> updateQuest(player, data, QuestType.HARVEST_NETHER_WART, 1);
                default          -> {}
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ANVIL — PrepareAnvilEvent: compute and preview the result
    //
    //  Stacking rules:
    //  HOE + BOOK:
    //    - Unenchanted hoe (level 0) + any book level N  → hoe level N
    //    - Enchanted hoe level N + book level N          → hoe level N+1  (same level only)
    //    - Any other mismatch                            → blocked
    //  BOOK + BOOK: same level only → level+1 (max V)
    //  HOE  + HOE:  same FF level   → level+1 (max V)
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory anvil = event.getInventory();
        ItemStack left  = anvil.getItem(0);
        ItemStack right = anvil.getItem(1);
        if (left == null || right == null) return;

        ItemStack result = computeAnvilResult(left, right);
        if (result == null) return; // not our combination — leave vanilla alone

        if (result.getType() == Material.AIR) {
            // blocked combination
            event.setResult(new ItemStack(Material.AIR));
            anvil.setRepairCost(9999);
        } else {
            event.setResult(result);
            anvil.setRepairCost(Math.max(1, getAnvilCost(left, right)));
        }
    }

    /**
     * Computes the result for our custom anvil combinations.
     * Returns null  = not our combination (let vanilla handle it)
     * Returns AIR   = our combination but it's invalid/blocked
     * Returns item  = valid result
     */
    private ItemStack computeAnvilResult(ItemStack left, ItemStack right) {
        boolean lHoe  = ItemUtils.isHoe(left.getType());
        boolean rHoe  = ItemUtils.isHoe(right.getType());
        boolean lBook = ItemUtils.isFarmingFortuneBook(left);
        boolean rBook = ItemUtils.isFarmingFortuneBook(right);

        // Case 1: HOE + BOOK
        if (lHoe && rBook) {
            int hoeLevel  = ItemUtils.getFarmingFortuneLevel(left);
            int bookLevel = ItemUtils.getFarmingFortuneBookLevel(right);
            int newLevel;
            if (hoeLevel == 0) {
                // Unenchanted hoe: apply book level directly
                newLevel = bookLevel;
            } else if (hoeLevel == bookLevel) {
                // Same level: upgrade by one (e.g. FF II hoe + FF II book = FF III hoe)
                newLevel = hoeLevel + 1;
            } else {
                return new ItemStack(Material.AIR); // mismatch — blocked
            }
            if (newLevel > 5) return new ItemStack(Material.AIR);
            ItemStack result = left.clone();
            ItemUtils.applyFarmingFortune(result, newLevel);
            return result;
        }

        // Case 2: BOOK + BOOK (same level only)
        if (lBook && rBook) {
            int lv = ItemUtils.getFarmingFortuneBookLevel(left);
            int rv = ItemUtils.getFarmingFortuneBookLevel(right);
            if (lv != rv) return new ItemStack(Material.AIR);
            int newLevel = lv + 1;
            if (newLevel > 5) return new ItemStack(Material.AIR);
            return ItemUtils.createFarmingFortuneBook(newLevel);
        }

        // Case 3: HOE + HOE (same FF level → level+1)
        if (lHoe && rHoe) {
            int lv = ItemUtils.getFarmingFortuneLevel(left);
            int rv = ItemUtils.getFarmingFortuneLevel(right);
            if (lv == 0 && rv == 0) return null; // no FF on either — vanilla repair
            if (lv == 0 || rv == 0) return new ItemStack(Material.AIR);
            if (lv != rv) return new ItemStack(Material.AIR);
            int newLevel = lv + 1;
            if (newLevel > 5) return new ItemStack(Material.AIR);
            ItemStack result = left.clone();
            ItemUtils.applyFarmingFortune(result, newLevel);
            return result;
        }

        return null; // not our combination
    }

    private int getAnvilCost(ItemStack left, ItemStack right) {
        boolean lHoe  = ItemUtils.isHoe(left.getType());
        boolean rBook = ItemUtils.isFarmingFortuneBook(right);
        boolean lBook = ItemUtils.isFarmingFortuneBook(left);
        boolean rHoe  = ItemUtils.isHoe(right.getType());

        if (lHoe && rBook) {
            int hoeLevel  = ItemUtils.getFarmingFortuneLevel(left);
            int bookLevel = ItemUtils.getFarmingFortuneBookLevel(right);
            int newLevel  = (hoeLevel == 0) ? bookLevel : hoeLevel + 1;
            return newLevel * 3;
        }
        if (lBook && ItemUtils.isFarmingFortuneBook(right)) {
            return (ItemUtils.getFarmingFortuneBookLevel(left) + 1) * 2;
        }
        if (lHoe && rHoe) {
            return (ItemUtils.getFarmingFortuneLevel(left) + 1) * 4;
        }
        return 3;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ANVIL CLICK — manually transfer item to bypass full-durability bug
    //
    //  Vanilla Minecraft refuses to output anything from the anvil when the
    //  left item has full durability and no vanilla operation applies.
    //  We bypass this by giving the item directly to the player here.
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory anvil)) return;
        if (event.getRawSlot() != 2) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack left  = anvil.getItem(0);
        ItemStack right = anvil.getItem(1);
        if (left == null || right == null) return;

        ItemStack result = computeAnvilResult(left, right);
        if (result == null) return; // not our combo — let vanilla handle

        event.setCancelled(true);

        if (result.getType() == Material.AIR) {
            // blocked combo — show message
            String msg = getBlockedMessage(left, right);
            if (msg != null) player.sendMessage(msg);
            return;
        }

        // Valid combo — give item, consume inputs, deduct XP levels
        int cost = getAnvilCost(left, right);
        if (player.getLevel() < cost) {
            player.sendMessage("§cYou need §e" + cost + " experience levels§c to use the anvil!");
            return;
        }

        // Remove inputs
        anvil.setItem(0, null);
        anvil.setItem(1, null);

        // Deduct XP levels
        player.setLevel(player.getLevel() - cost);

        // Give result
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(result);
        overflow.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));

        player.sendMessage("§b✦ Applied §bFarming Fortune " + ItemUtils.toRoman(
            ItemUtils.getFarmingFortuneLevel(result) > 0
                ? ItemUtils.getFarmingFortuneLevel(result)
                : ItemUtils.getFarmingFortuneBookLevel(result)
        ) + "§b!");
    }

    private String getBlockedMessage(ItemStack left, ItemStack right) {
        boolean lHoe  = ItemUtils.isHoe(left.getType());
        boolean rBook = ItemUtils.isFarmingFortuneBook(right);
        boolean lBook = ItemUtils.isFarmingFortuneBook(left);
        boolean rHoe  = ItemUtils.isHoe(right.getType());

        if (lHoe && rBook) {
            int h = ItemUtils.getFarmingFortuneLevel(left);
            int b = ItemUtils.getFarmingFortuneBookLevel(right);
            int result = (h == 0) ? b : h + 1;
            if (result > 5) return "§cMax is §bFarming Fortune V§c!";
            return "§cBook level must match the hoe's current §bFarming Fortune§c level!";
        }
        if (lBook && rBook) {
            int l = ItemUtils.getFarmingFortuneBookLevel(left);
            int r = ItemUtils.getFarmingFortuneBookLevel(right);
            if (l != r) return "§cCombine two §bFarming Fortune §cbooks of the same level!";
            return "§cMax is §bFarming Fortune V§c!";
        }
        if (lHoe && rHoe) {
            int l = ItemUtils.getFarmingFortuneLevel(left);
            int r = ItemUtils.getFarmingFortuneLevel(right);
            if (l != r) return "§cBoth hoes must have the same §bFarming Fortune§c level!";
            return "§cMax is §bFarming Fortune V§c!";
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BLOCK BREAK
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player     player = event.getPlayer();
        PlayerData data   = getDataIfFarmer(player);
        if (data == null) return;

        Block    block = event.getBlock();
        Material type  = block.getType();

        if (AGEABLE_CROPS.contains(type)) {
            if (!isFullyGrown(block)) return;
            awardPassiveXP(player, data);
            switch (type) {
                case WHEAT       -> updateQuest(player, data, QuestType.HARVEST_WHEAT, 1);
                case CARROTS     -> updateQuest(player, data, QuestType.HARVEST_CARROTS, 1);
                case NETHER_WART -> updateQuest(player, data, QuestType.HARVEST_NETHER_WART, 1);
                default          -> {}
            }
            if (isWearingFarmersHat(player)) {
                final Material cropType = type;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (block.getRelative(0, -1, 0).getType() == Material.FARMLAND) {
                        Ageable newData = (Ageable) Bukkit.createBlockData(cropType);
                        newData.setAge(0);
                        block.setBlockData(newData, false);
                    }
                }, 2L);
            }
            return;
        }

        switch (type) {
            case PUMPKIN    -> { awardPassiveXP(player, data); updateQuest(player, data, QuestType.HARVEST_PUMPKIN, 1); }
            case MELON      -> { awardPassiveXP(player, data); updateQuest(player, data, QuestType.HARVEST_MELON, 1); }
            case SUGAR_CANE -> { awardPassiveXP(player, data); updateQuest(player, data, QuestType.HARVEST_SUGARCANE, 1); }
            default -> {
                if (type == Material.RED_MUSHROOM || type == Material.BROWN_MUSHROOM
                 || type == Material.RED_MUSHROOM_BLOCK || type == Material.BROWN_MUSHROOM_BLOCK) {
                    awardPassiveXP(player, data);
                    updateQuest(player, data, QuestType.COLLECT_MUSHROOMS, 1);
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BLOCK DROP
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (!ALL_CROP_BLOCKS.contains(event.getBlock().getType())) return;

        FarmerRewardManager rm           = plugin.getFarmerRewardManager();
        boolean             fertilizer   = rm.hasFertilizerActive(player);
        int                 fortuneLevel = ItemUtils.getFarmingFortuneLevel(player.getInventory().getItemInMainHand());
        boolean             fortune      = fortuneLevel > 0 && Math.random() < fortuneLevel * 0.20;

        if (!fertilizer && !fortune) return;

        int multiplier = (fertilizer ? 2 : 1) * (fortune ? 2 : 1);
        for (org.bukkit.entity.Item dropped : event.getItems()) {
            ItemStack stack = dropped.getItemStack();
            stack.setAmount(Math.min(stack.getAmount() * multiplier, stack.getMaxStackSize()));
            dropped.setItemStack(stack);
        }

        if (fertilizer && fortune)
            player.sendMessage("§a✦ Fertilizer §7+ §bFarming Fortune " + ItemUtils.toRoman(fortuneLevel) + " §7both triggered! Drops ×" + multiplier);
        else if (fertilizer)
            player.sendMessage("§a✦ Advanced Fertilizer §7doubled your drops!");
        else
            player.sendMessage("§b✦ Farming Fortune " + ItemUtils.toRoman(fortuneLevel) + " §7triggered! Drops doubled.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REMAINING EVENTS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        PlayerData data = getDataIfFarmer(event.getPlayer());
        if (data == null) return;
        if (PLANTABLE.contains(event.getBlockPlaced().getType()))
            updateQuest(event.getPlayer(), data, QuestType.PLANT_CROPS, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;
        PlayerData data = getDataIfFarmer(player);
        if (data == null) return;
        if (event.getMother() instanceof Cow || event.getMother() instanceof MushroomCow) {
            updateQuest(player, data, QuestType.BREED_COWS, 1);
            checkFarmingFortuneBaby(player, event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupItem(PlayerPickupItemEvent event) {
        PlayerData data = getDataIfFarmer(event.getPlayer());
        if (data == null) return;
        ItemStack stack = event.getItem().getItemStack();
        if (stack.getType() == Material.EGG)
            updateQuest(event.getPlayer(), data, QuestType.COLLECT_EGGS, stack.getAmount());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        PlayerData data = getDataIfFarmer(event.getPlayer());
        if (data == null) return;
        if (!(event.getRightClicked() instanceof Cow)) return;
        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.BUCKET) return;
        updateQuest(event.getPlayer(), data, QuestType.MILK_COWS, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        PlayerData data = getDataIfFarmer(event.getPlayer());
        if (data == null) return;
        if (PRODUCE.contains(event.getItem().getType()))
            updateQuest(event.getPlayer(), data, QuestType.EAT_PRODUCE, 1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private PlayerData getDataIfFarmer(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        return data.hasJob(FarmerJob.ID) ? data : null;
    }

    private void awardPassiveXP(Player player, PlayerData data) {
        data.addJobXP(FarmerJob.ID, plugin.getConfig().getDouble("farmer-passive-xp-per-crop", 0.2));
        plugin.getJobManager().checkLevelUp(player, data, FarmerJob.ID);
    }

    private void updateQuest(Player player, PlayerData data, QuestType type, int amount) {
        plugin.getQuestManager().updateQuestProgress(player, data, FarmerJob.ID, type, amount);
    }

    private boolean isFullyGrown(Block block) {
        if (block.getBlockData() instanceof Ageable ageable)
            return ageable.getAge() == ageable.getMaximumAge();
        return true;
    }

    private boolean isWearingFarmersHat(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        return helmet != null && ItemUtils.isFarmersHat(helmet);
    }

    private void checkFarmingFortuneBaby(Player player, EntityBreedEvent event) {
        int fortuneLevel = ItemUtils.getFarmingFortuneLevel(player.getInventory().getItemInMainHand());
        if (fortuneLevel == 0) return;
        if (Math.random() < fortuneLevel * 0.20) {
            Entity baby = event.getMother().getWorld()
                .spawnEntity(event.getMother().getLocation(), event.getMother().getType());
            if (baby instanceof org.bukkit.entity.Ageable a) a.setBaby();
            player.sendMessage("§b✦ Farming Fortune " + ItemUtils.toRoman(fortuneLevel) + " triggered! An extra baby animal was born.");
        }
    }
}
