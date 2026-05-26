package com.herenavy.herenavy.config;

import com.herenavy.herenavy.HereNavyPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public final class ConfigManager {

    private final HereNavyPlugin plugin;
    
    // Configured values
    private boolean skyrimCompassEnabled;
    private String skyrimCompassStyle;
    private boolean tomtomArrowEnabled;
    private String arrowStyle;
    
    private int maxLevel;
    private int expPerBiome;
    private int expPerStructure;
    
    private final Map<Integer, List<String>> levelUnlocks = new HashMap<>();
    private String rewardItem;
    private String rewardName;

    public ConfigManager(HereNavyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Visual options
        this.skyrimCompassEnabled = config.getBoolean("navigation-visuals.skyrim-compass", true);
        this.skyrimCompassStyle = config.getString("navigation-visuals.skyrim-compass-style", "ACTIONBAR").toUpperCase();
        this.tomtomArrowEnabled = config.getBoolean("navigation-visuals.tomtom-arrow", true);
        this.arrowStyle = config.getString("navigation-visuals.arrow-style", "TEXT_DISPLAY").toUpperCase();

        // Progression options
        this.maxLevel = config.getInt("max-level", 100);
        this.expPerBiome = config.getInt("exp-per-new-biome", 50);
        this.expPerStructure = config.getInt("exp-per-new-structure", 500);

        // Load unlocks
        levelUnlocks.clear();
        ConfigurationSection unlocksSection = config.getConfigurationSection("unlocks");
        if (unlocksSection != null) {
            for (String key : unlocksSection.getKeys(false)) {
                if (key.startsWith("level_")) {
                    try {
                        int level = Integer.parseInt(key.substring(6));
                        if (level == 100) {
                            this.rewardItem = unlocksSection.getString("level_100.reward_item", "ELYTRA");
                            this.rewardName = unlocksSection.getString("level_100.reward_name", "<gold><bold>World Explorer</bold></gold>");
                        } else {
                            List<String> list = unlocksSection.getStringList(key);
                            levelUnlocks.put(level, list);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid level format in config unlocks: " + key);
                    }
                }
            }
        }
    }

    public boolean isSkyrimCompassEnabled() {
        return skyrimCompassEnabled;
    }

    public String getSkyrimCompassStyle() {
        return skyrimCompassStyle;
    }

    public boolean isTomtomArrowEnabled() {
        return tomtomArrowEnabled;
    }

    public String getArrowStyle() {
        return arrowStyle;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getExpPerBiome() {
        return expPerBiome;
    }

    public int getExpPerStructure() {
        return expPerStructure;
    }

    public Map<Integer, List<String>> getLevelUnlocks() {
        return Collections.unmodifiableMap(levelUnlocks);
    }

    public String getRewardItem() {
        return rewardItem;
    }

    public String getRewardName() {
        return rewardName;
    }

    /**
     * Checks if a specific biome or structure is unlocked for a given level
     */
    public boolean isUnlocked(String key, int playerLevel) {
        // Lowercase for comparison
        String normalizedKey = key.toLowerCase();
        
        // Find highest level requirement matched
        for (Map.Entry<Integer, List<String>> entry : levelUnlocks.entrySet()) {
            int requiredLevel = entry.getKey();
            List<String> items = entry.getValue();
            
            for (String item : items) {
                if (item.toLowerCase().equals(normalizedKey)) {
                    return playerLevel >= requiredLevel;
                }
            }
        }
        
        // By default, if not listed in level requirements, it requires Level 1 (fully unlocked)
        return true;
    }

    /**
     * Get the required level for tracking a specific key
     */
    public int getRequiredLevel(String key) {
        String normalizedKey = key.toLowerCase();
        for (Map.Entry<Integer, List<String>> entry : levelUnlocks.entrySet()) {
            int requiredLevel = entry.getKey();
            for (String item : entry.getValue()) {
                if (item.toLowerCase().equals(normalizedKey)) {
                    return requiredLevel;
                }
            }
        }
        return 1; // Default
    }
}
