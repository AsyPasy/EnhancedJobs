package com.enhancedjobs.gui.farmer;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.jobs.farmer.FarmerJob;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class FarmerVendorGUI {

    public static final String TITLE      = "§8§l✦ §r§bFarmer Vendor §8§l✦";
    public static final String TITLE_SELL = "§8§l✦ §r§cSell Crops §8§l✦";

    public record VendorItem(Material material, String name, double goldCost, int amount) {}

    // Items the player can BUY
    public static final VendorItem[] ITEMS = {
        new VendorItem(Material.WHEAT_SEEDS,    "§fWheat Seeds",    1,  16),
        new VendorItem(Material.CARROT,         "§fCarrots",        1,  16),
        new VendorItem(Material.POTATO,         "§fPotatoes",       1,  16),
        new VendorItem(Material.BEETROOT_SEEDS, "§fBeetroot Seeds", 1,  16),
        new VendorItem(Material.MELON_SEEDS,    "§fMelon Seeds",    2,   8),
        new VendorItem(Material.PUMPKIN_SEEDS,  "§fPumpkin Seeds",  2,   8),
        new VendorItem(Material.BONE_MEAL,      "§fBone Meal",      2,   8),
        new VendorItem(Material.BUCKET,         "§fBucket",         3,   1),
        new VendorItem(Material.WHEAT,          "§fWheat ×32",      5,  32),
        new VendorItem(Material.HAY_BLOCK,      "§fHay Block",      4,   4),
        new VendorItem(Material.COMPOSTER,      "§fComposter",      8,   1),
        new VendorItem(Material.WATER_BUCKET,   "§fWater Bucket",   3,   1),
    };

    // Items the player can SELL and how much gold each gives per item
    public record SellItem(Material material, String name, double goldPerItem) {}

    public static final SellItem[] SELL_ITEMS = {
        new SellItem(Material.WHEAT,        "§fWheat",         0.1),
        new SellItem(Material.CARROT,       "§fCarrot",        0.1),
        new SellItem(Material.POTATO,       "§fPotato",        0.1),
        new SellItem(Material.BEETROOT,     "§fBeetroot",      0.1),
        new SellItem(Material.MELON_SLICE,  "§fMelon Slice",   0.15),
        new SellItem(Material.PUMPKIN,      "§fPumpkin",       0.5),
        new SellItem(Material.SUGAR_CANE,   "§fSugar Cane",    0.1),
        new SellItem(Material.COCOA_BEANS,  "§fCocoa Beans",   0.2),
        new SellItem(Material.NETHER_WART,  "§fNether Wart",   0.3),
        new SellItem(Material.SWEET_BERRIES,"§fSweet Berries", 0.15),
        new SellItem(Material.HAY_BLOCK,    "§fHay Block",     0.8),
        new SellItem(Material.PUMPKIN_PIE,  "§fPumpkin Pie",   0.4),
    };

    private FarmerVendorGUI() {}

    // ── Buy GUI ───────────────────────────────────────────────────────────────

    public static void open(EnhancedJobSystem plugin, Player player) {
        Inventory  inv  = Bukkit.createInventory(null, 54, TITLE);
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        // Gold display (slot 4)
        inv.setItem(4, goldDisplay(data.getGold()));

        // Buy items (slots 10–25)
        int[] itemSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25};
        for (int i = 0; i < Math.min(ITEMS.length, itemSlots.length); i++)
            inv.setItem(itemSlots[i], buildBuyItem(ITEMS[i]));

        // Switch to Sell tab (slot 40)
        inv.setItem(40, makeTabButton(Material.GOLD_INGOT, "§6→ Switch to Sell",
            List.of("§7Click to sell your crops.")));

        // Back (slot 49)
        inv.setItem(49, FarmerGUI.makeButton(Material.ARROW, "§7← Back",
            List.of("§7Return to Farmer menu.")));

        player.openInventory(inv);
    }

    // ── Sell GUI ──────────────────────────────────────────────────────────────

    public static void openSell(EnhancedJobSystem plugin, Player player) {
        Inventory  inv  = Bukkit.createInventory(null, 54, TITLE_SELL);
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        fillBorder(inv, Material.RED_STAINED_GLASS_PANE);

        // Gold display (slot 4)
        inv.setItem(4, goldDisplay(data.getGold()));

        // Sell items (slots 10–25)
        int[] sellSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25};
        for (int i = 0; i < Math.min(SELL_ITEMS.length, sellSlots.length); i++)
            inv.setItem(sellSlots[i], buildSellItem(SELL_ITEMS[i], player));

        // Sell ALL button (slot 31)
        ItemStack sellAll = new ItemStack(Material.EMERALD);
        ItemMeta  sm      = sellAll.getItemMeta();
        sm.setDisplayName("§a§lSell ALL Crops");
        sm.setLore(List.of(
            "§7Sells every crop in your inventory",
            "§7at the listed prices.",
            "",
            "§eClick to sell everything!"
        ));
        sellAll.setItemMeta(sm);
        inv.setItem(31, sellAll);

        // Switch to Buy tab (slot 40)
        inv.setItem(40, makeTabButton(Material.CYAN_DYE, "§b→ Switch to Buy",
            List.of("§7Click to buy farming supplies.")));

        // Back (slot 49)
        inv.setItem(49, FarmerGUI.makeButton(Material.ARROW, "§7← Back",
            List.of("§7Return to Farmer menu.")));

        player.openInventory(inv);
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    public static ItemStack buildBuyItem(VendorItem vi) {
        ItemStack item = new ItemStack(vi.material(), vi.amount());
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(vi.name());
        meta.setLore(List.of(
            "§7Amount: §e×" + vi.amount(),
            "§7Cost:   §e" + vi.goldCost() + " gold coin(s)",
            "",
            "§eClick to purchase!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildSellItem(SellItem si, Player player) {
        // Count how many the player has
        int count = countInInventory(player, si.material());
        double totalGold = Math.round(count * si.goldPerItem() * 10.0) / 10.0;

        ItemStack item = new ItemStack(si.material(), Math.max(1, count));
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(si.name());
        meta.setLore(List.of(
            "§7Price per item: §e" + si.goldPerItem() + " gold",
            "§7You have:       §e" + count,
            "§7You'll receive: §e" + totalGold + " gold",
            "",
            count > 0 ? "§eClick to sell all!" : "§cYou have none of this."
        ));
        item.setItemMeta(meta);
        return item;
    }

 private static ItemStack goldDisplay(double gold) {
    ItemStack item = new ItemStack(Material.SUNFLOWER);
    ItemMeta  meta = item.getItemMeta();
    meta.setDisplayName("§6⬛ Your Gold Balance");
    meta.setLore(List.of(
        "§e" + String.format("%.2f", gold) + " §6Gold Coins"
    ));
    item.setItemMeta(meta);
    return item;
}

    private static ItemStack makeTabButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    public static int countInInventory(Player player, Material mat) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == mat)
                total += stack.getAmount();
        }
        return total;
    }

    public static void removeFromInventory(Player player, Material mat, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (remaining <= 0) break;
            if (stack == null || stack.getType() != mat) continue;
            if (stack.getAmount() <= remaining) {
                remaining -= stack.getAmount();
                stack.setAmount(0);
            } else {
                stack.setAmount(stack.getAmount() - remaining);
                remaining = 0;
            }
        }
    }

    private static void fillBorder(Inventory inv, Material mat) {
        ItemStack glass = new ItemStack(mat);
        ItemMeta  m     = glass.getItemMeta();
        m.setDisplayName("§r");
        glass.setItemMeta(m);
        for (int i = 0; i < 54; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8)
                inv.setItem(i, glass);
        }
    }
}
