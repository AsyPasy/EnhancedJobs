package com.enhancedjobs;

import com.enhancedjobs.commands.JobsAdminCommand;
import com.enhancedjobs.commands.JobsCommand;
import com.enhancedjobs.data.PlayerDataManager;
import com.enhancedjobs.gui.GUIListener;
import com.enhancedjobs.jobs.JobManager;
import com.enhancedjobs.jobs.farmer.AutomaticFarmManager;
import com.enhancedjobs.jobs.farmer.FarmerListener;
import com.enhancedjobs.jobs.farmer.FarmerRewardManager;
import com.enhancedjobs.jobs.farmer.MoneyTreeManager;
import com.enhancedjobs.quests.QuestManager;
import com.enhancedjobs.utils.GoldManager;
import com.enhancedjobs.utils.ItemUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class EnhancedJobSystem extends JavaPlugin {

    private static EnhancedJobSystem instance;

    private PlayerDataManager    playerDataManager;
    private JobManager           jobManager;
    private QuestManager         questManager;
    private GoldManager          goldManager;
    private FarmerRewardManager  farmerRewardManager;
    private AutomaticFarmManager automaticFarmManager;
    private MoneyTreeManager     moneyTreeManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        ItemUtils.init(this);

        this.playerDataManager    = new PlayerDataManager(this);
        this.goldManager          = new GoldManager(this);
        this.questManager         = new QuestManager(this);
        this.jobManager           = new JobManager(this);
        this.farmerRewardManager  = new FarmerRewardManager(this);
        this.automaticFarmManager = new AutomaticFarmManager(this);
        this.moneyTreeManager     = new MoneyTreeManager(this);

        // Scarecrow recipe removed

        getCommand("jobs").setExecutor(new JobsCommand(this));
        getCommand("jobsadmin").setExecutor(new JobsAdminCommand(this));

        getServer().getPluginManager().registerEvents(new PluginListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmerListener(this), this);

        getLogger().info("EnhancedJobSystem v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null)    playerDataManager.saveAllData();
        if (automaticFarmManager != null) automaticFarmManager.saveData();
        if (moneyTreeManager != null)     moneyTreeManager.saveData();
        getLogger().info("EnhancedJobSystem disabled.");
    }

    public static EnhancedJobSystem getInstance()         { return instance; }
    public PlayerDataManager getPlayerDataManager()       { return playerDataManager; }
    public JobManager getJobManager()                     { return jobManager; }
    public QuestManager getQuestManager()                 { return questManager; }
    public GoldManager getGoldManager()                   { return goldManager; }
    public FarmerRewardManager getFarmerRewardManager()   { return farmerRewardManager; }
    public AutomaticFarmManager getAutomaticFarmManager() { return automaticFarmManager; }
    public MoneyTreeManager getMoneyTreeManager()         { return moneyTreeManager; }
}
