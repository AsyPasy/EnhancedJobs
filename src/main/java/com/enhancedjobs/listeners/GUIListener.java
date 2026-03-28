package com.enhancedjobs.listeners;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.gui.*;
import com.enhancedjobs.jobs.JobType;
import com.enhancedjobs.quests.QuestTaskType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Central GUI click router.
 *
 * Identifies which GUI is open by matching the inventory title prefix,
 * then delegates click handling to the appropriate logic.
 *
 * All GUIs are server-owned (null holder), so we match by title string.
 */
public class GUIListener implements Listener {

    private final EnhancedJobSystem plugin;

    public GUIListener(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.equals(JobSelectionGUI.TITLE)) {
            event.setCancelled(true);
            handleJobSelection(player, event.getRawSlot(), event.isShiftClick());
        } else if (title.startsWith(JobInfoGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
            handleJobInfo(player, title, event.getRawSlot(), event.isShiftClick());
        } else if (title.startsWith(QuestGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
            handleQuestGUI(player, title, event.getRawSlot());
        } else if (title.startsWith(RewardsGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
            handleRewardsGUI(player, title, event.getRawSlot());
        } else if (title.equals(VendorGUI.TITLE)) {
            handleVendorGUI(player, event);
        }
    }

    // ── Job Selection GUI ─────────────────────────────────────────────────────

    private void handleJobSelection(Player player, int slot, boolean shiftClick) {
        if (slot < 0 || slot > 26) return;
        if (isGlassSlot(slot, 27)) return;

        // Map slot to JobType index
        JobType[] jobs  = JobType.values();
        int[] slots     = buildCentredSlots(jobs.length);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != slot) continue;
            JobType job = jobs[i];
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

            if (!data.hasJob(job.name())) {
                plugin.getJobManager().joinJob(player, job);
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> new JobInfoGUI(plugin, job).open(player), 1L);
            } else if (shiftClick) {
                plugin.getJobManager().leaveJob(player, job);
                player.closeInventory();
            } else {
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> new JobInfoGUI(plugin, job).open(player), 1L);
            }
            return;
        }
    }

    // ── Job Info GUI ──────────────────────────────────────────────────────────

    private void handleJobInfo(Player player, String title, int slot, boolean shiftClick) {
        JobType job = resolveJobFromTitle(title, JobInfoGUI.TITLE_PREFIX);
        if (job == null) return;

        switch (slot) {
            case 11 -> { // Quests
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> new QuestGUI(plugin, job).open(player), 1L);
            }
            case 15 -> { // Rewards
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> new RewardsGUI(plugin, job).open(player), 1L);
            }
            case 22 -> { // Leave (shift-click required)
                if (shiftClick) {
                    plugin.getJobManager().leaveJob(player, job);
                    player.closeInventory();
                    plugin.getServer().getScheduler().runTaskLater(plugin,
                            () -> new JobSelectionGUI(plugin).open(player), 1L);
                } else {
                    player.sendMessage("§c[Jobs] Shift-click to confirm leaving the " + job.getDisplayName() + " job.");
                }
            }
            case 18 -> { // Back
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> new JobSelectionGUI(plugin).open(player), 1L);
            }
        }
    }

    // ── Quest GUI ─────────────────────────────────────────────────────────────

    private void handleQuestGUI(Player player, String title, int slot) {
        JobType job = resolveJobFromTitle(title, QuestGUI.TITLE_PREFIX);
        if (job == null) return;

        if (slot == 45) { // Back
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> new JobInfoGUI(plugin, job).open(player), 1L);
        }
        // Quest items themselves are informational only
    }

    // ── Rewards GUI ───────────────────────────────────────────────────────────

    private void handleRewardsGUI(Player player, String title, int slot) {
        JobType job = resolveJobFromTitle(title, RewardsGUI.TITLE_PREFIX);
        if (job == null) return;

        if (slot == 45) { // Back
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> new JobInfoGUI(plugin, job).open(player), 1L);
            return;
        }

        if (job == JobType.FARMER) {
            var fr = plugin.getRewardManager().getFarmerRewards();
            switch (slot) {
                case 13 -> fr.claimHoe(player);   // Level 4 hoe
                case 16 -> fr.claimHat(player);   // Level 6 hat
                case 28 -> fr.buyAutoFarm(player); // Level 8 auto farm
                // 10 (boots) and 34 (seeds) are auto-granted, not clickable
            }
            // Refresh GUI
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> new RewardsGUI(plugin, job).open(player), 1L);
        }
    }

    // ── Vendor GUI ────────────────────────────────────────────────────────────

    private void handleVendorGUI(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();

        // Only handle clicks inside the vendor inventory (not player inv)
        if (slot < 0 || slot >= 54) return;
        event.setCancelled(true);

        if (slot == 53) { // Close
            player.closeInventory();
            return;
        }

        if (slot == 49) { // Confirm – sell all crops in top 36 slots
            confirmSale(player, event);
            return;
        }

        if (slot == 45) { // Select all – no persistent selection needed; confirm sells everything
            player.sendMessage("§7[Vendor] Click §aConfirm §7to sell all displayed crops.");
            return;
        }
        // Clicks on individual crop items are informational (prices shown in lore)
    }

    /** Sells every crop stack currently displayed in the vendor GUI (slots 0–35). */
    private void confirmSale(Player player, InventoryClickEvent event) {
        var topInv = event.getView().getTopInventory();
        VendorGUI vendorGUI = new VendorGUI(plugin);

        double totalEarned = 0;
        int totalItems     = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack item = topInv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (!VendorGUI.CROP_MATERIALS.contains(item.getType())) continue;

            double price = vendorGUI.getCropPrice(item.getType());
            totalEarned += price * item.getAmount();
            totalItems  += item.getAmount();

            // Remove from player's actual inventory
            player.getInventory().removeItem(new ItemStack(item.getType(), item.getAmount()));
        }

        if (totalItems == 0) {
            player.sendMessage("§c[Vendor] No crops to sell!");
            return;
        }

        // Pay the player
        plugin.getEconomyHook().deposit(player, totalEarned);
        player.sendMessage("§2§l[Vendor] §r§aSold §e" + totalItems + " crops §afor §e"
                + String.format("%.1f", totalEarned) + " gold coins§a!");

        // Track quest progress for SELL_CROPS
        plugin.getQuestManager().addProgress(player, "FARMER", QuestTaskType.SELL_CROPS, totalItems);

        player.closeInventory();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Extracts the JobType from a GUI title like "§6§lQuests: §r§eFarmer".
     * @param title  full inventory title
     * @param prefix the known prefix to strip
     */
    private JobType resolveJobFromTitle(String title, String prefix) {
        String raw = title.replace(prefix, "").trim();
        // Strip all colour codes
        raw = raw.replaceAll("§.", "").trim();
        return JobType.fromString(raw);
    }

    private int[] buildCentredSlots(int count) {
        int[] available = {10, 11, 12, 13, 14, 15, 16};
        if (count >= available.length) return available;
        int start = (available.length - count) / 2;
        int[] result = new int[count];
        for (int i = 0; i < count; i++) result[i] = available[start + i];
        return result;
    }

    private boolean isGlassSlot(int slot, int invSize) {
        int rows = invSize / 9;
        int row  = slot / 9;
        int col  = slot % 9;
        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }
}
