package com.enhancedjobs.data;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.quests.ActiveQuest;
import com.enhancedjobs.quests.QuestTaskType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Handles loading and saving {@link PlayerData} to per-player YAML files
 * in {@code plugins/EnhancedJobSystem/playerdata/}.
 */
public class DataManager {

    private final EnhancedJobSystem plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public DataManager(EnhancedJobSystem plugin) {
        this.plugin     = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns cached data or loads from disk. Never returns null. */
    public PlayerData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    /** Saves one player's data to disk. */
    public void savePlayerData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) save(data);
    }

    /** Saves all cached player data to disk (called on plugin disable). */
    public void saveAll() {
        for (PlayerData data : cache.values()) save(data);
    }

    /** Removes player from cache (called on quit to free memory after saving). */
    public void unload(UUID uuid) {
        savePlayerData(uuid);
        cache.remove(uuid);
    }

    // ── Internal load / save ─────────────────────────────────────────────────

    private PlayerData load(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        PlayerData data = new PlayerData(uuid);
        if (!file.exists()) return data;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Active jobs
        List<String> jobs = cfg.getStringList("activeJobs");
        for (String j : jobs) data.getActiveJobs().add(j);

        // Job levels & XP
        if (cfg.isConfigurationSection("jobLevels")) {
            for (String key : cfg.getConfigurationSection("jobLevels").getKeys(false)) {
                data.getJobLevels().put(key, cfg.getInt("jobLevels." + key, 1));
            }
        }
        if (cfg.isConfigurationSection("jobXP")) {
            for (String key : cfg.getConfigurationSection("jobXP").getKeys(false)) {
                data.getJobXP().put(key, cfg.getDouble("jobXP." + key, 0.0));
            }
        }

        // Claimed rewards
        List<String> rewards = cfg.getStringList("claimedRewards");
        data.getClaimedRewards().addAll(rewards);

        // Quest state
        data.setLastQuestReset(cfg.getLong("quest.lastReset", System.currentTimeMillis()));
        data.setDailyQuestsRemaining(cfg.getInt("quest.remaining", 3));

        // Active quests
        if (cfg.isList("quest.active")) {
            List<Map<?, ?>> rawQuests = cfg.getMapList("quest.active");
            for (Map<?, ?> m : rawQuests) {
                try {
                    String templateId   = (String) m.get("templateId");
                    String description  = (String) m.get("description");
                    String jobType      = (String) m.get("jobType");
                    QuestTaskType type  = QuestTaskType.valueOf((String) m.get("taskType"));
                    int    target       = (int) m.get("target");
                    double xpReward     = ((Number) m.get("xpReward")).doubleValue();
                    int    progress     = (int) m.get("progress");
                    boolean completed   = (boolean) m.get("completed");
                    boolean rewarded    = m.containsKey("rewardClaimed") && (boolean) m.get("rewardClaimed");

                    ActiveQuest aq = new ActiveQuest(templateId, description, jobType, type, target, xpReward);
                    aq.setProgress(progress);
                    aq.setCompleted(completed);
                    aq.setRewardClaimed(rewarded);
                    data.getActiveQuests().add(aq);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load a quest entry: " + e.getMessage());
                }
            }
        }

        // AutoFarm data
        data.getAutoFarmData().addAll(cfg.getStringList("autoFarms"));

        // MoneyTree data
        data.getMoneyTreeData().addAll(cfg.getStringList("moneyTrees"));
        data.setMoneyTreeSeedsClaimed(cfg.getBoolean("moneyTreeSeedsClaimed", false));

        return data;
    }

    private void save(PlayerData data) {
        File file = new File(dataFolder, data.getPlayerUUID() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();

        cfg.set("activeJobs", new ArrayList<>(data.getActiveJobs()));

        for (Map.Entry<String, Integer> e : data.getJobLevels().entrySet())
            cfg.set("jobLevels." + e.getKey(), e.getValue());

        for (Map.Entry<String, Double> e : data.getJobXP().entrySet())
            cfg.set("jobXP." + e.getKey(), e.getValue());

        cfg.set("claimedRewards", new ArrayList<>(data.getClaimedRewards()));

        cfg.set("quest.lastReset",  data.getLastQuestReset());
        cfg.set("quest.remaining",  data.getDailyQuestsRemaining());

        List<Map<String, Object>> questList = new ArrayList<>();
        for (ActiveQuest aq : data.getActiveQuests()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("templateId",    aq.getTemplateId());
            m.put("description",   aq.getDescription());
            m.put("jobType",       aq.getJobType());
            m.put("taskType",      aq.getTaskType().name());
            m.put("target",        aq.getTarget());
            m.put("xpReward",      aq.getXpReward());
            m.put("progress",      aq.getProgress());
            m.put("completed",     aq.isCompleted());
            m.put("rewardClaimed", aq.isRewardClaimed());
            questList.add(m);
        }
        cfg.set("quest.active", questList);

        cfg.set("autoFarms",   new ArrayList<>(data.getAutoFarmData()));
        cfg.set("moneyTrees",  new ArrayList<>(data.getMoneyTreeData()));
        cfg.set("moneyTreeSeedsClaimed", data.isMoneyTreeSeedsClaimed());

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + data.getPlayerUUID(), e);
        }
    }
}
