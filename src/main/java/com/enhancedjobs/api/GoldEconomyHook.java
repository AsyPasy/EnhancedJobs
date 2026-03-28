package com.enhancedjobs.api;

import com.enhancedjobs.EnhancedJobSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Hook into GoldEconomy's static {@code com.goldeconomy.GoldAPI} class.
 *
 * GoldAPI exposes (all static):
 *   double  getBalance(UUID)
 *   void    addGold(UUID, double)
 *   boolean removeGold(UUID, double)
 *   boolean hasGold(UUID, double)
 *
 * We call these via reflection so EnhancedJobSystem compiles without
 * a GoldEconomy jar on the build path (soft dependency).
 */
public class GoldEconomyHook {

    private final EnhancedJobSystem plugin;
    private boolean enabled = false;

    private Method mGetBalance;
    private Method mAddGold;
    private Method mRemoveGold;
    private Method mHasGold;

    public GoldEconomyHook(EnhancedJobSystem plugin) {
        this.plugin = plugin;

        var ge = Bukkit.getPluginManager().getPlugin("GoldEconomy");
        if (ge == null || !ge.isEnabled()) {
            plugin.getLogger().warning("GoldEconomy not found – economy features disabled.");
            return;
        }

        try {
            Class<?> api = Class.forName("com.goldeconomy.GoldAPI");
            mGetBalance  = api.getMethod("getBalance",  UUID.class);
            mAddGold     = api.getMethod("addGold",     UUID.class, double.class);
            mRemoveGold  = api.getMethod("removeGold",  UUID.class, double.class);
            mHasGold     = api.getMethod("hasGold",     UUID.class, double.class);
            enabled = true;
            plugin.getLogger().info("GoldEconomy hook enabled successfully.");
        } catch (Exception e) {
            plugin.getLogger().warning("GoldEconomy hook failed to initialise: " + e.getMessage());
        }
    }

    public boolean isEnabled() { return enabled; }

    /** Returns the player's current gold coin balance. */
    public double getBalance(Player player) {
        if (!enabled) return 0;
        try { return (double) mGetBalance.invoke(null, player.getUniqueId()); }
        catch (Exception e) { warn("getBalance", e); return 0; }
    }

    /**
     * Withdraws {@code amount} coins from the player.
     * Returns true if successful (player had enough funds).
     */
    public boolean withdraw(Player player, double amount) {
        if (!enabled) return false;
        try { return (boolean) mRemoveGold.invoke(null, player.getUniqueId(), amount); }
        catch (Exception e) { warn("removeGold", e); return false; }
    }

    /** Deposits {@code amount} coins into the player's account. */
    public void deposit(Player player, double amount) {
        if (!enabled) return;
        try { mAddGold.invoke(null, player.getUniqueId(), amount); }
        catch (Exception e) { warn("addGold", e); }
    }

    /** Returns true if the player has at least {@code amount} coins. */
    public boolean has(Player player, double amount) {
        if (!enabled) return false;
        try { return (boolean) mHasGold.invoke(null, player.getUniqueId(), amount); }
        catch (Exception e) { warn("hasGold", e); return false; }
    }

    private void warn(String method, Exception e) {
        plugin.getLogger().warning("[GoldEconomy] " + method + " failed: " + e.getMessage());
    }
}
