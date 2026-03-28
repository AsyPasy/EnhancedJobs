package com.enhancedjobs.jobs;

import org.bukkit.Material;

/**
 * Enum of all available job types.
 * Future professions can be added here; each needs a matching quest pool and listener.
 */
public enum JobType {

    FARMER("Farmer", Material.WHEAT, "§2", "§a",
            "A skilled agriculturalist who tills the land and tends to livestock."),
    // Future jobs – add quest pools & listeners when details are provided:
    // MINER("Miner",     Material.IRON_PICKAXE, "§8", "§7", "..."),
    // FISHER("Fisher",   Material.FISHING_ROD,  "§1", "§3", "..."),
    ;

    private final String displayName;
    private final Material icon;
    private final String primaryColor;
    private final String secondaryColor;
    private final String description;

    JobType(String displayName, Material icon,
            String primaryColor, String secondaryColor, String description) {
        this.displayName   = displayName;
        this.icon          = icon;
        this.primaryColor  = primaryColor;
        this.secondaryColor = secondaryColor;
        this.description   = description;
    }

    public String getDisplayName()   { return displayName;    }
    public Material getIcon()        { return icon;           }
    public String getPrimaryColor()  { return primaryColor;   }
    public String getSecondaryColor(){ return secondaryColor; }
    public String getDescription()   { return description;    }

    /** Safe lookup that returns null if not found. */
    public static JobType fromString(String name) {
        if (name == null) return null;
        try { return JobType.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
