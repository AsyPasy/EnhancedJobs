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

import java.util.List;

/**
 * Job info / hub GUI. Opened when the player clicks a job they already have
 * in {@link JobSelectionGUI}. Provides navigation to Quests, Rewards, and a
 * Leave Job button.
 *
 * Layout (27 slots / 3 rows):
 *   Slot 11 – Quests
 *   Slot 13 – XP / level info (decorative)
 *   Slot 15 – Rewards
 *   Slot 22 – Leave Job (red)
 *   Slot 18 – Back
 */
public class JobInfoGUI {

    public static final String TITLE_PREFIX = "§8§l";

    private final EnhancedJobSystem plugin;
    private final JobType job;

    public JobInfoGUI(EnhancedJobSystem plugin, JobType job) {
        this.plugin = plugin;
        this.job    = job;
    }

    public void open(Player player) {
        String title = TITLE_PREFIX + job.getDisplayName() + " Job";
        Inventory inv = Bukkit.createInventory(null, 27, title);
        fillBorder(inv);

        PlayerData data  = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int level        = data.getLevel(job.name());
        double xp        = data.getXP(job.name());
        double needed    = XPUtils.getXPForLevel(level);
        int questCount   = data.getActiveQuestsForJob(job.name()).size();
        long resetMs     = data.getMillisUntilReset();

        // Quests button
        ItemStack questItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta  qMeta = questItem.getItemMeta();
        qMeta.setDisplayName("§e§lDaily Quests");
        qMeta.setLore(List.of(
                "",
                "§7Active quests: §e" + questCount,
                "§7Reset in: §b" + XPUtils.formatDuration(resetMs),
                "",
                "§e▶ Click to view quests"
        ));
        questItem.setItemMeta(qMeta);
        inv.setItem(11, questItem);

        // Level / XP info (decorative)
        ItemStack xpItem = new ItemStack(job.getIcon());
        ItemMeta  xMeta  = xpItem.getItemMeta();
        xMeta.setDisplayName(job.getPrimaryColor() + "§l" + job.getDisplayName()
                + " §7– Level §f" + level);
        if (level < XPUtils.MAX_LEVEL) {
            xMeta.setLore(List.of(
                    "",
                    "§7XP: §f" + String.format("%.1f", xp) + " §7/ §f"
                            + String.format("%.0f", needed),
                    "   " + XPUtils.buildProgressBar(xp, needed, 20),
                    "",
                    "§8" + job.getDescription()
            ));
        } else {
            xMeta.setLore(List.of("", "§6§lMAX LEVEL!", "", "§8" + job.getDescription()));
        }
        xpItem.setItemMeta(xMeta);
        inv.setItem(13, xpItem);

        // Rewards button
        ItemStack rewardItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta  rMeta = rewardItem.getItemMeta();
        rMeta.setDisplayName("§d§lRewards");
        rMeta.setLore(List.of(
                "",
                "§7View and claim level rewards.",
                "",
                "§d▶ Click to view rewards"
        ));
        rewardItem.setItemMeta(rMeta);
        inv.setItem(15, rewardItem);

        // Leave job
        ItemStack leaveItem = new ItemStack(Material.BARRIER);
        ItemMeta  lMeta = leaveItem.getItemMeta();
        lMeta.setDisplayName("§c§lLeave Job");
        lMeta.setLore(List.of(
                "",
                "§7Your XP and level will be saved.",
                "§c▶ Shift-click to confirm"
        ));
        leaveItem.setItemMeta(lMeta);
        inv.setItem(22, leaveItem);

        // Back
        inv.setItem(18, buildBackButton());

        player.openInventory(inv);
    }

    private ItemStack buildBackButton() {
        ItemStack i = new ItemStack(Material.ARROW);
        ItemMeta  m = i.getItemMeta();
        m.setDisplayName("§7◀ Back to Jobs");
        i.setItemMeta(m);
        return i;
    }

    private void fillBorder(Inventory inv) {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  m = g.getItemMeta();
        m.setDisplayName("§r");
        g.setItemMeta(m);
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) inv.setItem(i, g);
        }
    }
}
