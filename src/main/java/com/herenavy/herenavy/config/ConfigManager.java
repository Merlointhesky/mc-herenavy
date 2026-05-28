package com.herenavy.herenavy.config;

import com.herenavy.herenavy.HereNavyPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
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
    private final Set<String> untrackedStructures = new HashSet<>();
    private final Map<String, String> defaultIcons = new HashMap<>();

    public ConfigManager(HereNavyPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        
        File configFile = new java.io.File(plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            mergeMissingDefaults(configFile, "config.yml");
        }

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

        // Load untracked structures
        untrackedStructures.clear();
        List<String> untracked = config.getStringList("untracked-structures");
        if (untracked != null) {
            for (String s : untracked) {
                untrackedStructures.add(s.toLowerCase());
            }
        }

        // Load default structure icons
        loadDefaultIcons();
    }

    private void mergeMissingDefaults(java.io.File localFile, String resourcePath) {
        try {
            java.io.InputStream resourceStream = plugin.getResource(resourcePath);
            if (resourceStream == null) return;

            org.bukkit.configuration.file.YamlConfiguration jarConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(resourceStream, java.nio.charset.StandardCharsets.UTF_8)
            );

            org.bukkit.configuration.file.YamlConfiguration localConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(localFile);

            boolean modified = false;
            for (String key : jarConfig.getKeys(true)) {
                if (!localConfig.contains(key)) {
                    localConfig.set(key, jarConfig.get(key));
                    modified = true;
                } else if (jarConfig.isList(key) && localConfig.isList(key)) {
                    List<?> jarList = jarConfig.getList(key);
                    List<Object> localList = new ArrayList<>(localConfig.getList(key));
                    boolean listModified = false;
                    for (Object val : jarList) {
                        if (!localList.contains(val)) {
                            localList.add(val);
                            listModified = true;
                        }
                    }
                    if (listModified) {
                        localConfig.set(key, localList);
                        modified = true;
                    }
                }
            }

            if (modified) {
                localConfig.save(localFile);
                plugin.getLogger().info("Successfully merged new default configuration keys into " + localFile.getName() + " without altering existing entries.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to safely merge defaults for " + localFile.getName() + ": " + e.getMessage());
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

    /**
     * Checks if a structure is currently tracked by the server
     */
    public boolean isStructureTracked(String key) {
        return !untrackedStructures.contains(key.toLowerCase());
    }

    /**
     * Toggles structure tracking status and persists it to config.yml
     */
    public void setStructureTracked(String key, boolean tracked) {
        String normKey = key.toLowerCase();
        if (tracked) {
            untrackedStructures.remove(normKey);
        } else {
            untrackedStructures.add(normKey);
        }
        plugin.getConfig().set("untracked-structures", new ArrayList<>(untrackedStructures));
        plugin.saveConfig();
    }

    /**
     * Loads structure icons from default_icons.yml in data folder, copying it from resources if missing.
     */
    public void loadDefaultIcons() {
        defaultIcons.clear();
        java.io.File file = new java.io.File(plugin.getDataFolder(), "default_icons.yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("default_icons.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Could not save default_icons.yml resource from jar.");
            }
        }
        if (file.exists()) {
            org.bukkit.configuration.file.YamlConfiguration defaultIconsConfig = 
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            ConfigurationSection iconsSection = defaultIconsConfig.getConfigurationSection("icons");
            if (iconsSection != null) {
                for (String key : iconsSection.getKeys(false)) {
                    defaultIcons.put(key.toLowerCase(), iconsSection.getString(key));
                }
            }
        }
    }

    /**
     * Gets the icon URL for a structure, falling back to default_icons.yml if not set in config.yml
     */
    public String getStructureIcon(String structureType) {
        if (structureType == null) return null;
        String iconPath = null;
        
        String customUrl = plugin.getConfig().getString("bluemap.icons." + structureType);
        if (customUrl != null && !customUrl.isEmpty()) {
            iconPath = customUrl;
        } else {
            String normType = structureType.toLowerCase();
            customUrl = plugin.getConfig().getString("bluemap.icons." + normType);
            if (customUrl != null && !customUrl.isEmpty()) {
                iconPath = customUrl;
            } else {
                iconPath = defaultIcons.get(normType);
            }
        }

        if (iconPath == null || iconPath.isEmpty()) {
            return null;
        }

        // If it's already a full URL, return it directly
        if (iconPath.startsWith("http://") || iconPath.startsWith("https://")) {
            return iconPath;
        }

        // Prepend the configured base URL (falls back to Minecraft Wiki's file path redirect service)
        String baseUrl = plugin.getConfig().getString("bluemap.icon-base-url", "https://minecraft.wiki/w/Special:FilePath/");
        if (baseUrl == null) {
            baseUrl = "";
        }
        return baseUrl + iconPath;
    }
}
