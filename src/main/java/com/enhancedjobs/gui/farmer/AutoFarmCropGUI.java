package com.enhancedjobs.gui.farmer;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.jobs.farmer.AutomaticFarmManager.CropChoice;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class AutoFarmCropGUI implements InventoryHolder {

    public static final String TITLE = "§8§l✦ §r§eChoose Auto-Farm Crop §8§l✦";

    private final Location   farmCenter;
    private final CropChoice currentCrop;
    private Inventory        inventory;

    private AutoFarmCropGUI(Location farmCenter, CropChoice currentCrop) {
        this.farmCenter  = farmCenter;
        this.currentCrop = currentCrop;
    }

    public Location   getFarmCenter()  { return farmCenter; }
    public CropChoice getCurrentCrop() { return currentCrop; }

    @Override
    public Inventory getInventory() { return inventory; }

    public static void open(EnhancedJobSystem plugin, Player player,
                            Location farmCenter, CropChoice current) {
        AutoFarmCropGUI holder = new AutoFarmCropGUI(farmCenter, current);
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE);
        holder.inventory = inv;

        ItemStack glass = make(new ItemStack(Material.LIME_STAINED_GLASS_PANE), "§r", null);
        for (int i = 0; i < 27; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 2 || col == 0 || col == 8)
                inv.setItem(i, glass);
        }

        inv.setItem(4, make(new ItemStack(Material.HAY_BLOCK),
            "§e§lAuto-Farm Settings",
            List.of("§7Current crop: " + current.displayName,
                    "§7Click a crop below to change it.")));

        CropChoice[] choices = CropChoice.values();
        int[] slots = { 10, 11, 12, 13, 14 };
        for (int i = 0; i < choices.length && i < slots.length; i++) {
            CropChoice c = choices[i];
            boolean sel = c == current;
            // Use guiMaterial (item form) not blockMaterial (block form)
            inv.setItem(slots[i], make(new ItemStack(c.guiMaterial),
                (sel ? "§a§l✔ " : "§e") + c.displayName,
                List.of(sel ? "§a§lCurrently selected" : "§7Click to select",
                        c == CropChoice.MONEY_TREE
                            ? "§7Soil prepared; plant seeds manually."
                            : "§7Auto-harvested every cycle.")));
        }

        inv.setItem(22, make(new ItemStack(Material.ARROW), "§7Close",
            List.of("§7Close this menu.")));

        player.openInventory(inv);
    }

    private static ItemStack make(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
