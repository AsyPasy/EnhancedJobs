package com.enhancedjobs.gui;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.jobs.JobType;
import com.enhancedjobs.quests.ActiveQuest;
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
 * Quest GUI for a specific job.
 *
 * Layout (54 slots / 6 rows):
 *   Row 0: border (glass)
 *   Row 1: quest slots 1–3 (slots 10, 13, 16)
 *   Row 2: quest slots 4–6 (if more than 3)   — reserved for future daily-count expansion
 *   Row 5: border with 24h timer indicator in slot 49 (bottom-centre)
 *
 * The 24h reset timer is shown directly in the GUI on the clock icon at the bottom.
 */
public class QuestGUI {

    public static final String TITLE_PREFIX = "§6§lQuests: §r§e";

    private final EnhancedJobSystem plugin;
    private final JobType job;

    public QuestGUI(EnhancedJobSystem plugin, JobType job) {
        this.plugin = plugin;
        this.job    = job;
    }

    public void open(Player player) {
        String title = TITLE_PREFIX + job.getDisplayName();
        Inventory inv = Bukkit.createInventory(null, 54, title);

        fillBorder(inv);

        plugin.getQuestManager().checkAndReset(player);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        List<ActiveQuest> quests = data.getActiveQuestsForJob(job.name());

        // Quest item slots
        int[] questSlots = {10, 13, 16, 28, 31, 34};
        for (int i = 0; i < quests.size() && i < questSlots.length; i++) {
            inv.setItem(questSlots[i], buildQuestItem(quests.get(i)));
        }

        // No quests placeholder
        if (quests.isEmpty()) {
            inv.setItem(22, buildNoQuestsItem());
        }

        // 24h reset timer in bottom-centre (slot 49)
        inv.setItem(49, buildTimerItem(data));

        // Back button (slot 45)
        inv.setItem(45, buildBackButton());

        // Daily slots remaining (slot 53)
        inv.setItem(53, buildSlotsItem(data));

        player.openInventory(inv);
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildQuestItem(ActiveQuest quest) {
        Material mat = quest.isCompleted() ? Material.LIME_WOOL : Material.YELLOW_WOOL;
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();

        String statusTag = quest.isCompleted() ? " §a[COMPLETE]" : "";
        meta.setDisplayName("§e§l" + quest.getDescription() + statusTag);

        List<String> lore = new ArrayList<>();
        lore.add("");

        if (!quest.isCompleted()) {
            double pct    = quest.getProgressPercent();
            String bar    = XPUtils.buildProgressBar(quest.getProgress(), quest.getTarget(), 20);
            lore.add("§7Progress: §f" + quest.getProgress() + " §7/ §f" + quest.getTarget()
                    + " §7(" + (int) pct + "%)");
            lore.add("  " + bar);
        } else {
            lore.add("§aCompleted! Well done.");
        }

        lore.add("");
        lore.add("§6Reward: §e+" + String.format("%.0f", quest.getXpReward()) + " XP");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildTimerItem(PlayerData data) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta  meta = item.getItemMeta();

        long remaining = data.getMillisUntilReset();
        meta.setDisplayName("§b§lDaily Quest Reset");

        List<String> lore = new ArrayList<>();
        lore.add("");
        if (remaining <= 0) {
            lore.add("§aQuests are ready to reset!");
            lore.add("§7(Open your quests to trigger reset)");
        } else {
            lore.add("§7Time until reset:");
            lore.add("§e" + XPUtils.formatDuration(remaining));
            lore.add("");
            lore.add("§7You receive §e" + plugin.getConfig().getInt("daily-quests", 3)
                    + " free quests §7per day.");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNoQuestsItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName("§c§lNo Active Quests");
        meta.setLore(List.of(
                "",
                "§7Your daily quests will reset soon.",
                "§7Check the timer at the bottom."
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName("§7◀ Back to Jobs");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSlotsItem(PlayerData data) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName("§b§lQuest Slots");
        meta.setLore(List.of(
                "",
                "§7Remaining today: §e" + data.getDailyQuestsRemaining(),
                "§7(Shared across all jobs)"
        ));
        item.setItemMeta(meta);
        return item;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void fillBorder(Inventory inv) {
        ItemStack glass = makeGlass();
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
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
