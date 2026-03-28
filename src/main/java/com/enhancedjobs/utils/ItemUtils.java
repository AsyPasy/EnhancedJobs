package com.enhancedjobs.utils;

import com.enhancedjobs.EnhancedJobSystem;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemUtils {

    private static EnhancedJobSystem plugin;

    private static NamespacedKey KEY_FERTILIZER;
    private static NamespacedKey KEY_FARMING_FORTUNE_BOOK;
    private static NamespacedKey KEY_FARMING_FORTUNE_HOE;
    private static NamespacedKey KEY_FARMERS_HAT;
    private static NamespacedKey KEY_AUTO_FARM;
    private static NamespacedKey KEY_MONEY_TREE;

    private ItemUtils() {}

    public static void init(EnhancedJobSystem p) {
        plugin                   = p;
        KEY_FERTILIZER           = new NamespacedKey(p, "advanced_fertilizer");
        KEY_FARMING_FORTUNE_BOOK = new NamespacedKey(p, "farming_fortune_book");
        KEY_FARMING_FORTUNE_HOE  = new NamespacedKey(p, "farming_fortune_hoe");
        KEY_FARMERS_HAT          = new NamespacedKey(p, "farmers_hat");
        KEY_AUTO_FARM            = new NamespacedKey(p, "auto_farm_block");
        KEY_MONEY_TREE           = new NamespacedKey(p, "money_tree_seed");
    }

    // ── Hoe check ─────────────────────────────────────────────────────────────

    public static boolean isHoe(Material mat) {
        return switch (mat) {
            case WOODEN_HOE, STONE_HOE, IRON_HOE, GOLDEN_HOE,
                 DIAMOND_HOE, NETHERITE_HOE -> true;
            default -> false;
        };
    }

    // ── Roman numeral helper ──────────────────────────────────────────────────

    public static String toRoman(int level) {
        return switch (level) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III";
            case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(level);
        };
    }

    // ── Advanced Fertilizer ───────────────────────────────────────────────────

    public static ItemStack createFertilizer() {
        ItemStack item = new ItemStack(Material.BONE_MEAL);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName("§a✦ Advanced Fertilizer");
        meta.setLore(List.of(
            "§7Doubles crop drops for §e10 minutes§7.",
            "§73-hour cooldown.",
            "§e§lRight-click §7to consume.",
            "§8[Farmer Level 2 Reward]"
        ));
        meta.getPersistentDataContainer().set(KEY_FERTILIZER, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isFertilizer(ItemStack item) {
        return item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer()
                   .has(KEY_FERTILIZER, PersistentDataType.BYTE);
    }

    // ── Farming Fortune Book ──────────────────────────────────────────────────

    public static ItemStack createFarmingFortuneBook(int level) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName("§b✦ Farming Fortune " + toRoman(level));
        meta.setLore(List.of(
            "§7Enchantment for §ahoes§7.",
            "§bFarming Fortune " + toRoman(level) + " §7(+" + (level * 20) + "% drop chance)",
            "§7Apply via §eanvil§7. Stacks to §bLevel V§7.",
            "§8[Farmer Level 4 Reward]"
        ));
        meta.getPersistentDataContainer().set(KEY_FARMING_FORTUNE_BOOK, PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isFarmingFortuneBook(ItemStack item) {
        return item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer()
                   .has(KEY_FARMING_FORTUNE_BOOK, PersistentDataType.INTEGER);
    }

    public static int getFarmingFortuneBookLevel(ItemStack item) {
        if (!isFarmingFortuneBook(item)) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                   .getOrDefault(KEY_FARMING_FORTUNE_BOOK, PersistentDataType.INTEGER, 0);
    }

    // ── Farming Fortune Hoe ───────────────────────────────────────────────────

    public static int getFarmingFortuneLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                   .getOrDefault(KEY_FARMING_FORTUNE_HOE, PersistentDataType.INTEGER, 0);
    }

    public static void applyFarmingFortune(ItemStack item, int level) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_FARMING_FORTUNE_HOE, PersistentDataType.INTEGER, level);

        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(l -> l.contains("Farming Fortune"));
        lore.add(0, "§bFarming Fortune " + toRoman(level) + " §7(" + (level * 20) + "% chance to double drops)");
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    // ── Farmer's Hat ──────────────────────────────────────────────────────────

    public static ItemStack createFarmersHat() {
        ItemStack item = new ItemStack(Material.LEATHER_HELMET);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName("§2✦ Farmer's Hat");
        meta.setLore(List.of(
            "§7Automatically replants crops on harvest.",
            "§7Simply wear it while farming!",
            "§8[Farmer Level 6 – Unique]"
        ));
        meta.getPersistentDataContainer().set(KEY_FARMERS_HAT, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isFarmersHat(ItemStack item) {
        return item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer()
                   .has(KEY_FARMERS_HAT, PersistentDataType.BYTE);
    }

    // ── Automatic Farm Block ──────────────────────────────────────────────────

    public static ItemStack createAutoFarmBlock() {
        ItemStack item = new ItemStack(Material.HAY_BLOCK);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName("§e✦ Automatic Farm");
        meta.setLore(List.of(
            "§7Place to spawn a §e3×3 auto-farm§7.",
            "§7Harvests automatically into a chest.",
            "§7§nShift + right-click the chest§7 to change crop.",
            "§8[Farmer Level 8 Reward]"
        ));
        meta.getPersistentDataContainer().set(KEY_AUTO_FARM, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isAutoFarmBlock(ItemStack item) {
        return item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer()
                   .has(KEY_AUTO_FARM, PersistentDataType.BYTE);
    }

    // ── Money Tree Seed ───────────────────────────────────────────────────────

    public static List<ItemStack> createMoneyTreeSeed(int count) {
        List<ItemStack> seeds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ItemStack item = new ItemStack(Material.OAK_SAPLING);
            ItemMeta  meta = item.getItemMeta();
            meta.setDisplayName("§6✦ Money Tree Seed");
            meta.setLore(List.of(
                "§7Plant and wait §e15 hours§7.",
                "§7Drops §e5-15 gold coins §7and §e1-4 seeds§7.",
                "§8[Farmer Level 10 – Unique]"
            ));
            meta.getPersistentDataContainer().set(KEY_MONEY_TREE, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
            seeds.add(item);
        }
        return seeds;
    }

    public static boolean isMoneyTreeSeed(ItemStack item) {
        return item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer()
                   .has(KEY_MONEY_TREE, PersistentDataType.BYTE);
    }
}
