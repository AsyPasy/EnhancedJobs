package com.enhancedjobs.utils;

import com.enhancedjobs.EnhancedJobSystem;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class GoldManager {

    private static GoldManager instance;
    private final EnhancedJobSystem plugin;

    public GoldManager(EnhancedJobSystem plugin) {
        this.plugin = plugin;
        instance    = this;
    }

    public static GoldManager getInstance() { return instance; }

    // ── Economy bridge ────────────────────────────────────────────────────────

    private boolean isEconomyLoaded() {
        Plugin ge = Bukkit.getPluginManager().getPlugin("GoldEconomy");
        return ge != null && ge.isEnabled();
    }

    // ── Public API (UUID) ─────────────────────────────────────────────────────

    public double getBalance(UUID uuid) {
        if (isEconomyLoaded()) {
            try {
                Class<?> api = Class.forName("com.goldeconomy.GoldAPI");
                Object result = api.getMethod("getBalance", UUID.class).invoke(null, uuid);
                if (result instanceof Double d) return d;
            } catch (Exception ignored) {}
        }
        return plugin.getPlayerDataManager().getPlayerData(uuid).getInternalGold();
    }

    public void addGold(UUID uuid, double amount) {
        if (isEconomyLoaded()) {
            try {
                Class<?> api = Class.forName("com.goldeconomy.GoldAPI");
                api.getMethod("addGold", UUID.class, double.class).invoke(null, uuid, amount);
                return;
            } catch (Exception ignored) {}
        }
        plugin.getPlayerDataManager().getPlayerData(uuid).addInternalGold(amount);
    }

    public boolean removeGold(UUID uuid, double amount) {
        if (isEconomyLoaded()) {
            try {
                Class<?> api   = Class.forName("com.goldeconomy.GoldAPI");
                Object   result = api.getMethod("removeGold", UUID.class, double.class)
                                     .invoke(null, uuid, amount);
                if (result instanceof Boolean b) return b;
            } catch (Exception ignored) {}
        }
        return plugin.getPlayerDataManager().getPlayerData(uuid).removeInternalGold(amount);
    }

    public boolean hasGold(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public void setGold(UUID uuid, double amount) {
        if (isEconomyLoaded()) {
            try {
                Class<?> api = Class.forName("com.goldeconomy.GoldAPI");
                api.getMethod("setGold", UUID.class, double.class).invoke(null, uuid, amount);
                return;
            } catch (Exception ignored) {}
        }
        plugin.getPlayerDataManager().getPlayerData(uuid).setInternalGold(amount);
    }
}
