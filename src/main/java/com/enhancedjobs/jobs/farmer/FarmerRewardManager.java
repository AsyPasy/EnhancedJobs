package com.enhancedjobs.jobs.farmer;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.utils.ItemUtils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class FarmerRewardManager {

    private final EnhancedJobSystem plugin;

    public static final String REWARD_FERTILIZER  = "ADVANCED_FERTILIZER";
    public static final String REWARD_FORTUNE      = "FARMING_FORTUNE";
    public static final String REWARD_FARMERS_HAT  = "FARMERS_HAT";
    public static final String REWARD_AUTO_FARM    = "AUTOMATIC_FARM";
    public static final String REWARD_MONEY_TREE   = "MONEY_TREE_SEEDS";

    private final Map<UUID, Long> activeFertilizer  = new HashMap<>();
    private final Map<UUID, Long> fertilizerCooldown = new HashMap<>();

    public FarmerRewardManager(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    // ── Fertilizer state ──────────────────────────────────────────────────────

    public boolean hasFertilizerActive(Player player) {
        Long expiry = activeFertilizer.get(player.getUniqueId());
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) { activeFertilizer.remove(player.getUniqueId()); return false; }
        return true;
    }

    public boolean isFertilizerOnCooldown(Player player) {
        Long expiry = fertilizerCooldown.get(player.getUniqueId());
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) { fertilizerCooldown.remove(player.getUniqueId()); return false; }
        return true;
    }

    public long getFertilizerCooldownRemaining(Player player) {
        Long expiry = fertilizerCooldown.get(player.getUniqueId());
        return expiry == null ? 0 : Math.max(0, expiry - System.currentTimeMillis());
    }

    public void activateFertilizer(Player player) {
        long durMs  = TimeUnit.MINUTES.toMillis(plugin.getConfig().getLong("fertilizer-duration-minutes", 10));
        long coolMs = TimeUnit.HOURS.toMillis(plugin.getConfig().getLong("fertilizer-cooldown-hours", 3));

        activeFertilizer .put(player.getUniqueId(), System.currentTimeMillis() + durMs);
        fertilizerCooldown.put(player.getUniqueId(), System.currentTimeMillis() + coolMs);

        player.sendMessage("§a✦ Advanced Fertilizer activated! Crops double for §e"
            + plugin.getConfig().getLong("fertilizer-duration-minutes", 10) + " minutes§a.");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) player.sendMessage("§e⚠ Your Advanced Fertilizer has worn off.");
        }, durMs / 50L);
    }

    // ── Level 2: Buy Advanced Fertilizer (300 gold) ───────────────────────────

    public boolean buyFertilizer(Player player, PlayerData data) {
        if (data.getJobLevel(FarmerJob.ID) < 2) {
            player.sendMessage("§cRequires §eLevel 2 §cFarmer."); return false;
        }
        if (!data.removeGold(300)) {
            player.sendMessage("§cYou need §e300 gold coins§c."); return false;
        }
        player.getInventory().addItem(ItemUtils.createFertilizer());
        player.sendMessage("§aPurchased §eAdvanced Fertilizer§a! §7Right-click to consume.");
        return true;
    }

    // ── Level 4: Buy Farming Fortune Book (250 gold, Level I each) ───────────

    public boolean buyFarmingFortuneBook(Player player, PlayerData data) {
        if (data.getJobLevel(FarmerJob.ID) < 4) {
            player.sendMessage("§cRequires §eLevel 4 §cFarmer."); return false;
        }
        if (!data.removeGold(250)) {
            player.sendMessage("§cYou need §e250 gold coins§c."); return false;
        }
        player.getInventory().addItem(ItemUtils.createFarmingFortuneBook(1));
        player.sendMessage("§aPurchased §eFarming Fortune I §abook! Apply to a hoe in an anvil. §7Stacks to V.");
        return true;
    }

    // ── Level 6: Farmer's Hat (one-time claim) ────────────────────────────────

    public boolean claimFarmersHat(Player player, PlayerData data) {
        if (data.getJobLevel(FarmerJob.ID) < 6) {
            player.sendMessage("§cRequires §eLevel 6 §cFarmer."); return false;
        }
        if (data.hasClaimedReward(FarmerJob.ID, REWARD_FARMERS_HAT)) {
            player.sendMessage("§cAlready claimed."); return false;
        }
        data.claimReward(FarmerJob.ID, REWARD_FARMERS_HAT);
        player.getInventory().addItem(ItemUtils.createFarmersHat());
        player.sendMessage("§aClaimed §ethe Farmer's Hat§a! Wear it to auto-replant crops.");
        return true;
    }

    // ── Level 8: Automatic Farm (100 gold) ────────────────────────────────────

    public boolean buyAutoFarm(Player player, PlayerData data) {
        if (data.getJobLevel(FarmerJob.ID) < 8) {
            player.sendMessage("§cRequires §eLevel 8 §cFarmer."); return false;
        }
        if (!data.removeGold(100)) {
            player.sendMessage("§cYou need §e100 gold coins§c."); return false;
        }
        player.getInventory().addItem(ItemUtils.createAutoFarmBlock());
        player.sendMessage("§aPurchased §eAutomatic Farm§a! Place it to start a 3×3 auto-farm.");
        return true;
    }

    // ── Level 10: Money Tree Seeds (one-time claim) ───────────────────────────

    public boolean claimMoneyTreeSeeds(Player player, PlayerData data) {
        if (data.getJobLevel(FarmerJob.ID) < 10) {
            player.sendMessage("§cRequires §eLevel 10 §cFarmer."); return false;
        }
        if (data.hasClaimedReward(FarmerJob.ID, REWARD_MONEY_TREE)) {
            player.sendMessage("§cAlready claimed."); return false;
        }
        data.claimReward(FarmerJob.ID, REWARD_MONEY_TREE);
        ItemUtils.createMoneyTreeSeed(5).forEach(s -> player.getInventory().addItem(s));
        player.sendMessage("§aClaimed §e5 Money Tree Seeds§a!");
        return true;
    }
}
