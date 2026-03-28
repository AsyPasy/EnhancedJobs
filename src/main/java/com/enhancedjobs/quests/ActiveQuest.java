package com.enhancedjobs.quests;

/**
 * Represents a quest currently assigned to a player.
 * Created by {@link QuestTemplate#generate()}.
 */
public class ActiveQuest {

    private final String templateId;
    private final String description;
    private final String jobType;
    private final QuestTaskType taskType;
    private final int target;
    private final double xpReward;

    private int progress;
    private boolean completed;
    private boolean rewardClaimed;

    public ActiveQuest(String templateId, String description, String jobType,
                       QuestTaskType taskType, int target, double xpReward) {
        this.templateId  = templateId;
        this.description = description;
        this.jobType     = jobType;
        this.taskType    = taskType;
        this.target      = target;
        this.xpReward    = xpReward;
        this.progress    = 0;
        this.completed   = false;
        this.rewardClaimed = false;
    }

    /**
     * Add progress toward this quest.
     * @param amount how many units to add
     * @return true if the quest just completed from this addition
     */
    public boolean addProgress(int amount) {
        if (completed) return false;
        progress = Math.min(progress + amount, target);
        if (progress >= target) {
            completed = true;
            return true;
        }
        return false;
    }

    /** Returns progress as a percentage (0–100). */
    public int getProgressPercent() {
        if (target <= 0) return 100;
        return (int) Math.min(100, (progress * 100.0) / target);
    }

    /** Builds a short display line, e.g. "Harvest 2500 wheat (1200/2500)". */
    public String getSummary() {
        String status = completed ? " §a[DONE]" : " §7(" + progress + "/" + target + ")";
        return "§e" + description + status;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getTemplateId()    { return templateId;    }
    public String getDescription()   { return description;   }
    public String getJobType()       { return jobType;       }
    public QuestTaskType getTaskType(){ return taskType;     }
    public int getTarget()           { return target;        }
    public double getXpReward()      { return xpReward;      }
    public int getProgress()         { return progress;      }
    public void setProgress(int p)   { this.progress = p;    }
    public boolean isCompleted()     { return completed;     }
    public void setCompleted(boolean c){ this.completed = c; }
    public boolean isRewardClaimed() { return rewardClaimed; }
    public void setRewardClaimed(boolean r){ this.rewardClaimed = r; }
}
