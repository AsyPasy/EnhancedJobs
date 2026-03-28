package com.enhancedjobs.npc;

import com.enhancedjobs.EnhancedJobSystem;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks which ZNPCS NPC IDs have been registered as Crop Vendors via
 * {@code /jobadmin setvendor <npcId>}.
 *
 * ZNPCS NPC IDs are integers assigned when the NPC is created.
 * We listen for right-click interactions in {@link ZNPCSVendorListener}.
 *
 * Vendor IDs are persisted in config.yml under {@code vendor-npc-ids}.
 */
public class VendorManager {

    private final EnhancedJobSystem plugin;
    private final Set<Integer> vendorIds = new HashSet<>();

    public VendorManager(EnhancedJobSystem plugin) {
        this.plugin = plugin;
        load();
    }

    // ── API ───────────────────────────────────────────────────────────────────

    public boolean isVendor(int npcId) {
        return vendorIds.contains(npcId);
    }

    public boolean addVendor(int npcId) {
        boolean added = vendorIds.add(npcId);
        if (added) save();
        return added;
    }

    public boolean removeVendor(int npcId) {
        boolean removed = vendorIds.remove(npcId);
        if (removed) save();
        return removed;
    }

    public Set<Integer> getVendorIds() {
        return vendorIds;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        List<Integer> ids = plugin.getConfig().getIntegerList("vendor-npc-ids");
        vendorIds.addAll(ids);
    }

    private void save() {
        plugin.getConfig().set("vendor-npc-ids", List.copyOf(vendorIds));
        plugin.saveConfig();
    }
}
