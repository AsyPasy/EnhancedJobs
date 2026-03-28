package com.enhancedjobs;

import com.enhancedjobs.api.GoldEconomyHook;
import com.enhancedjobs.commands.JobAdminCommand;
import com.enhancedjobs.commands.JobCommand;
import com.enhancedjobs.data.DataManager;
import com.enhancedjobs.farm.AutoFarmManager;
import com.enhancedjobs.items.CustomItems;
import com.enhancedjobs.jobs.JobManager;
import com.enhancedjobs.listeners.*;
import com.enhancedjobs.npc.VendorManager;
import com.enhancedjobs.npc.ZNPCSVendorListener;
import com.enhancedjobs.quests.QuestManager;
import com.enhancedjobs.rewards.RewardManager;
import com.enhancedjobs.tree.MoneyTreeManager;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * EnhancedJobSystem – main plugin entry point.
 *
 * Startup order:
 *   1. Load config
 *   2. Init custom items (registers scarecrow recipe)
 *   3. Init GoldEconomy hook (soft dep)
 *   4. Create DataManager
 *   5. Create all feature managers
 *   6. Register all event listeners
 *   7. Register commands
 *   8. Try to hook ZNPCS vendor listener (soft dep)
 */
public class EnhancedJobSystem extends JavaPlugin {

    // ── Managers (accessible to listeners/GUIs) ───────────────────────────────
    private DataManager      dataManager;
    private GoldEconomyHook  economyHook;
    private QuestManager     questManager;
    private JobManager       jobManager;
    private RewardManager    rewardManager;
    private AutoFarmManager  autoFarmManager;
    private MoneyTreeManager moneyTreeManager;
    private VendorManager    vendorManager;

    @Override
    public void onEnable() {
        // 1. Config
        saveDefaultConfig();

        // 2. Custom items & recipes
        CustomItems.init(this);

        // 3. Economy hook (GoldEconomy soft dep)
        economyHook = new GoldEconomyHook(this);

        // 4. Data persistence
        dataManager = new DataManager(this);

        // 5. Feature managers
        autoFarmManager  = new AutoFarmManager(this, dataManager);
        moneyTreeManager = new MoneyTreeManager(this, dataManager, economyHook);
        questManager     = new QuestManager(this, dataManager);
        jobManager       = new JobManager(this, dataManager);
        rewardManager    = new RewardManager(this, dataManager);
        vendorManager    = new VendorManager(this);

        // Start the AutoFarm harvest scheduler
        autoFarmManager.startScheduler();

        // 6. Event listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerSessionListener(this), this);
        pm.registerEvents(new FarmerListener(this),        this);
        pm.registerEvents(new RewardListener(this),        this);
        pm.registerEvents(new GUIListener(this),           this);

       // ZNPCS vendor listener (soft dep – only register if ZNPCS is loaded)
if (getServer().getPluginManager().getPlugin("ZNPCS") != null) {
    boolean hooked = ZNPCSVendorListener.register(this, vendorManager);
    if (hooked) {
        getLogger().info("ZNPCS vendor hook registered.");
    } else {
        getLogger().warning("ZNPCS found but vendor hook failed to register.");
    }
} else {
    getLogger().info("ZNPCS not found – vendor NPC feature disabled.");
}

        // 7. Commands
        var jobCmd = getCommand("job");
        if (jobCmd != null) {
            var exec = new JobCommand(this);
            jobCmd.setExecutor(exec);
            jobCmd.setTabCompleter(exec);
        }
        var adminCmd = getCommand("jobadmin");
        if (adminCmd != null) {
            var exec = new JobAdminCommand(this);
            adminCmd.setExecutor(exec);
            adminCmd.setTabCompleter(exec);
        }

        getLogger().info("EnhancedJobSystem v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        // Stop AutoFarm scheduler then save everything
        if (autoFarmManager != null) autoFarmManager.stopScheduler();
        if (dataManager     != null) dataManager.saveAll();
        getLogger().info("EnhancedJobSystem disabled, all data saved.");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public DataManager      getDataManager()     { return dataManager;      }
    public GoldEconomyHook  getEconomyHook()     { return economyHook;      }
    public QuestManager     getQuestManager()    { return questManager;     }
    public JobManager       getJobManager()      { return jobManager;       }
    public RewardManager    getRewardManager()   { return rewardManager;    }
    public AutoFarmManager  getAutoFarmManager() { return autoFarmManager;  }
    public MoneyTreeManager getMoneyTreeManager(){ return moneyTreeManager; }
    public VendorManager    getVendorManager()   { return vendorManager;    }
}
