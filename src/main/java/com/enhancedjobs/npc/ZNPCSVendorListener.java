package com.enhancedjobs.npc;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.gui.VendorGUI;
import io.github.znetworkw.znpcservers.npc.event.NPCInteractEvent; // ← ADD THIS
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ZNPCSVendorListener implements Listener {

    private final EnhancedJobSystem plugin;
    private final VendorManager vendorManager;

    public ZNPCSVendorListener(EnhancedJobSystem plugin, VendorManager vendorManager) {
        this.plugin        = plugin;
        this.vendorManager = vendorManager;
    }

    @EventHandler
    public void onNPCInteract(NPCInteractEvent event) {
        if (!event.isRightClick()) return;

        int npcId = event.getNpc().getId();
        if (!vendorManager.isVendor(npcId)) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        new VendorGUI(plugin).open(player);
    }
}
