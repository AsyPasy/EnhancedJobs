package com.enhancedjobs.quests;

public class Quest {

    private final QuestType type;
    private final int       target;
    private final double    xpReward;
    private int             progress  = 0;
    private boolean         completed = false;

    public Quest(QuestType type, int target, double xpReward) {
        this.type     = type;
        this.target   = target;
        this.xpReward = xpReward;
    }

    public QuestType getType()     { return type; }
    public int       getTarget()   { return target; }
    public double    getXpReward() { return xpReward; }
    public int       getProgress() { return progress; }
    public boolean   isCompleted() { return completed; }

    public void setProgress(int p) {
        this.progress = Math.min(p, target);
        if (this.progress >= target) this.completed = true;
    }

    public void incrementProgress(int amount) {
        setProgress(this.progress + amount);
    }

    public String getDisplayName() {
        return switch (type) {
            case HARVEST_WHEAT       -> "Harvest "  + target + " Wheat";
            case BREED_COWS          -> "Breed "    + target + " Cows";
            case PLANT_CROPS         -> "Plant "    + target + " Crops";
            case COLLECT_EGGS        -> "Collect "  + target + " Eggs";
            case MILK_COWS           -> "Milk "     + target + " Cows";
            case EAT_PRODUCE         -> "Eat "      + target + " Melons/Apples/Carrots";
            case HARVEST_CARROTS     -> "Harvest "  + target + " Carrots";
            case SELL_CROPS          -> "Sell "     + target + " Crops to a Vendor";
            case HARVEST_PUMPKIN     -> "Harvest "  + target + " Pumpkins";
            case HARVEST_MELON       -> "Harvest "  + target + " Melons";
            case COLLECT_MUSHROOMS   -> "Collect "  + target + " Mushrooms";
            case HARVEST_SUGARCANE   -> "Harvest "  + target + " Sugarcane";
            case HARVEST_NETHER_WART -> "Harvest "  + target + " Nether Wart";
            case TILL_DIRT           -> "Till "     + target + " Dirt Blocks";
        };
    }

    public String getProgressBar() {
        int bars   = 20;
        int filled = (int) ((progress / (double) target) * bars);
        return "§a" + "█".repeat(filled) + "§8" + "█".repeat(bars - filled)
             + " §e" + progress + "§7/§e" + target;
    }
}
