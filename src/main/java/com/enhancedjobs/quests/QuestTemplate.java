package com.enhancedjobs.quests;

import java.util.Random;

/**
 * A QuestTemplate defines the parameters for a quest.
 * Calling {@link #generate()} creates an {@link ActiveQuest} with a
 * randomly chosen amount and linearly-scaled XP reward.
 */
public class QuestTemplate {

    private static final Random RANDOM = new Random();

    private final String id;
    private final String jobType;
    private final QuestTaskType taskType;
    private final String descriptionTemplate; // e.g. "Harvest {amount} wheat"
    private final int minAmount;
    private final int maxAmount;
    private final double minXP;
    private final double maxXP;

    public QuestTemplate(String id, String jobType, QuestTaskType taskType,
                         String descriptionTemplate,
                         int minAmount, int maxAmount,
                         double minXP, double maxXP) {
        this.id                  = id;
        this.jobType             = jobType;
        this.taskType            = taskType;
        this.descriptionTemplate = descriptionTemplate;
        this.minAmount           = minAmount;
        this.maxAmount           = maxAmount;
        this.minXP               = minXP;
        this.maxXP               = maxXP;
    }

    /**
     * Generates a new {@link ActiveQuest} with a random target amount and
     * proportionally scaled XP reward.
     */
    public ActiveQuest generate() {
        int amount;
        if (minAmount == maxAmount) {
            amount = minAmount;
        } else {
            amount = minAmount + RANDOM.nextInt(maxAmount - minAmount + 1);
        }

        double ratio  = (maxAmount == minAmount) ? 1.0
                        : (double)(amount - minAmount) / (maxAmount - minAmount);
        double xpReward = minXP + ratio * (maxXP - minXP);
        xpReward = Math.round(xpReward * 10.0) / 10.0; // round to 1dp

        String description = descriptionTemplate.replace("{amount}", String.valueOf(amount));
        return new ActiveQuest(id, description, jobType, taskType, amount, xpReward);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId()             { return id;          }
    public String getJobType()        { return jobType;     }
    public QuestTaskType getTaskType(){ return taskType;    }
    public String getDescriptionTemplate() { return descriptionTemplate; }
    public int getMinAmount()         { return minAmount;   }
    public int getMaxAmount()         { return maxAmount;   }
    public double getMinXP()          { return minXP;       }
    public double getMaxXP()          { return maxXP;       }
}
