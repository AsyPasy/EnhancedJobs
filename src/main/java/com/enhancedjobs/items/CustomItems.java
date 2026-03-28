package com.enhancedjobs.items;

import com.enhancedjobs.EnhancedJobSystem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

/**
 * Central factory and identifier for every custom item in EnhancedJobSystem.
 * Items are tagged with a PersistentDataContainer key "ejs_item" whose value
 * is the item's internal ID string.
 */
public class CustomItems {

    // Internal ID constants
    public static final String ID_FARMERS_BOOTS    = "farmers_boots";
    public static final String ID_FARMERS_HOE      = "farmers_hoe";
    public static final String ID_FARMERS_HAT      = "farmers_hat";
    public static final String ID_AUTO_FARM        = "auto_farm";
    public static final String ID_MONEY_TREE_SEED  = "money_tree_seed";
    public static final String ID_SCARECROW        = "scarecrow";

    private static NamespacedKey itemKey;

    public static void init(EnhancedJobSystem plugin) {
        itemKey = new NamespacedKey(plugin, "ejs_item");
        registerScarecrowRecipe(plugin);
    }

    // ── Identification helpers ────────────────────────────────────────────────

    public static boolean isCustomItem(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return id.equals(pdc.get(itemKey, PersistentDataType.STRING));
    }

    public static boolean isFarmersBoots(ItemStack item)   { return isCustomItem(item, ID_FARMERS_BOOTS);   }
    public static boolean isFarmersHoe(ItemStack item)     { return isCustomItem(item, ID_FARMERS_HOE);     }
    public static boolean isFarmersHat(ItemStack item)     { return isCustomItem(item, ID_FARMERS_HAT);     }
    public static boolean isAutoFarm(ItemStack item)       { return isCustomItem(item, ID_AUTO_FARM);       }
    public static boolean isMoneyTreeSeed(ItemStack item)  { return isCustomItem(item, ID_MONEY_TREE_SEED); }
    public static boolean isScarecrow(ItemStack item)      { return isCustomItem(item, ID_SCARECROW);       }

    // ── Item factories ────────────────────────────────────────────────────────

    /** Level 2 reward: Farmer's Boots – grants Speed II when on farmland. */
    public static ItemStack createFarmersBoots() {
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§2§lFarmer's Boots");
        meta.setLore(Arrays.asList(
                "§7Standing on farmland grants",
                "§aSpeed II§7.",
                "",
                "§8Job Reward – Farmer Lvl 2"
        ));
        meta.addEnchant(Enchantment.PROTECTION, 2, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        tag(meta, ID_FARMERS_BOOTS);
        item.setItemMeta(meta);
        return item;
    }

    /** Level 4 reward: Farmer's Hoe – harvests 3×3 area; 1% crit for double drops. */
    public static ItemStack createFarmersHoe() {
        ItemStack item = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§a§lFarmer's Enchanted Hoe");
        meta.setLore(Arrays.asList(
                "§7Right-click crops to harvest",
                "§ea 3×3 area§7.",
                "§71% chance per swing to §6crit§7",
                "§7(doubles crop drops).",
                "",
                "§8Job Reward – Farmer Lvl 4"
        ));
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        tag(meta, ID_FARMERS_HOE);
        item.setItemMeta(meta);
        return item;
    }

    /** Level 6 reward: Farmer's Hat – auto-replants crops when harvested. */
    public static ItemStack createFarmersHat() {
        ItemStack item = new ItemStack(Material.LEATHER_HELMET);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§2§lFarmer's Hat");
        meta.setLore(Arrays.asList(
                "§7Crops are automatically",
                "§areplanted§7 after you harvest them.",
                "",
                "§8Job Reward – Farmer Lvl 6",
                "§c§lOne-time claimable only!"
        ));
        meta.addEnchant(Enchantment.PROTECTION, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        tag(meta, ID_FARMERS_HAT);
        item.setItemMeta(meta);
        return item;
    }

    /** Level 8 reward: Auto Farm – a placeable self-harvesting 3×3 farm block. */
    public static ItemStack createAutoFarmBlock() {
        ItemStack item = new ItemStack(Material.DISPENSER);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§6§lAutomatic Farm");
        meta.setLore(Arrays.asList(
                "§7Place this block to create an",
                "§aAutomatic 3×3 Farm§7.",
                "",
                "§7Crops are harvested every §e30 minutes§7",
                "§7and stored in the adjacent chest.",
                "§7Works even when the chunk is unloaded!",
                "",
                "§8Job Reward – Farmer Lvl 8",
                "§ePrice: 100 Gold Coins"
        ));
        tag(meta, ID_AUTO_FARM);
        item.setItemMeta(meta);
        return item;
    }

    /** Level 10 reward: Money Tree Seeds (5x). */
    public static ItemStack createMoneyTreeSeeds(int amount) {
        ItemStack item = new ItemStack(Material.OAK_SAPLING, amount);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§6§lMoney Tree Seed");
        meta.setLore(Arrays.asList(
                "§7Plant on farmland and wait §e15 hours§7.",
                "§7When fully grown, break it to receive:",
                "§a5–15 Gold Coins §7and §a1–4 seeds§7.",
                "",
                "§8Job Reward – Farmer Lvl 10",
                "§c§lOne-time claimable only!"
        ));
        tag(meta, ID_MONEY_TREE_SEED);
        item.setItemMeta(meta);
        return item;
    }

    /** Scarecrow item (result of the crafting recipe). */
    public static ItemStack createScarecrow() {
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName("§a§lScarecrow");
        meta.setLore(Arrays.asList(
                "§7A handcrafted scarecrow.",
                "§7Used to complete a Farmer quest.",
                "",
                "§8Crafted via the crafting table."
        ));
        tag(meta, ID_SCARECROW);
        item.setItemMeta(meta);
        return item;
    }

    // ── Scarecrow crafting recipe ─────────────────────────────────────────────
    //   Layout:
    //     [ ]  [P]  [ ]
    //     [W]  [S]  [W]
    //     [ ]  [S]  [ ]
    //   P = Carved Pumpkin, S = Stick, W = Wheat

    private static void registerScarecrowRecipe(EnhancedJobSystem plugin) {
        NamespacedKey key = new NamespacedKey(plugin, "scarecrow_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, createScarecrow());
        recipe.shape(" P ", "WSW", " S ");
        recipe.setIngredient('P', Material.CARVED_PUMPKIN);
        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('W', Material.WHEAT);
        plugin.getServer().addRecipe(recipe);
    }

    // ── Internal tag helper ───────────────────────────────────────────────────

    private static void tag(ItemMeta meta, String id) {
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
    }
}
