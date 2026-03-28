package com.enhancedjobs.gui;

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

public class MainJobsGUI {

    public static final String TITLE = "§8§l✦ §r§6Jobs §8§l✦";

    private MainJobsGUI() {}

    public static void open(EnhancedJobSystem plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        fillBorder(inv);

        // ── Farmer slot (slot 13 – centre of 27-slot inventory) ───────────────
        PlayerData data  = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        boolean enrolled = data.hasJob(FarmerJob.ID);
        int     level    = data.getJobLevel(FarmerJob.ID);
        double  xp       = data.getJobXP(FarmerJob.ID);

        ItemStack farmerItem = buildJobItem(
            Material.WHEAT,
            "§2§l🌾 Farmer",
            enrolled, level, xp
        );
        inv.setItem(13, farmerItem);

        player.openInventory(inv);
    }

    private static ItemStack buildJobItem(Material mat, String name,
                                          boolean enrolled, int level, double xp) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(name);

        String status = enrolled ? "§a● Enrolled" : "§c● Not enrolled";
        meta.setLore(List.of(
            status,
            "§7Level: §e"    + level + "§7/10",
            XPUtils.buildXPBar(level, xp),
            "",
            "§eClick §7to open job details."
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack glass = borderGlass();
        for (int i = 0; i < 27; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 2 || col == 0 || col == 8)
                inv.setItem(i, glass);
        }
    }

    private static ItemStack borderGlass() {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m  = g.getItemMeta();
        m.setDisplayName("§r");
        g.setItemMeta(m);
        return g;
    }
}
