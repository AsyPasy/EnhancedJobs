package com.enhancedjobs.rewards;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.api.GoldEconomyHook;
import com.enhancedjobs.data.DataManager;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.items.CustomItems;
import org.bukkit.entity.Player;

/**
 * Handles granting all five Farmer level rewards.
 *
 * Level 2  – Farmer's Boots (granted automatically, re-claimable if lost? No – one per level-up)
 * Level 4  – Farmer's Enchanted Hoe (one-time claimable from GUI)
 * Level 6  – Farmer's Hat (one-time claimable from GUI)
 * Level 8  – AutoFarm block purchase (100 gold coins, re-buyable)
 * Level 10 – 5× Money Tree Seeds (one-time only, ever)
 */
public class FarmerRewards {

    private static final String KEY_BOOTS   = "FARMER_LVL2_BOOTS";
    private static final String KEY_HOE     = "FARMER_LVL4_HOE";
    private static final String KEY_HAT     = "FARMER_LVL6_HAT";
    private static final String KEY_TREES   = "FARMER_LVL10_SEEDS";

    private final EnhancedJobSystem plugin;
    private final DataManager dataManager;
    private final GoldEconomyHook economy;

    public FarmerRewards(EnhancedJobSystem plugin, DataManager dataManager, GoldEconomyHook economy) {
        this.plugin      = plugin;
        this.dataManager = dataManager;
        this.economy     = economy;
    }

    // ── Called on level-up ────────────────────────────────────────────────────

    /**
     * Checks the new level and auto-grants or notifies the player of available rewards.
     */
    public void onLevelUp(Player player, int newLevel) {
        switch (newLevel) {
            case 2  -> grantBoots(player);
            case 4  -> notifyClaimable(player, "§aFarmer's Enchanted Hoe", "Farmer Level 4 reward");
            case 6  -> notifyClaimable(player, "§aFarmer's Hat", "Farmer Level 6 reward");
            case 8  -> notifyBuyable(player, "§6Automatic Farm", 100);
            case 10 -> grantMoneyTreeSeeds(player);
        }
    }

    // ── Individual reward methods ─────────────────────────────────────────────

    /** Level 2: Farmer's Boots – granted automatically on level-up. */
    private void grantBoots(Player player) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.hasClaimedReward(KEY_BOOTS)) return; // already got them this lifetime
        data.claimReward(KEY_BOOTS);
        player.getInventory().addItem(CustomItems.createFarmersBoots());
        dataManager.savePlayerData(player.getUniqueId());
        player.sendMessage("§6§l[Jobs] §r§aReward unlocked: §eFarmer's Boots§a! "
                + "Gain §bSpeed II§a while standing on farmland.");
    }

    /** Level 4: Hoe – triggered from GUI claim button (via {@link RewardManager}). */
    public boolean claimHoe(Player player) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getLevel("FARMER") < 4) {
            player.sendMessage("§c[Jobs] You need to be Farmer Level 4 to claim this.");
            return false;
        }
        if (data.hasClaimedReward(KEY_HOE)) {
            player.sendMessage("§c[Jobs] You have already claimed the Farmer's Hoe.");
            return false;
        }
        data.claimReward(KEY_HOE);
        player.getInventory().addItem(CustomItems.createFarmersHoe());
        dataManager.savePlayerData(player.getUniqueId());
        player.sendMessage("§6§l[Jobs] §r§aClaimed: §eFarmer's Enchanted Hoe§a!");
        return true;
    }

    /** Level 6: Hat – one-time claimable, only via GUI. */
    public boolean claimHat(Player player) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getLevel("FARMER") < 6) {
            player.sendMessage("§c[Jobs] You need to be Farmer Level 6 to claim this.");
            return false;
        }
        if (data.hasClaimedReward(KEY_HAT)) {
            player.sendMessage("§c[Jobs] You have already claimed the Farmer's Hat.");
            return false;
        }
        data.claimReward(KEY_HAT);
        player.getInventory().addItem(CustomItems.createFarmersHat());
        dataManager.savePlayerData(player.getUniqueId());
        player.sendMessage("§6§l[Jobs] §r§aClaimed: §eFarmer's Hat§a! Crops replant automatically.");
        return true;
    }

    /**
     * Level 8: AutoFarm block – costs 100 gold coins, re-purchasable.
     * Triggered from GUI claim/buy button.
     */
    public boolean buyAutoFarm(Player player) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.getLevel("FARMER") < 8) {
            player.sendMessage("§c[Jobs] You need to be Farmer Level 8 to purchase this.");
            return false;
        }
        if (!economy.isEnabled()) {
            player.sendMessage("§c[Jobs] GoldEconomy is not available. Cannot process purchase.");
            return false;
        }
        double cost = 100.0;
        if (economy.getBalance(player) < cost) {
            player.sendMessage("§c[Jobs] You need §e100 gold coins§c to buy the Automatic Farm. "
                    + "You have §e" + economy.getBalance(player) + "§c.");
            return false;
        }
        if (!economy.withdraw(player, cost)) {
            player.sendMessage("§c[Jobs] Payment failed. Please try again.");
            return false;
        }
        player.getInventory().addItem(CustomItems.createAutoFarmBlock());
        player.sendMessage("§6§l[Jobs] §r§aPurchased: §6Automatic Farm Block§a! "
                + "Place it to start your auto-harvesting 3×3 farm.");
        return true;
    }

    /** Level 10: Money Tree Seeds – 5 seeds, one-time only, ever. */
    private void grantMoneyTreeSeeds(Player player) {
        PlayerData data = dataManager.getPlayerData(player.getUniqueId());
        if (data.isMoneyTreeSeedsClaimed()) return;
        data.setMoneyTreeSeedsClaimed(true);
        dataManager.savePlayerData(player.getUniqueId());
        player.getInventory().addItem(CustomItems.createMoneyTreeSeeds(5));
        player.sendMessage("§6§l[Jobs] §r§aMayor reward: §e5 Money Tree Seeds§a! "
                + "Each grows for 15 hours and drops 5–15 gold coins + 1–4 seeds. "
                + "§c§lThis reward can never be obtained again!");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void notifyClaimable(Player player, String itemName, String source) {
        player.sendMessage("§6§l[Jobs] §r§aNew reward available: " + itemName
                + " §7(" + source + ")§a! Claim it via §e/job§a.");
    }

    private void notifyBuyable(Player player, String itemName, int cost) {
        player.sendMessage("§6§l[Jobs] §r§aNew reward available: §6" + itemName
                + " §7(costs " + cost + " gold coins)§a! Purchase it via §e/job§a.");
    }
}
