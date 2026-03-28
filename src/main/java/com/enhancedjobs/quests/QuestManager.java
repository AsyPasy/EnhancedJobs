package com.enhancedjobs.quests;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class QuestManager {

    private final EnhancedJobSystem plugin;
    private final Random random = new Random();

    public static final int FREE_DAILY_QUESTS = 3;
    public static final int MAX_DAILY_QUESTS  = 9;

    public QuestManager(EnhancedJobSystem plugin) {
        this.plugin = plugin;
    }

    public void checkAndResetDailyQuests(PlayerData data, String job) {
        long lastReset = data.getLastQuestReset(job);
        if (lastReset == 0) return;
        long resetHours = plugin.getConfig().getLong("quest-reset-hours", 24);
        if (System.currentTimeMillis() - lastReset >= TimeUnit.HOURS.toMillis(resetHours)) {
            data.resetDailyQuests(job);
            data.setActiveQuests(job, List.of());
        }
    }

    public double getNextQuestCost(PlayerData data, String job) {
        checkAndResetDailyQuests(data, job);
        int count = data.getDailyQuestCount(job);
        if (count >= MAX_DAILY_QUESTS) return -1; // signals "limit reached"
        if (count < FREE_DAILY_QUESTS) return 0;
        double base = plugin.getConfig().getDouble("base-extra-quest-cost", 15.0);
        return base * Math.pow(2, count - FREE_DAILY_QUESTS);
    }

    public boolean purchaseQuest(Player player, PlayerData data, String job) {
        checkAndResetDailyQuests(data, job);

        if (data.getDailyQuestCount(job) >= MAX_DAILY_QUESTS) {
            player.sendMessage("§cYou've reached the daily quest limit of §e" + MAX_DAILY_QUESTS + "§c!");
            return false;
        }

        double cost = getNextQuestCost(data, job);
        if (cost > 0 && !data.removeGold(cost)) {
            player.sendMessage("§cYou need §e" + (int) cost + " gold coins §cfor another quest!");
            return false;
        }

        Quest quest = generateFarmerQuest();
        List<Quest> current = new ArrayList<>(data.getActiveQuests(job));
        current.add(quest);
        data.setActiveQuests(job, current);
        data.incrementDailyQuestCount(job);

        if (data.getLastQuestReset(job) == 0)
            data.setLastQuestReset(job, System.currentTimeMillis());

        player.sendMessage("§aNew quest assigned: §e" + quest.getDisplayName()
            + " §7(+" + (int) quest.getXpReward() + " XP on completion)");
        return true;
    }

    public void updateQuestProgress(Player player, PlayerData data, String job,
                                    QuestType type, int amount) {
        for (Quest q : data.getActiveQuests(job)) {
            if (q.getType() == type && !q.isCompleted()) {
                q.incrementProgress(amount);
                if (q.isCompleted()) {
                    data.addJobXP(job, q.getXpReward());
                    player.sendMessage("§a✔ Quest complete: §e" + q.getDisplayName()
                        + " §7(+" + (int) q.getXpReward() + " XP)");
                    plugin.getJobManager().checkLevelUp(player, data, job);
                }
                break;
            }
        }
    }

    public Quest generateFarmerQuest() {
        QuestType[] types = QuestType.values();
        return buildFarmerQuest(types[random.nextInt(types.length)]);
    }

    public Quest buildFarmerQuest(QuestType type) {
        return switch (type) {
            case HARVEST_WHEAT       -> scaled(type, 1000,  5000,  100,  600);
            case BREED_COWS          -> scaled(type, 10,    50,    100,  300);
            case PLANT_CROPS         -> scaled(type, 500,   7500,  100,  600);
            case COLLECT_EGGS        -> scaled(type, 50,    250,   200, 1000);
            case MILK_COWS           -> scaled(type, 50,    300,   50,   300);
            case EAT_PRODUCE         -> scaled(type, 40,    650,   30,   600);
            case HARVEST_CARROTS     -> scaled(type, 500,   2500,  200,  900);
            case SELL_CROPS          -> scaled(type, 1000,  10000, 100, 1000);
            case HARVEST_PUMPKIN     -> scaled(type, 100,   500,   150,  700);
            case HARVEST_MELON       -> scaled(type, 200,   1000,  150,  700);
            case COLLECT_MUSHROOMS   -> scaled(type, 100,   500,   100,  400);
            case HARVEST_SUGARCANE   -> scaled(type, 200,   2000,  100,  600);
            case HARVEST_NETHER_WART -> scaled(type, 100,   1000,  150,  500);
            case TILL_DIRT           -> scaled(type, 100,   500,   100,  400);
        };
    }

    private Quest scaled(QuestType type, int minTarget, int maxTarget,
                         double minXP, double maxXP) {
        int target = minTarget + random.nextInt(maxTarget - minTarget + 1);
        double ratio = (double)(target - minTarget) / (maxTarget - minTarget);
        return new Quest(type, target, Math.round(minXP + ratio * (maxXP - minXP)));
    }
}
