package com.enhancedjobs.gui;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.gui.farmer.*;
import com.enhancedjobs.jobs.farmer.AutomaticFarmManager;
import com.enhancedjobs.jobs.farmer.AutomaticFarmManager.CropChoice;
import com.enhancedjobs.jobs.farmer.FarmerJob;
import com.enhancedjobs.jobs.farmer.FarmerRewardManager;
import com.enhancedjobs.quests.QuestType;
import com.enhancedjobs.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final EnhancedJobSystem plugin;

    public GUIListener(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        // ── Custom holder-based GUIs (no title matching needed) ───────────────
        if (event.getInventory().getHolder() instanceof AutoFarmCropGUI cropGui) {
            event.setCancelled(true);
            handleAutoFarmCrop(event, player, cropGui);
            return;
        }
        if (event.getInventory().getHolder() instanceof JobsAdminGUI) {
            event.setCancelled(true);
            handleJobsAdmin(event, player);
            return;
        }

        // ── Title-based GUIs ──────────────────────────────────────────────────
        String title = event.getView().getTitle();
        event.setCancelled(true);

        if      (title.equals(MainJobsGUI.TITLE))          handleMainJobs(event, player);
        else if (title.equals(FarmerGUI.TITLE))            handleFarmerMain(event, player);
        else if (title.equals(FarmerQuestsGUI.TITLE))      handleFarmerQuests(event, player);
        else if (title.equals(FarmerRewardsGUI.TITLE))     handleFarmerRewards(event, player);
        else if (title.equals(FarmerVendorGUI.TITLE))      handleFarmerVendorBuy(event, player);
        else if (title.equals(FarmerVendorGUI.TITLE_SELL)) handleFarmerVendorSell(event, player);
        else event.setCancelled(false);
    }

    // ── Main Jobs GUI ─────────────────────────────────────────────────────────

    private void handleMainJobs(InventoryClickEvent e, Player player) {
        if (e.getSlot() == 13) FarmerGUI.open(plugin, player);
    }

    // ── Farmer Main GUI ───────────────────────────────────────────────────────

    private void handleFarmerMain(InventoryClickEvent e, Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        switch (e.getSlot()) {
            case 19 -> {
                if (!data.hasJob(FarmerJob.ID)) {
                    data.joinJob(FarmerJob.ID);
                    player.sendMessage("§aYou joined the §2Farmer §ajob!");
                } else {
                    data.leaveJob(FarmerJob.ID);
                    player.sendMessage("§cYou left the §2Farmer §cjob.");
                }
                FarmerGUI.open(plugin, player);
            }
            case 22 -> {
                if (!data.hasJob(FarmerJob.ID)) { player.sendMessage("§cJoin the Farmer job first!"); return; }
                FarmerQuestsGUI.open(plugin, player);
            }
            case 25 -> {
                if (!data.hasJob(FarmerJob.ID)) { player.sendMessage("§cJoin the Farmer job first!"); return; }
                FarmerRewardsGUI.open(plugin, player);
            }
            case 31 -> {
                if (!data.hasJob(FarmerJob.ID)) { player.sendMessage("§cJoin the Farmer job first!"); return; }
                FarmerVendorGUI.open(plugin, player);
            }
            case 49 -> MainJobsGUI.open(plugin, player);
        }
    }

    // ── Farmer Quests GUI ─────────────────────────────────────────────────────

    private void handleFarmerQuests(InventoryClickEvent e, Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (e.getSlot() == 31) {
            plugin.getQuestManager().purchaseQuest(player, data, FarmerJob.ID);
            FarmerQuestsGUI.open(plugin, player);
        } else if (e.getSlot() == 49) {
            FarmerGUI.open(plugin, player);
        }
    }

    // ── Farmer Rewards GUI ────────────────────────────────────────────────────

    private void handleFarmerRewards(InventoryClickEvent e, Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        FarmerRewardManager rm = plugin.getFarmerRewardManager();
        switch (e.getSlot()) {
            case 10 -> { rm.buyFertilizer(player, data);         FarmerRewardsGUI.open(plugin, player); }
            case 19 -> { rm.buyFarmingFortuneBook(player, data); FarmerRewardsGUI.open(plugin, player); }
            case 28 -> { rm.claimFarmersHat(player, data);       FarmerRewardsGUI.open(plugin, player); }
            case 37 -> { rm.buyAutoFarm(player, data);           FarmerRewardsGUI.open(plugin, player); }
            case 46 -> { rm.claimMoneyTreeSeeds(player, data);   FarmerRewardsGUI.open(plugin, player); }
            case 49 -> FarmerGUI.open(plugin, player);
        }
    }

    // ── Farmer Vendor BUY GUI ─────────────────────────────────────────────────

    private void handleFarmerVendorBuy(InventoryClickEvent e, Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int slot = e.getSlot();

        if (slot == 49) { FarmerGUI.open(plugin, player); return; }
        if (slot == 40) { FarmerVendorGUI.openSell(plugin, player); return; }

        int[] itemSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25};
        for (int i = 0; i < itemSlots.length; i++) {
            if (slot == itemSlots[i] && i < FarmerVendorGUI.ITEMS.length) {
                FarmerVendorGUI.VendorItem vi = FarmerVendorGUI.ITEMS[i];
                if (!data.removeGold(vi.goldCost())) {
                    player.sendMessage("§cNot enough gold! You need §e" + vi.goldCost() + " coin(s).");
                } else {
                    player.getInventory().addItem(new ItemStack(vi.material(), vi.amount()));
                    player.sendMessage("§aPurchased §e" + vi.name().replaceAll("§.", "") + " §afor §e" + vi.goldCost() + " gold coin(s)§a!");
                    FarmerVendorGUI.open(plugin, player);
                }
                return;
            }
        }
    }

    // ── Farmer Vendor SELL GUI ────────────────────────────────────────────────

    private void handleFarmerVendorSell(InventoryClickEvent e, Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int slot = e.getSlot();

        if (slot == 49) { FarmerGUI.open(plugin, player); return; }
        if (slot == 40) { FarmerVendorGUI.open(plugin, player); return; }

        if (slot == 31) {
            double totalEarned = 0;
            int    totalSold   = 0;
            for (FarmerVendorGUI.SellItem si : FarmerVendorGUI.SELL_ITEMS) {
                int count = FarmerVendorGUI.countInInventory(player, si.material());
                if (count > 0) {
                    double earned = Math.round(count * si.goldPerItem() * 10.0) / 10.0;
                    FarmerVendorGUI.removeFromInventory(player, si.material(), count);
                    data.addGold(earned);
                    totalEarned += earned;
                    totalSold   += count;
                    plugin.getQuestManager().updateQuestProgress(player, data, FarmerJob.ID, QuestType.SELL_CROPS, count);
                }
            }
            plugin.getJobManager().checkLevelUp(player, data, FarmerJob.ID);
            if (totalSold > 0) player.sendMessage("§a✔ Sold §e" + totalSold + " §acrop(s) for §e" + String.format("%.1f", totalEarned) + " §agold coins!");
            else               player.sendMessage("§cYou have no crops to sell.");
            FarmerVendorGUI.openSell(plugin, player);
            return;
        }

        int[] sellSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25};
        for (int i = 0; i < sellSlots.length; i++) {
            if (slot == sellSlots[i] && i < FarmerVendorGUI.SELL_ITEMS.length) {
                FarmerVendorGUI.SellItem si = FarmerVendorGUI.SELL_ITEMS[i];
                int count = FarmerVendorGUI.countInInventory(player, si.material());
                if (count == 0) {
                    player.sendMessage("§cYou don't have any §e" + si.name().replaceAll("§.", "") + "§c to sell.");
                } else {
                    double earned = Math.round(count * si.goldPerItem() * 10.0) / 10.0;
                    FarmerVendorGUI.removeFromInventory(player, si.material(), count);
                    data.addGold(earned);
                    player.sendMessage("§a✔ Sold §e" + count + "x " + si.name().replaceAll("§.", "") + " §afor §e" + earned + " §agold coins!");
                    plugin.getQuestManager().updateQuestProgress(player, data, FarmerJob.ID, QuestType.SELL_CROPS, count);
                    plugin.getJobManager().checkLevelUp(player, data, FarmerJob.ID);
                    FarmerVendorGUI.openSell(plugin, player);
                }
                return;
            }
        }
    }

    // ── Auto-Farm Crop Selection GUI ──────────────────────────────────────────

    private void handleAutoFarmCrop(InventoryClickEvent e, Player player, AutoFarmCropGUI gui) {
        int slot = e.getSlot();

        // Close button
        if (slot == 22) { player.closeInventory(); return; }

        // Crop slots 10–14 map to CropChoice.values() in order
        int[] cropSlots = { 10, 11, 12, 13, 14 };
        CropChoice[] choices = CropChoice.values();
        for (int i = 0; i < cropSlots.length && i < choices.length; i++) {
            if (slot == cropSlots[i]) {
                plugin.getAutomaticFarmManager().setCropType(gui.getFarmCenter(), choices[i], player);
                // Reopen with updated selection
                AutoFarmCropGUI.open(plugin, player, gui.getFarmCenter(), choices[i]);
                return;
            }
        }
    }

    // ── JobsAdmin GUI ─────────────────────────────────────────────────────────

    private void handleJobsAdmin(InventoryClickEvent e, Player player) {
        int slot = e.getSlot();

        // Close button (slot 49)
        if (slot == 49) { player.closeInventory(); return; }

        // Map slot → item to give
        ItemStack toGive = switch (slot) {
            case 10 -> ItemUtils.createFarmingFortuneBook(1);
            case 11 -> ItemUtils.createFarmingFortuneBook(2);
            case 12 -> ItemUtils.createFarmingFortuneBook(3);
            case 13 -> ItemUtils.createFarmingFortuneBook(4);
            case 14 -> ItemUtils.createFarmingFortuneBook(5);
            case 19 -> ItemUtils.createFertilizer();
            case 20 -> ItemUtils.createFarmersHat();
            case 21 -> ItemUtils.createAutoFarmBlock();
            case 22 -> ItemUtils.createMoneyTreeSeed(1).get(0);
            default -> null;
        };

        if (toGive == null) return;

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), toGive);
            player.sendMessage("§e[JobsAdmin] §fInventory full — item dropped at your feet.");
        } else {
            player.getInventory().addItem(toGive);
            player.sendMessage("§e[JobsAdmin] §fItem given: " + toGive.getItemMeta().getDisplayName());
        }
    }
}
