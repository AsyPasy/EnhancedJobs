package com.enhancedjobs.jobs;

import com.enhancedjobs.EnhancedJobSystem;
import com.enhancedjobs.data.PlayerData;
import com.enhancedjobs.jobs.farmer.FarmerJob;
import com.enhancedjobs.utils.XPUtils;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class JobManager {

    private final EnhancedJobSystem plugin;
    private final Map<String, Job>  jobs = new LinkedHashMap<>();

    public JobManager(EnhancedJobSystem plugin) {
        this.plugin = plugin;
        registerJob(new FarmerJob());
    }

    public void registerJob(Job job) {
        jobs.put(job.getId().toUpperCase(), job);
    }

    public Job getJob(String id) {
        return jobs.get(id.toUpperCase());
    }

    public Collection<Job> getAllJobs() {
        return jobs.values();
    }

    // ── Level-up logic ────────────────────────────────────────────────────────

    public void checkLevelUp(Player player, PlayerData data, String job) {
        String key   = job.toUpperCase();
        int    level = data.getJobLevel(key);
        if (level >= 10) return;

        double xp       = data.getJobXP(key);
        double required = XPUtils.xpRequired(level);

        while (xp >= required && level < 10) {
            xp    -= required;
            level++;
            data.setJobLevel(key, level);
            data.setJobXP(key, xp);
            player.sendMessage("§6★ §eLEVEL UP! §6" + getJob(key).getDisplayName()
                    + " §eis now §6Level " + level + "§e!");
            if (level % 2 == 0)
                player.sendMessage("§aNew rewards are available! Open §e/jobs §ato claim them.");
            required = XPUtils.xpRequired(level);
        }
        if (level < 10) data.setJobXP(key, xp);
    }
}
