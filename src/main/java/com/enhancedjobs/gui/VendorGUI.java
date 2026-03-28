package com.enhancedjobs.gui;

import com.enhancedjobs.EnhancedJobSystem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Crop Vendor GUI.
 *
 * Opens a 54-slot inventory displaying all sellable crops found in the
 * player's inventory. The player clicks a crop stack to toggle it as
 * "selected for sale" (glows green), then clicks the Confirm button to
 * sell all selected crops.
 *
 * Sell prices (per item) are read from config.yml under {@code crop-prices}.
 * Quest progress for SELL_CROPS is tracked via QuestManager on confirmation.
 *
 * Layout:
 *   Rows 0–3: crop items (up to 36 different stacks)
 *   Row 4:    separator glass
 *   Row 5:    slot 45 = "Select All", slot 49 = "Confirm Sale", slot 53 = "Close"
 */
public class VendorGUI {

    public static final String TITLE = "§2§lCrop Vendor";

    /** All materials that count as sellable crops. */
    public static final Set<Material> CROP_MATERIALS = Set.of(
            Material.WHEAT,
            Material.CARROT,
            Material.POTATO,
            Material.BEETROOT,
            Material.PUMPKIN,
            Material.MELON_SLICE,
            Material.MELON,
            Material.SUGAR_CANE,
            Material.COCOA_BEANS,
            Material.NETHER_WART,
            Material.SWEET_BERRIES,
            Material.GLOW_BERRIES,
            Material.TORCHFLOWER,
            Material.PITCHER_PLANT
    );

    private final EnhancedJobSystem plugin;

    public VendorGUI(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Fill row 4 separator
        ItemStack sep = makeGlass(Material.GREEN_STAINED_GLASS_PANE, "§r");
        for (int i = 36; i < 45; i++) inv.setItem(i, sep);
        inv.setItem(44, sep);

        // Collect all crop stacks from player inventory
        List<ItemStack> crops = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && CROP_MATERIALS.contains(item.getType())) {
                crops.add(item.clone());
            }
        }

        // Place crop items in slots 0–35
        for (int i = 0; i < crops.size() && i < 36; i++) {
            inv.setItem(i, buildCropItem(crops.get(i)));
        }

        if (crops.isEmpty()) {
            ItemStack none = new ItemStack(Material.BARRIER);
            ItemMeta  m    = none.getItemMeta();
            m.setDisplayName("§cNo sellable crops in inventory!");
            m.setLore(List.of("§7Bring crops to sell them here."));
            none.setItemMeta(m);
            inv.setItem(22, none);
        }

        // Bottom bar buttons
        inv.setItem(45, buildSelectAll());
        inv.setItem(49, buildConfirmButton(crops));
        inv.setItem(53, buildCloseButton());

        player.openInventory(inv);
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildCropItem(ItemStack crop) {
        ItemStack item = crop.clone();
        ItemMeta  meta = item.getItemMeta();
        double price   = getCropPrice(crop.getType());
        double total   = price * crop.getAmount();

        String name = formatMaterialName(crop.getType());
        meta.setDisplayName("§a" + name + " §7x" + crop.getAmount());
        meta.setLore(List.of(
                "",
                "§7Unit price: §e" + String.format("%.1f", price) + " coins",
                "§7Total:      §e" + String.format("%.1f", total) + " coins",
                "",
                "§7Click to select / deselect"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildConfirmButton(List<ItemStack> crops) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta  meta = item.getItemMeta();
        double grand   = crops.stream().mapToDouble(c -> getCropPrice(c.getType()) * c.getAmount()).sum();
        meta.setDisplayName("§a§lConfirm Sale");
        meta.setLore(List.of(
                "",
                "§7Selling all selected crops.",
                "§7Total earnings: §e" + String.format("%.1f", grand) + " coins",
                "",
                "§a▶ Click to sell!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSelectAll() {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName("§e§lSelect All Crops");
        meta.setLore(List.of("§7Selects all crops for sale."));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName("§c§lClose");
        item.setItemMeta(meta);
        return item;
    }

    // ── Price helpers ─────────────────────────────────────────────────────────

    /**
     * Returns the configured sell price for a crop material.
     * Defaults to 1.0 coin per item if not configured.
     */
    public double getCropPrice(Material mat) {
        String key = "crop-prices." + mat.name().toLowerCase();
        return plugin.getConfig().getDouble(key, 1.0);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private ItemStack makeGlass(Material mat, String name) {
        ItemStack g = new ItemStack(mat);
        ItemMeta  m = g.getItemMeta();
        m.setDisplayName(name);
        g.setItemMeta(m);
        return g;
    }

    private String formatMaterialName(Material mat) {
        String raw = mat.name().replace("_", " ").toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!word.isEmpty())
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
