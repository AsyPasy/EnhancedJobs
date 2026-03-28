package com.enhancedjobs.listeners;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.items.CustomItems;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles the passive effects of custom reward items:
 *
 * Farmer's Boots (Lvl 2):
 *   - Grants Speed II while the player stands on FARMLAND.
 *   - Removes the effect when they leave farmland.
 *
 * Farmer's Enchanted Hoe (Lvl 4):
 *   - Right-click a fully-grown crop → harvest ALL fully-grown crops in a 3×3 area.
 *   - 1% chance per swing to "crit" → doubles drops from each harvested block.
 *   - Hat replant logic also fires here if the player is wearing the hat.
 *
 * Farmer's Hat (Lvl 6):
 *   - Auto-replant is handled inside FarmerListener#onBlockBreak for individual breaks;
 *   - This listener adds replant support for the 3×3 hoe sweeps.
 */
public class RewardListener implements Listener {

    private static final Random RANDOM = new Random();
    private static final double CRIT_CHANCE = 0.01; // 1%

    private final EnhancedJobSystem plugin;

    public RewardListener(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    // ── Farmer's Boots: Speed II on farmland ─────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only evaluate when the player actually changes blocks (reduces overhead)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()) return;

        Player    player = event.getPlayer();
        ItemStack boots  = player.getInventory().getBoots();
        if (!CustomItems.isFarmersBoots(boots)) return;

        Block below = player.getLocation().subtract(0, 0.1, 0).getBlock();
        boolean onFarmland = below.getType() == Material.FARMLAND;

        boolean hasSpeed = player.hasPotionEffect(PotionEffectType.SPEED);

        if (onFarmland && !hasSpeed) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false, false));
        } else if (!onFarmland && hasSpeed) {
            // Only remove if it was the boots that gave it (ambient flag = false)
            PotionEffect effect = player.getPotionEffect(PotionEffectType.SPEED);
            if (effect != null && !effect.isAmbient()) {
                player.removePotionEffect(PotionEffectType.SPEED);
            }
        }
    }

    // ── Farmer's Hoe: 3×3 harvest + 1% crit ──────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHoeRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player    player = event.getPlayer();
        ItemStack hoe    = player.getInventory().getItemInMainHand();
        if (!CustomItems.isFarmersHoe(hoe)) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        // Only activate on a crop that is fully grown
        if (!(clicked.getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() < ageable.getMaximumAge()) return;

        event.setCancelled(true); // prevent normal block interaction

        boolean crit       = RANDOM.nextDouble() < CRIT_CHANCE;
        boolean wearingHat = CustomItems.isFarmersHat(player.getInventory().getHelmet());

        // Collect all fully-grown crop blocks in 3×3
        List<Block> targets = get3x3Crops(clicked);

        for (Block block : targets) {
            if (crit) {
                // Double drops: drop items manually twice then break silently
                List<ItemStack> drops = new ArrayList<>(block.getDrops(hoe));
                drops.addAll(block.getDrops(hoe));
                block.setType(Material.AIR);
                for (ItemStack drop : drops) {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            } else {
                block.breakNaturally(hoe);
            }

            // Hat auto-replant for 3×3 sweep
            if (wearingHat) {
                Material seed = getSeedFor(block.getType());
                if (seed != null) {
                    final Block finalBlock = block;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (finalBlock.getType() == Material.AIR) finalBlock.setType(seed);
                    }, 1L);
                }
            }
        }

        // Track harvest quest progress for all harvested blocks
        for (Block block : targets) {
            plugin.getQuestManager().addProgress(
                    player, "FARMER",
                    block.getType() == Material.CARROTS
                            ? com.enhancedjobs.quests.QuestTaskType.HARVEST_CARROTS
                            : com.enhancedjobs.quests.QuestTaskType.HARVEST_WHEAT,
                    1
            );
            plugin.getQuestManager().grantPassiveXP(player, "FARMER",
                    plugin.getConfig().getDouble("passive-xp.harvest-crop", 0.2));
        }

        if (crit) {
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                    "✦ Critical Harvest! Drops doubled! ✦",
                    net.kyori.adventure.text.format.NamedTextColor.GOLD,
                    net.kyori.adventure.text.format.TextDecoration.BOLD));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Block> get3x3Crops(Block center) {
        List<Block> blocks = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block b = center.getRelative(dx, 0, dz);
                if (b.getBlockData() instanceof Ageable a && a.getAge() == a.getMaximumAge()) {
                    blocks.add(b);
                }
            }
        }
        return blocks;
    }

    private Material getSeedFor(Material crop) {
        return switch (crop) {
            case WHEAT    -> Material.WHEAT;
            case CARROTS  -> Material.CARROTS;
            case POTATOES -> Material.POTATOES;
            case BEETROOTS-> Material.BEETROOTS;
            default       -> null;
        };
    }
}
