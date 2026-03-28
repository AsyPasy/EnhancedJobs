package com.enhancedjobs.gui;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Admin GUI for receiving any custom plugin item.
 * Opened via /jobsadmin. Requires enhancedjobs.admin permission.
 */
public class JobsAdminGUI implements InventoryHolder {

    public static final String TITLE = "§8§l✦ §r§cJobsAdmin – Custom Items §8§l✦";

    private Inventory inventory;

    @Override
    public Inventory getInventory() { return inventory; }

    public static void open(EnhancedJobSystem plugin, Player player) {
        JobsAdminGUI holder = new JobsAdminGUI();
        Inventory inv = Bukkit.createInventory(holder, 54, TITLE);
        holder.inventory = inv;

        // Border
        ItemStack border = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta(); bm.setDisplayName("§r"); border.setItemMeta(bm);
        for (int i = 0; i < 54; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inv.setItem(i, border);
        }

        // ── Row 1: Enchantment books (slots 10–16) ────────────────────────────
        inv.setItem(10, labeled(ItemUtils.createFarmingFortuneBook(1), "§b✦ Farming Fortune I",   "§7Give yourself this book."));
        inv.setItem(11, labeled(ItemUtils.createFarmingFortuneBook(2), "§b✦ Farming Fortune II",  "§7Give yourself this book."));
        inv.setItem(12, labeled(ItemUtils.createFarmingFortuneBook(3), "§b✦ Farming Fortune III", "§7Give yourself this book."));
        inv.setItem(13, labeled(ItemUtils.createFarmingFortuneBook(4), "§b✦ Farming Fortune IV",  "§7Give yourself this book."));
        inv.setItem(14, labeled(ItemUtils.createFarmingFortuneBook(5), "§b✦ Farming Fortune V",   "§7Give yourself this book."));

        // ── Row 2: Consumables & wearables (slots 19–25) ─────────────────────
        inv.setItem(19, labeled(ItemUtils.createFertilizer(),     "§a✦ Advanced Fertilizer", "§7Give yourself this item."));
        inv.setItem(20, labeled(ItemUtils.createFarmersHat(),     "§2✦ Farmer's Hat",        "§7Give yourself this item."));
        inv.setItem(21, labeled(ItemUtils.createAutoFarmBlock(),  "§e✦ Automatic Farm",      "§7Give yourself this item."));
        inv.setItem(22, labeled(ItemUtils.createMoneyTreeSeed(1).get(0), "§6✦ Money Tree Seed", "§7Give yourself this item."));

        // ── Info panel (slot 4 top center) ────────────────────────────────────
        ItemStack info = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta  im   = info.getItemMeta();
        im.setDisplayName("§c§lAdmin Item Panel");
        im.setLore(List.of(
            "§7Click any item to receive it.",
            "§7Items drop on the ground if",
            "§7your inventory is full.",
            "",
            "§cFor admins only."
        ));
        info.setItemMeta(im);
        inv.setItem(4, info);

        // Close button (slot 49)
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta  cm    = close.getItemMeta();
        cm.setDisplayName("§cClose");
        close.setItemMeta(cm);
        inv.setItem(49, close);

        player.openInventory(inv);
    }

    /** Appends a "§eClick to receive" line to an existing item's lore. */
    private static ItemStack labeled(ItemStack item, String overrideName, String hint) {
        ItemMeta meta = item.getItemMeta();
        if (overrideName != null) meta.setDisplayName(overrideName);
        java.util.List<String> lore = meta.getLore() != null
            ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
        lore.add("");
        lore.add("§e" + hint);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
