package com.enhancedjobs.jobs.farmer;

import com.enhancedjobs.jobs.Job;

public class FarmerJob implements Job {

    public static final String ID = "FARMER";

    @Override public String getId()          { return ID; }
    @Override public String getDisplayName() { return "§2🌾 Farmer"; }
    @Override public String getDescription() { return "§7Grow crops, raise animals, and master the land."; }
}
