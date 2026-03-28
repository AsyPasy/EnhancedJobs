package com.enhancedjobs.gui.farmer;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.jobs.farmer.FarmerJob;
import com.enhancedjobs.quests.Quest;
import com.enhancedjobs.quests.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FarmerQuestsGUI {

    public static final String TITLE = "§8§l✦ §r§eFarmer Quests §8§l✦";

    private FarmerQuestsGUI() {}

    public static void open(EnhancedJobSystem plugin, Player player) {
        Inventory  inv  = Bukkit.createInventory(null, 54, TITLE);
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        fillBorder(inv);

        List<Quest> quests     = data.getActiveQuests(FarmerJob.ID);
        int[]       questSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20};
        for (int i = 0; i < Math.min(quests.size(), questSlots.length); i++)
            inv.setItem(questSlots[i], buildQuestItem(quests.get(i)));

        plugin.getQuestManager().checkAndResetDailyQuests(data, FarmerJob.ID);
        double cost      = plugin.getQuestManager().getNextQuestCost(data, FarmerJob.ID);
        int    count     = data.getDailyQuestCount(FarmerJob.ID);
        long   lastReset = data.getLastQuestReset(FarmerJob.ID);
        int    freeLeft  = Math.max(0, QuestManager.FREE_DAILY_QUESTS - count);
        boolean atLimit  = count >= QuestManager.MAX_DAILY_QUESTS;

        String costLine;
        if (atLimit)      costLine = "§cDaily limit reached (" + QuestManager.MAX_DAILY_QUESTS + "/" + QuestManager.MAX_DAILY_QUESTS + ")";
        else if (cost == 0) costLine = "§aCost: FREE §7(" + freeLeft + " free remaining)";
        else              costLine = "§7Cost: §e" + (int) cost + " gold coins";

        String resetLine;
        if (lastReset == 0) {
            resetLine = "§7Resets: §aafter your first quest today";
        } else {
            long remaining = plugin.getConfig().getLong("quest-reset-hours", 24) * 3_600_000L
                             - (System.currentTimeMillis() - lastReset);
            if (remaining <= 0) resetLine = "§aResets: now (take a quest to refresh)";
            else resetLine = "§7Daily reset in: §e" + TimeUnit.MILLISECONDS.toHours(remaining)
                             + "h " + TimeUnit.MILLISECONDS.toMinutes(remaining) % 60 + "m";
        }

        String actionLine;
        if (atLimit)      actionLine = "§cYou've reached today's quest limit.";
        else if (cost == 0) actionLine = "§eClick to claim a free quest!";
        else              actionLine = "§eClick to purchase a quest.";

        ItemStack btn  = new ItemStack(atLimit ? Material.RED_DYE : Material.EMERALD);
        ItemMeta  meta = btn.getItemMeta();
        meta.setDisplayName(atLimit ? "§c§lQuest Limit Reached" : "§a§lGet New Quest");
        meta.setLore(List.of(
            costLine,
            "§7Quests today: §e" + count + "§7/§e" + QuestManager.MAX_DAILY_QUESTS,
            "§7Free quests per day: §e" + QuestManager.FREE_DAILY_QUESTS,
            resetLine,
            "",
            actionLine
        ));
        btn.setItemMeta(meta);
        inv.setItem(31, btn);

        inv.setItem(49, FarmerGUI.makeButton(Material.ARROW, "§7← Back",
            List.of("§7Return to Farmer menu.")));

        player.openInventory(inv);
    }

    private static ItemStack buildQuestItem(Quest q) {
        ItemStack item = new ItemStack(q.isCompleted() ? Material.LIME_STAINED_GLASS_PANE : Material.PAPER);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName((q.isCompleted() ? "§a§l✔ " : "§e") + q.getDisplayName());
        List<String> lore = new ArrayList<>();
        lore.add(q.getProgressBar());
        lore.add("§7XP Reward: §e+" + (int) q.getXpReward());
        if (q.isCompleted()) lore.add("§a§lCOMPLETED!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack glass = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta  m     = glass.getItemMeta();
        m.setDisplayName("§r");
        glass.setItemMeta(m);
        for (int i = 0; i < 54; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8)
                inv.setItem(i, glass);
        }
    }
}
