package com.enhancedjobs.gui.farmer;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.jobs.farmer.FarmerJob;
import com.enhancedjobs.jobs.farmer.FarmerRewardManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FarmerRewardsGUI {

    public static final String TITLE = "§8§l✦ §r§6Farmer Rewards §8§l✦";

    private FarmerRewardsGUI() {}

    public static void open(EnhancedJobSystem plugin, Player player) {
        Inventory  inv  = Bukkit.createInventory(null, 54, TITLE);
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int level = data.getJobLevel(FarmerJob.ID);

        fillBorder(inv);

        // ── Level 2: Advanced Fertilizer (slot 10) ────────────────────────────
        inv.setItem(10, buildRewardItem(
            Material.BONE_MEAL, "§a✦ Advanced Fertilizer", level >= 2,
            false,
            List.of("§7Doubles crop drops for §e10 min§7.",
                    "§73-hour cooldown.",
                    "§7Right-click to consume.",
                    "§7Cost: §e300 gold coins",
                    "§8Unlocks at Level 2"),
            "§eClick to purchase!"
        ));

        // ── Level 4: Farming Fortune Book (slot 19) ───────────────────────────
        inv.setItem(19, buildRewardItem(
            Material.ENCHANTED_BOOK, "§b✦ Farming Fortune Book", level >= 4,
            false,
            List.of("§7Custom hoe enchantment.",
                    "§7Each level = §e+20% §7chance to double crop drops.",
                    "§7Apply to a hoe §7via §eanvil§7.",
                    "§7Stacks up to §bFarming Fortune V§7.",
                    "§7Cost: §e250 gold coins §7per book",
                    "§8Unlocks at Level 4"),
            "§eClick to purchase!"
        ));

        // ── Level 6: Farmer's Hat (slot 28) ───────────────────────────────────
        boolean claimed6 = data.hasClaimedReward(FarmerJob.ID, FarmerRewardManager.REWARD_FARMERS_HAT);
        inv.setItem(28, buildRewardItem(
            Material.LEATHER_HELMET, "§2✦ Farmer's Hat", level >= 6,
            claimed6,
            List.of("§7Auto-replants crops on harvest.",
                    "§7§lOne-time claim only.",
                    "§8Unlocks at Level 6"),
            claimed6 ? "§cAlready claimed." : "§eClick to claim!"
        ));

        // ── Level 8: Automatic Farm (slot 37) ─────────────────────────────────
        inv.setItem(37, buildRewardItem(
            Material.HAY_BLOCK, "§e✦ Automatic Farm", level >= 8,
            false,
            List.of("§73×3 auto-harvesting farm block.",
                    "§7Stores crops in a nearby chest.",
                    "§7Cost: §e100 gold coins",
                    "§8Unlocks at Level 8"),
            "§eClick to purchase!"
        ));

        // ── Level 10: Money Tree Seeds (slot 46) ──────────────────────────────
        boolean claimed10 = data.hasClaimedReward(FarmerJob.ID, FarmerRewardManager.REWARD_MONEY_TREE);
        inv.setItem(46, buildRewardItem(
            Material.OAK_SAPLING, "§6✦ Money Tree Seeds ×5", level >= 10,
            claimed10,
            List.of("§7Drops §e5-15 gold coins §7when harvested.",
                    "§7Grows in §e15 hours§7.",
                    "§7Returns §e1-4 seeds §7on harvest.",
                    "§7§lOne-time claim only.",
                    "§8Unlocks at Level 10"),
            claimed10 ? "§cAlready claimed." : "§eClick to claim!"
        ));

        inv.setItem(49, FarmerGUI.makeButton(Material.ARROW, "§7← Back",
            List.of("§7Return to Farmer menu.")));

        player.openInventory(inv);
    }

    private static ItemStack buildRewardItem(Material mat, String name, boolean unlocked,
                                              boolean claimed, List<String> lore, String action) {
        ItemStack item = new ItemStack(unlocked ? mat : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta = item.getItemMeta();

        if (!unlocked) {
            meta.setDisplayName("§8§l🔒 Locked");
            meta.setLore(new ArrayList<>(lore));
        } else {
            meta.setDisplayName(claimed ? "§7" + name.replaceAll("§[0-9a-fk-or]", "") + " §a✔" : name);
            List<String> full = new ArrayList<>(lore);
            full.add(action);
            meta.setLore(full);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack g = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta  m = g.getItemMeta(); m.setDisplayName("§r"); g.setItemMeta(m);
        for (int i = 0; i < 54; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inv.setItem(i, g);
        }
    }
}
