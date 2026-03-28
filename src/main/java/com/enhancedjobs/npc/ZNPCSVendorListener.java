package com.enhancedjobs.npc;
import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.gui.VendorGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listens for ZNPCS right-click interactions.
 *
 * ZNPCS (original by ZNix) fires {@link NPCInteractEvent} when a player
 * clicks an NPC. The event exposes the NPC's numeric ID via
 * {@code event.getNpc().getId()}.
 *
 * ── ZNPCS dependency note ────────────────────────────────────────────────────
 * Add ZNPCS to your build path (it is a softdepend so the plugin still loads
 * without it, but vendor NPC interaction will be inactive).
 *
 * Maven / local install:
 *   mvn install:install-file \
 *     -Dfile=ZNPCS.jar \
 *     -DgroupId=io.github.znetworkw \
 *     -DartifactId=znpcservers \
 *     -Dversion=3.9.5 \
 *     -Dpackaging=jar
 * Then add to pom.xml:
 *   <dependency>
 *     <groupId>io.github.znetworkw</groupId>
 *     <artifactId>znpcservers</artifactId>
 *     <version>3.9.5</version>
 *     <scope>provided</scope>
 *   </dependency>
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class ZNPCSVendorListener implements Listener {

    private final EnhancedJobSystem plugin;
    private final VendorManager vendorManager;

    public ZNPCSVendorListener(EnhancedJobSystem plugin, VendorManager vendorManager) {
        this.plugin        = plugin;
        this.vendorManager = vendorManager;
    }

    @EventHandler
    public void onNPCInteract(NPCInteractEvent event) {
        // Only handle right-click interactions
        if (!event.isRightClick()) return;

        int npcId = event.getNpc().getId();
        if (!vendorManager.isVendor(npcId)) return;

        Player player = event.getPlayer();
        event.setCancelled(true); // prevent any default NPC action

        // Open the crop selling GUI
        new VendorGUI(plugin).open(player);
    }
}
