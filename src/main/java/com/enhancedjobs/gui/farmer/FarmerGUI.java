package com.enhancedjobs.gui.farmer;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.jobs.farmer.FarmerJob;
import com.enhancedjobs.utils.XPUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class FarmerGUI {

    public static final String TITLE = "§8§l✦ §r§2Farmer §8§l✦";

    private FarmerGUI() {}

    public static void open(EnhancedJobSystem plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBorder(inv);

        PlayerData data     = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        boolean    enrolled = data.hasJob(FarmerJob.ID);
        int        level    = data.getJobLevel(FarmerJob.ID);
        double     xp       = data.getJobXP(FarmerJob.ID);
        double     gold     = data.getGold();

        // ── Row 1: Stats header ────────────────────────────────────────────────

        // Job info (slot 4)
        ItemStack info = new ItemStack(Material.WHEAT);
        ItemMeta  im   = info.getItemMeta();
        im.setDisplayName("§2§l🌾 Farmer");
        im.setLore(List.of(
            "§7Level: §e" + level + "§7/10",
            XPUtils.buildXPBar(level, xp),
            "§7Gold: §e" + String.format("%.1f", gold),
            "",
            enrolled ? "§aYou are enrolled in this job." : "§cYou are not enrolled."
        ));
        info.setItemMeta(im);
        inv.setItem(4, info);

        // ── Row 2: Action buttons ─────────────────────────────────────────────

        // Join/Leave (slot 19)
        ItemStack joinLeave;
        if (!enrolled) {
            joinLeave = makeButton(Material.LIME_DYE, "§a§lJoin Farmer",
                List.of("§7Click to become a Farmer!"));
        } else {
            joinLeave = makeButton(Material.RED_DYE, "§c§lLeave Farmer",
                List.of("§7Click to leave this job.", "§cWarning: Progress is kept."));
        }
        inv.setItem(19, joinLeave);

        // Quests (slot 22)
        ItemStack quests = makeButton(Material.BOOK, "§e§lQuests",
            List.of("§7View and manage your quests."));
        inv.setItem(22, quests);

        // Rewards (slot 25)
        ItemStack rewards = makeButton(Material.CHEST, "§6§lRewards & Shop",
            List.of("§7View unlockable rewards", "§7and the Farmer vendor."));
        inv.setItem(25, rewards);

        // Vendor (slot 31)
        ItemStack vendor = makeButton(Material.EMERALD, "§b§lFarmer Vendor",
            List.of("§7Browse the Farmer's item shop."));
        inv.setItem(31, vendor);

        // Back (slot 49)
        ItemStack back = makeButton(Material.ARROW, "§7← Back",
            List.of("§7Return to job selection."));
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    public static ItemStack makeButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack glass = borderGlass();
        for (int i = 0; i < 54; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8)
                inv.setItem(i, glass);
        }
    }

    private static ItemStack borderGlass() {
        ItemStack g = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta  m = g.getItemMeta();
        m.setDisplayName("§r");
        g.setItemMeta(m);
        return g;
    }
}
