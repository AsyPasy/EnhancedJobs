package com.enhancedjobs.data;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.quests.Quest;
import com.enhancedjobs.quests.QuestType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.*;

public class PlayerDataManager {

    private final EnhancedJobSystem plugin;
    private final File playersDir;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public PlayerDataManager(EnhancedJobSystem plugin) {
        this.plugin     = plugin;
        this.playersDir = new File(plugin.getDataFolder(), "players");
        if (!playersDir.exists()) playersDir.mkdirs();
    }

    public PlayerData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public void saveAllData() { cache.keySet().forEach(this::save); }

    public void unloadPlayer(UUID uuid) { save(uuid); cache.remove(uuid); }

    // ── Load ──────────────────────────────────────────────────────────────────

    private PlayerData load(UUID uuid) {
        File      file = new File(playersDir, uuid + ".yml");
        PlayerData data = new PlayerData(uuid);
        if (!file.exists()) return data;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Internal gold — only meaningful when GoldEconomy is absent
        data.setInternalGold(cfg.getDouble("gold", 0.0));

        for (String job : cfg.getStringList("active-jobs"))
            data.joinJob(job);

        ConfigurationSection levels = cfg.getConfigurationSection("levels");
        if (levels != null)
            for (String k : levels.getKeys(false))
                data.setJobLevel(k, levels.getInt(k));

        ConfigurationSection xpSec = cfg.getConfigurationSection("xp");
        if (xpSec != null)
            for (String k : xpSec.getKeys(false))
                data.setJobXP(k, xpSec.getDouble(k));

        ConfigurationSection dqc = cfg.getConfigurationSection("daily-quest-count");
        if (dqc != null)
            for (String k : dqc.getKeys(false))
                data.setDailyQuestCount(k, dqc.getInt(k));

        ConfigurationSection lqr = cfg.getConfigurationSection("last-quest-reset");
        if (lqr != null)
            for (String k : lqr.getKeys(false))
                data.setLastQuestReset(k, lqr.getLong(k));

        ConfigurationSection rewards = cfg.getConfigurationSection("claimed-rewards");
        if (rewards != null)
            for (String job : rewards.getKeys(false))
                for (String r : rewards.getStringList(job))
                    data.claimReward(job, r);

        ConfigurationSection quests = cfg.getConfigurationSection("active-quests");
        if (quests != null) {
            for (String job : quests.getKeys(false)) {
                List<Quest> list = new ArrayList<>();
                ConfigurationSection qs = quests.getConfigurationSection(job);
                if (qs != null) {
                    for (String qk : qs.getKeys(false)) {
                        ConfigurationSection q = qs.getConfigurationSection(qk);
                        if (q != null) {
                            try {
                                QuestType type  = QuestType.valueOf(q.getString("type", "HARVEST_WHEAT"));
                                int       target = q.getInt("target");
                                double    xp     = q.getDouble("xp-reward");
                                Quest     quest  = new Quest(type, target, xp);
                                quest.setProgress(q.getInt("current"));
                                list.add(quest);
                            } catch (Exception ignored) {}
                        }
                    }
                }
                data.setActiveQuests(job, list);
            }
        }
        return data;
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void save(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;

        File              file = new File(playersDir, uuid + ".yml");
        YamlConfiguration cfg  = new YamlConfiguration();

        // Save internal gold (fallback when GoldEconomy absent)
        cfg.set("gold", data.getInternalGold());
        cfg.set("active-jobs", new ArrayList<>(data.getActiveJobs()));

        data.getJobLevelsMap()    .forEach((k, v) -> cfg.set("levels."            + k, v));
        data.getJobXPMap()        .forEach((k, v) -> cfg.set("xp."                + k, v));
        data.getDailyQuestCounts().forEach((k, v) -> cfg.set("daily-quest-count." + k, v));
        data.getLastQuestResets() .forEach((k, v) -> cfg.set("last-quest-reset."  + k, v));

        data.getClaimedRewardsMap().forEach((job, set) ->
            cfg.set("claimed-rewards." + job, new ArrayList<>(set))
        );

        data.getActiveQuestsMap().forEach((job, list) -> {
            for (int i = 0; i < list.size(); i++) {
                Quest  q    = list.get(i);
                String path = "active-quests." + job + "." + i;
                cfg.set(path + ".type",      q.getType().name());
                cfg.set(path + ".target",    q.getTarget());
                cfg.set(path + ".current",   q.getProgress());
                cfg.set(path + ".xp-reward", q.getXpReward());
            }
        });

        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}
