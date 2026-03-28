package com.enhancedjobs.gui;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.jobs.JobType;
import com.enhancedjobs.utils.XPUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Main job browser GUI (opened via /job).
 * Shows every available JobType as a clickable icon.
 * Players can click a job to open its detail/info GUI.
 *
 * Inventory title: "§8§lChoose a Job"
 * Size: 27 slots (3 rows), jobs laid out starting at slot 10.
 */
public class JobSelectionGUI {

    public static final String TITLE = "§8§lChoose a Job";

    private final EnhancedJobSystem plugin;

    public JobSelectionGUI(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Fill borders with gray glass
        fillBorder(inv);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        JobType[] jobs  = JobType.values();

        // Start jobs at slot 10 (second row, second col) for a centred look
        int[] slots = buildCentredSlots(jobs.length);
        for (int i = 0; i < jobs.length && i < slots.length; i++) {
            JobType job  = jobs[i];
            boolean has  = data.hasJob(job.name());
            int level    = data.getLevel(job.name());
            double xp    = data.getXP(job.name());
            double needed = XPUtils.getXPForLevel(level);

            inv.setItem(slots[i], buildJobIcon(job, has, level, xp, needed));
        }

        player.openInventory(inv);
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildJobIcon(JobType job, boolean hasJob, int level, double xp, double needed) {
        ItemStack item = new ItemStack(job.getIcon());
        ItemMeta  meta = item.getItemMeta();

        String statusColor = hasJob ? "§a" : "§7";
        String statusTag   = hasJob ? " §a[JOINED]" : " §7[Click to join]";
        meta.setDisplayName(job.getPrimaryColor() + "§l" + job.getDisplayName() + statusTag);

        List<String> lore = new ArrayList<>();
        lore.add("§8" + job.getDescription());
        lore.add("");

        if (hasJob) {
            lore.add(statusColor + "Level: §f" + level + " §7/ " + XPUtils.MAX_LEVEL);
            if (level < XPUtils.MAX_LEVEL) {
                lore.add(statusColor + "XP:    §f" + String.format("%.1f", xp)
                        + " §7/ " + String.format("%.0f", needed));
                lore.add("       " + XPUtils.buildProgressBar(xp, needed, 20));
            } else {
                lore.add("§6§lMAX LEVEL REACHED!");
            }
            lore.add("");
            lore.add("§e▶ Click to view quests & rewards");
            lore.add("§c▶ Shift-click to leave this job");
        } else {
            lore.add("§7Click to join and start earning XP!");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns slot indices centred across row 1 (slots 9–17). */
    private int[] buildCentredSlots(int count) {
        int[] available = {10, 11, 12, 13, 14, 15, 16};
        if (count >= available.length) return available;
        int start = (available.length - count) / 2;
        int[] result = new int[count];
        for (int i = 0; i < count; i++) result[i] = available[start + i];
        return result;
    }

    private void fillBorder(Inventory inv) {
        ItemStack glass = makeGlass();
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == size / 9 - 1 || col == 0 || col == 8) {
                inv.setItem(i, glass);
            }
        }
    }

    private ItemStack makeGlass() {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  m = g.getItemMeta();
        m.setDisplayName("§r");
        g.setItemMeta(m);
        return g;
    }
}
