package com.enhancedjobs.gui;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.jobs.JobType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewards GUI for a specific job. Shows all milestone rewards (every even level)
 * with their current state: locked / unlocked-claimable / already-claimed / buyable.
 *
 * Clicking a claimable or buyable reward triggers the claim/purchase.
 *
 * Layout (54 slots):
 *   Border on all edges. Rewards at slots 10, 16, 28, 34, 49 (spread across rows 1–4).
 *   Back button at slot 45.
 */
public class RewardsGUI {

    public static final String TITLE_PREFIX = "§d§lRewards: §r§5";

    private final EnhancedJobSystem plugin;
    private final JobType job;

    public RewardsGUI(EnhancedJobSystem plugin, JobType job) {
        this.plugin = plugin;
        this.job    = job;
    }

    public void open(Player player) {
        String title = TITLE_PREFIX + job.getDisplayName();
        Inventory inv = Bukkit.createInventory(null, 54, title);
        fillBorder(inv);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        switch (job) {
            case FARMER -> populateFarmerRewards(inv, data, player);
        }

        inv.setItem(45, buildBackButton());
        player.openInventory(inv);
    }

    // ── Farmer rewards ────────────────────────────────────────────────────────

    private void populateFarmerRewards(Inventory inv, PlayerData data, Player player) {
        int playerLevel = data.getLevel("FARMER");

        // Level 2 – Farmer's Boots (auto-granted on level-up, shown as info)
        inv.setItem(10, buildRewardItem(
                Material.LEATHER_BOOTS,
                "§2§lFarmer's Boots",
                2, playerLevel,
                List.of("§7Grants §aSpeed II §7while on farmland.", "", "§8Auto-granted on level-up."),
                "AUTO", data.hasClaimedReward("FARMER_LVL2_BOOTS")
        ));

        // Level 4 – Farmer's Hoe (claimable once)
        inv.setItem(13, buildRewardItem(
                Material.DIAMOND_HOE,
                "§a§lFarmer's Enchanted Hoe",
                4, playerLevel,
                List.of("§7Harvest a §e3×3 area§7 per swing.",
                        "§71% chance to §6crit§7 (double drops).",
                        "", "§c§lOne-time claim only."),
                "CLAIM", data.hasClaimedReward("FARMER_LVL4_HOE")
        ));

        // Level 6 – Farmer's Hat (claimable once)
        inv.setItem(16, buildRewardItem(
                Material.LEATHER_HELMET,
                "§2§lFarmer's Hat",
                6, playerLevel,
                List.of("§7Crops §aauto-replant §7when you harvest them.", "", "§c§lOne-time claim only."),
                "CLAIM", data.hasClaimedReward("FARMER_LVL6_HAT")
        ));

        // Level 8 – Automatic Farm (buyable, re-purchasable, costs 100 coins)
        inv.setItem(28, buildRewardItem(
                Material.DISPENSER,
                "§6§lAutomatic Farm",
                8, playerLevel,
                List.of("§7A placeable 3×3 self-harvesting farm.",
                        "§7Stores drops in an adjacent chest.",
                        "§7Works while chunk is unloaded!",
                        "", "§eCost: §6100 Gold Coins", "§7(Re-purchasable)"),
                "BUY", false // never "already claimed" — always purchasable
        ));

        // Level 10 – Money Tree Seeds (one-time, auto-granted on level-up)
        inv.setItem(34, buildRewardItem(
                Material.OAK_SAPLING,
                "§6§lMoney Tree Seeds §7(x5)",
                10, playerLevel,
                List.of("§7Grows in §e15 hours§7.", "§7Drops §a5–15 coins §7+ §a1–4 seeds§7.",
                        "", "§c§lOne-time, auto-granted on level-up."),
                "AUTO", data.isMoneyTreeSeedsClaimed()
        ));
    }

    // ── Item builder ──────────────────────────────────────────────────────────

    /**
     * @param mat          icon material
     * @param name         display name
     * @param reqLevel     level required to unlock/claim
     * @param playerLevel  player's current level
     * @param description  lore lines describing the reward
     * @param type         "CLAIM" | "BUY" | "AUTO"
     * @param alreadyDone  whether the one-time reward has already been claimed
     */
    private ItemStack buildRewardItem(Material mat, String name,
                                      int reqLevel, int playerLevel,
                                      List<String> description,
                                      String type, boolean alreadyDone) {
        boolean unlocked = playerLevel >= reqLevel;

        ItemStack item = new ItemStack(unlocked ? mat : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta = item.getItemMeta();

        meta.setDisplayName(unlocked ? name : "§8§l[LOCKED] §7" + name.replaceAll("§.", ""));

        List<String> lore = new ArrayList<>();
        lore.add("§8Required level: " + (unlocked ? "§a" : "§c") + reqLevel);
        lore.add("");
        lore.addAll(description);
        lore.add("");

        if (!unlocked) {
            lore.add("§c🔒 Reach level " + reqLevel + " to unlock.");
        } else if (alreadyDone && !type.equals("BUY")) {
            lore.add("§7✔ Already claimed.");
        } else {
            switch (type) {
                case "CLAIM" -> lore.add("§a▶ Click to claim!");
                case "BUY"   -> lore.add("§e▶ Click to purchase (100 coins)");
                case "AUTO"  -> lore.add("§7(Granted automatically on level-up)");
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName("§7◀ Back");
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorder(Inventory inv) {
        ItemStack glass = makeGlass();
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inv.setItem(i, glass);
            }
        }
    }

    private ItemStack makeGlass() {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  m = g.getItemMeta();
        m.setDisplayName("§r");
        g.setItemMeta(m);
        return g;
    }
}
