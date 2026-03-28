package com.enhancedjobs.npc;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.gui.VendorGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;

/**
 * Hooks into ZNPCS NPC right-click events via reflection.
 *
 * No compile-time dependency on ZNPCS is needed — all access
 * goes through reflection so the plugin compiles and runs even
 * when ZNPCS is absent (the listener simply won't be registered).
 *
 * Call {@link #register(EnhancedJobSystem, VendorManager)} from
 * onEnable() after confirming ZNPCS is loaded.
 */
public class ZNPCSVendorListener {

    private static final String EVENT_CLASS =
            "io.github.znetworkw.znpcservers.npc.event.NPCInteractEvent";

    private ZNPCSVendorListener() {}

    /**
     * Dynamically registers the ZNPCS interact handler.
     * Must only be called when ZNPCS is confirmed to be loaded.
     *
     * @return true if registration succeeded, false otherwise.
     */
    @SuppressWarnings("unchecked")
    public static boolean register(EnhancedJobSystem plugin, VendorManager vendorManager) {
        try {
            Class<? extends Event> eventClass =
                    (Class<? extends Event>) Class.forName(EVENT_CLASS);

            Listener dummy = new Listener() {};

            plugin.getServer().getPluginManager().registerEvent(
                    eventClass,
                    dummy,
                    EventPriority.NORMAL,
                    (listener, event) -> handleEvent(plugin, vendorManager, event),
                    plugin,
                    false
            );
            return true;

        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning(
                    "ZNPCS event class not found even though plugin is loaded: " + e.getMessage());
            return false;
        }
    }

    private static void handleEvent(EnhancedJobSystem plugin,
                                    VendorManager vendorManager,
                                    Event event) {
        try {
            // event.isRightClick()
            Method isRightClick = event.getClass().getMethod("isRightClick");
            if (!(boolean) isRightClick.invoke(event)) return;

            // event.getNpc().getId()
            Method getNpc = event.getClass().getMethod("getNpc");
            Object npc    = getNpc.invoke(event);
            Method getId  = npc.getClass().getMethod("getId");
            int npcId     = (int) getId.invoke(npc);

            if (!vendorManager.isVendor(npcId)) return;

            // event.getPlayer()
            Method getPlayer = event.getClass().getMethod("getPlayer");
            Player player    = (Player) getPlayer.invoke(event);

            // event.setCancelled(true)
            Method setCancelled = event.getClass().getMethod("setCancelled", boolean.class);
            setCancelled.invoke(event, true);

            new VendorGUI(plugin).open(player);

        } catch (Exception e) {
            plugin.getLogger().warning("ZNPCS vendor event handling error: " + e.getMessage());
        }
    }
}
