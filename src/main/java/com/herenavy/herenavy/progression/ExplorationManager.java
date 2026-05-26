package com.herenavy.herenavy.progression;

import com.herenavy.herenavy.HereNavyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StructureSearchResult;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ExplorationManager {

    private final HereNavyPlugin plugin;
    private final StructureDiscoveryManager structureDiscoveryManager;
    private final File playersFolder;
    
    private final Map<UUID, PlayerData> onlinePlayerData = new HashMap<>();
    private BukkitRunnable discoveryTask;

    public ExplorationManager(HereNavyPlugin plugin, StructureDiscoveryManager structureDiscoveryManager) {
        this.plugin = plugin;
        this.structureDiscoveryManager = structureDiscoveryManager;
        this.playersFolder = new File(plugin.getDataFolder(), "players");
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }
    }

    /**
     * Loads a player's progression data from players/{uuid}.yml
     */
    public PlayerData loadPlayerData(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        File file = new File(playersFolder, uuid.toString() + ".yml");
        
        if (file.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            data.setLevel(yaml.getInt("level", 1));
            data.setExp(yaml.getInt("exp", 0));
            data.setShowArrow(yaml.getBoolean("preferences.show-arrow", true));
            data.setShowTrail(yaml.getBoolean("preferences.show-trail", true));
            data.setCompassStyle(yaml.getString("preferences.compass-style", "ACTIONBAR"));
            
            List<String> biomes = yaml.getStringList("discovered-biomes");
            for (String b : biomes) {
                data.discoverBiome(b);
            }
        } else {
            savePlayerData(data);
        }
        
        onlinePlayerData.put(uuid, data);
        return data;
    }

    /**
     * Saves player data to players/{uuid}.yml
     */
    public void savePlayerData(PlayerData data) {
        File file = new File(playersFolder, data.getUuid().toString() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        
        yaml.set("level", data.getLevel());
        yaml.set("exp", data.getExp());
        yaml.set("preferences.show-arrow", data.isShowArrow());
        yaml.set("preferences.show-trail", data.isShowTrail());
        yaml.set("preferences.compass-style", data.getCompassStyle());
        yaml.set("discovered-biomes", new ArrayList<>(data.getDiscoveredBiomes()));
        
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for UUID: " + data.getUuid());
        }
    }

    public void unloadPlayerData(UUID uuid) {
        PlayerData data = onlinePlayerData.remove(uuid);
        if (data != null) {
            savePlayerData(data);
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return onlinePlayerData.get(uuid);
    }

    /**
     * Awards EXP to a player and handles leveling up
     */
    public void awardExp(Player player, int amount) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data == null) return;
        
        int maxLvl = plugin.getConfigManager().getMaxLevel();
        if (data.getLevel() >= maxLvl) return; // Already max level

        data.addExp(amount);
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gray>+ " + amount + " Exploration EXP</gray>"
        ));

        // Check level up (progressive level requirement: level * 100)
        boolean leveledUp = false;
        while (data.getExp() >= getRequiredExpForNextLevel(data.getLevel()) && data.getLevel() < maxLvl) {
            data.setExp(data.getExp() - getRequiredExpForNextLevel(data.getLevel()));
            data.setLevel(data.getLevel() + 1);
            leveledUp = true;

            if (data.getLevel() == maxLvl) {
                data.setExp(0); // Cap at max level
                awardCapstoneReward(player);
                break;
            }
        }

        if (leveledUp) {
            savePlayerData(data);
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "\n<gold><bold>★ LEVEL UP! ★</bold></gold>\n" +
                    "<yellow>You are now Exploration Level <bold>" + data.getLevel() + "</bold>!</yellow>\n"
            ));
            player.playSound(player.getLocation(), "entity.player.levelup", 1.0f, 1.0f);
        } else {
            savePlayerData(data);
        }
    }

    public int getRequiredExpForNextLevel(int currentLevel) {
        return currentLevel * 100;
    }

    /**
     * Awards the level 100 Capstone Elytra
     */
    private void awardCapstoneReward(Player player) {
        String itemTypeStr = plugin.getConfigManager().getRewardItem();
        String itemNameStr = plugin.getConfigManager().getRewardName();
        
        Material mat = Material.matchMaterial(itemTypeStr);
        if (mat == null) mat = Material.ELYTRA;

        ItemStack elytra = new ItemStack(mat);
        ItemMeta meta = elytra.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(itemNameStr));
            // Add a description lore
            meta.lore(List.of(
                MiniMessage.miniMessage().deserialize("<gray>Awarded to a master cartographer for</gray>"),
                MiniMessage.miniMessage().deserialize("<gray>reaching Exploration Level 100.</gray>")
            ));
            elytra.setItemMeta(meta);
        }

        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(elytra);
        if (!remaining.isEmpty()) {
            for (ItemStack item : remaining.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<yellow>Your inventory was full! Your master reward has been dropped on the ground.</yellow>"
            ));
        } else {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>Congratulations! You have received the legendary </green>" + itemNameStr + "<green> item in your inventory!</green>"
            ));
        }

        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
            "\n<gold><bold>🏆 EXPLORATION CAPSTONE REACHED 🏆</bold></gold>\n" +
            "<yellow><bold>" + player.getName() + "</bold> has achieved Exploration Level 100 and unlocked the master Elytra!</yellow>\n"
        ));
    }

    /**
     * Starts the 20-tick exploration discovery loop
     */
    public void startTasks() {
        if (discoveryTask != null) return;
        
        discoveryTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerData data = getPlayerData(player.getUniqueId());
                    if (data == null) continue;

                    // 1. Passive Biome Discovery
                    Location loc = player.getLocation();
                    Biome biome = loc.getBlock().getBiome();
                    String biomeKey = "minecraft:" + biome.name().toLowerCase();
                    
                    if (!data.hasDiscoveredBiome(biomeKey)) {
                        data.discoverBiome(biomeKey);
                        savePlayerData(data);
                        int exp = plugin.getConfigManager().getExpPerBiome();
                        
                        player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<green>🌍 Discovered new Biome: <bold>" + formatName(biome.name()) + "</bold>!</green>"
                        ));
                        awardExp(player, exp);
                    }

                    // 2. Active Structure Discovery (check nearest generated structure within 2 chunks)
                    checkStructureDiscovery(player, data);
                }
            }
        };
        discoveryTask.runTaskTimer(plugin, 0, 20);
    }

    /**
     * Performs a localized search for nearby generated structures to trigger discoveries
     */
    private void checkStructureDiscovery(Player player, PlayerData data) {
        // Check if the current chunk contains any part (pieces) of our tracked structures
        org.bukkit.Chunk chunk = player.getLocation().getChunk();
        Collection<org.bukkit.generator.structure.GeneratedStructure> structures = player.getWorld().getStructures(chunk.getX(), chunk.getZ());

        for (org.bukkit.generator.structure.GeneratedStructure genStruct : structures) {
            Structure struct = genStruct.getStructure();
            String structKey = struct.getKey().toString();
            
            // Dynamically check against the registered structures in the GUI!
            if (plugin.getExplorerGUI().isRegisteredStructure(structKey)) {
                // Standing inside a piece chunk! Find center coordinate (within 10 chunks radius) to save uniquely
                StructureSearchResult result = player.getWorld().locateNearestStructure(player.getLocation(), struct, 10, false);
                if (result == null) continue;

                Location structLoc = result.getLocation();
                boolean newlyDiscovered = structureDiscoveryManager.registerDiscovery(
                    player.getUniqueId(),
                    structKey,
                    structLoc.getX(),
                    structLoc.getY(),
                    structLoc.getZ()
                );

                if (newlyDiscovered) {
                    int exp = plugin.getConfigManager().getExpPerStructure();
                    
                    Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                        "<gold><bold>⚔ DISCOVERY! ⚔</bold></gold> " +
                        "<yellow><bold>" + player.getName() + "</bold> has discovered a physical <bold>" + formatKey(structKey) + "</bold> at " +
                        "[" + structLoc.getBlockX() + ", " + structLoc.getBlockZ() + "]!</yellow>"
                    ));
                    
                    player.playSound(player.getLocation(), "ui.toast.challenge_complete", 1.0f, 1.0f);
                    awardExp(player, exp);
                    
                    // Stop navigation to this target automatically if they were navigating to it
                    if (plugin.getNavigationManager().isNavigating(player) && 
                        structKey.equalsIgnoreCase(plugin.getNavigationManager().getNavigationTargetName(player))) {
                        plugin.getNavigationManager().stopNavigation(player, true);
                    }
                }
            }
        }
    }

    public void stopTasks() {
        if (discoveryTask != null) {
            discoveryTask.cancel();
            discoveryTask = null;
        }
    }

    private String formatName(String raw) {
        String[] split = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            if (!s.isEmpty()) {
                sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String formatKey(String key) {
        if (key.contains(":")) {
            key = key.split(":")[1];
        }
        return formatName(key);
    }
}
