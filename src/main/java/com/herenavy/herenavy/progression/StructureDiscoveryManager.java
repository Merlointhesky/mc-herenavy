package com.herenavy.herenavy.progression;

import com.herenavy.herenavy.HereNavyPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class StructureDiscoveryManager {

    private final HereNavyPlugin plugin;
    private final File structuresFolder;
    
    // Memory Cache: Map<StructureType, Map<StructureUUID, StructureRecord>>
    private final Map<String, Map<UUID, StructureRecord>> cache = new HashMap<>();

    public static final class StructureRecord {
        private final UUID id;
        private final String type;
        private final double x;
        private final double y;
        private final double z;
        private final String worldName;
        private String customName;
        private final Set<UUID> discoveredPlayers;

        public StructureRecord(UUID id, String type, double x, double y, double z, String worldName, String customName, Set<UUID> discoveredPlayers) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.worldName = worldName;
            this.customName = customName;
            this.discoveredPlayers = discoveredPlayers;
        }

        public StructureRecord(UUID id, String type, double x, double y, double z, String worldName, Set<UUID> discoveredPlayers) {
            this(id, type, x, y, z, worldName, null, discoveredPlayers);
        }

        public UUID getId() { return id; }
        public String getType() { return type; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public String getWorldName() { return worldName; }
        public String getCustomName() { return customName; }
        public void setCustomName(String customName) { this.customName = customName; }
        public Set<UUID> getDiscoveredPlayers() { return discoveredPlayers; }
    }

    public StructureDiscoveryManager(HereNavyPlugin plugin) {
        this.plugin = plugin;
        this.structuresFolder = new File(plugin.getDataFolder(), "structures");
        if (!structuresFolder.exists()) {
            structuresFolder.mkdirs();
        }
        preloadAllStructures();
    }

    /**
     * Scans the structures/ directory and loads all entries into memory
     */
    private void preloadAllStructures() {
        cache.clear();
        if (!structuresFolder.exists()) return;

        File[] typeDirs = structuresFolder.listFiles();
        if (typeDirs == null) return;

        for (File typeDir : typeDirs) {
            if (!typeDir.isDirectory()) continue;
            String type = typeDir.getName().replace("_col_", ":"); // Restore colons

            File[] files = typeDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null) continue;

            Map<UUID, StructureRecord> typeCache = cache.computeIfAbsent(type, k -> new HashMap<>());

            for (File file : files) {
                try {
                    String name = file.getName().substring(0, file.getName().length() - 4);
                    UUID id = UUID.fromString(name);

                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    double x = yaml.getDouble("x");
                    double y = yaml.getDouble("y");
                    double z = yaml.getDouble("z");
                    String world = yaml.getString("world");
                    String customName = yaml.getString("custom-name");
                    
                    List<String> uuids = yaml.getStringList("discovered-by");
                    Set<UUID> players = new HashSet<>();
                    for (String s : uuids) {
                        try {
                            players.add(UUID.fromString(s));
                        } catch (IllegalArgumentException ignored) {}
                    }

                    StructureRecord record = new StructureRecord(id, type, x, y, z, world, customName, players);
                    typeCache.put(id, record);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to load structure record from: " + file.getPath());
                }
            }
        }
    }

    /**
     * Converts structure type to folder name (replacing colons for compatibility)
     */
    private String getFolderName(String type) {
        return type.replace(":", "_col_");
    }

    /**
     * Saves a record to its YAML file
     */
    public void saveRecord(StructureRecord record) {
        String typeFolder = getFolderName(record.getType());
        File folder = new File(structuresFolder, typeFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, record.getId().toString() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("x", record.getX());
        yaml.set("y", record.getY());
        yaml.set("z", record.getZ());
        if (record.getWorldName() != null) {
            yaml.set("world", record.getWorldName());
        }
        if (record.getCustomName() != null) {
            yaml.set("custom-name", record.getCustomName());
        }

        List<String> list = new ArrayList<>();
        for (UUID u : record.getDiscoveredPlayers()) {
            list.add(u.toString());
        }
        yaml.set("discovered-by", list);

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save structure file: " + file.getPath());
        }
    }

    /**
     * Finds a matching physical structure by proximity (within 100 blocks)
     */
    public StructureRecord findNearbyStructure(String type, double x, double z) {
        Map<UUID, StructureRecord> typeCache = cache.get(type);
        if (typeCache == null) return null;

        for (StructureRecord record : typeCache.values()) {
            double dx = record.getX() - x;
            double dz = record.getZ() - z;
            double distSq = dx * dx + dz * dz;
            if (distSq <= 10000.0) { // 100 blocks distance limit (100^2)
                return record;
            }
        }
        return null;
    }

    /**
     * Checks if a player has discovered a structure instance
     */
    public boolean hasPlayerDiscovered(UUID playerUuid, String type, double x, double z) {
        StructureRecord record = findNearbyStructure(type, x, z);
        if (record == null) return false;
        return record.getDiscoveredPlayers().contains(playerUuid);
    }

    /**
     * Registers a player discovering a structure.
     * Returns true if this is the player's FIRST time discovering this specific physical structure.
     */
    public boolean registerDiscovery(UUID playerUuid, String type, double x, double y, double z, String worldName) {
        Map<UUID, StructureRecord> typeCache = cache.computeIfAbsent(type, k -> new HashMap<>());
        StructureRecord record = findNearbyStructure(type, x, z);

        if (record == null) {
            // Brand new physical structure instance found in the world
            UUID id = UUID.randomUUID();
            Set<UUID> players = new HashSet<>();
            players.add(playerUuid);
            
            StructureRecord newRecord = new StructureRecord(id, type, x, y, z, worldName, players);
            typeCache.put(id, newRecord);
            saveRecord(newRecord);
            
            // Check if it is a village to trigger the naming session
            if (type.toLowerCase().contains("village")) {
                startNamingSession(playerUuid, newRecord);
                org.bukkit.entity.Player p = Bukkit.getPlayer(playerUuid);
                if (p != null) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize(
                        "\n<gold><bold>🏘 NEW SETTLEMENT DISCOVERED! 🏘</bold></gold>\n" +
                        "<yellow>You are the first explorer to discover this village!</yellow>\n" +
                        "<yellow>Please type a custom name for this village in chat within the next 60 seconds (or type <bold>default</bold> to use the default name):</yellow>\n"
                    ));
                    p.playSound(p.getLocation(), "entity.player.levelup", 1.0f, 0.5f);
                }
            } else {
                // Trigger BlueMap marker registration instantly for other structures
                if (plugin.getBlueMapHook() != null && plugin.getConfigManager().isStructureTracked(type)) {
                    plugin.getBlueMapHook().addMarker(newRecord);
                }
            }
            
            return true;
        } else {
            // Already registered physical structure in files
            if (record.getDiscoveredPlayers().add(playerUuid)) {
                // First time this player has discovered it
                saveRecord(record);
                return true;
            }
            return false; // Already discovered by this player
        }
    }

    /**
     * Gets all structure records discovered by a specific player of a specific type
     */
    public List<StructureRecord> getDiscoveredStructures(UUID playerUuid, String type) {
        List<StructureRecord> list = new ArrayList<>();
        Map<UUID, StructureRecord> typeCache = cache.get(type);
        if (typeCache == null) return list;

        for (StructureRecord record : typeCache.values()) {
            if (record.getDiscoveredPlayers().contains(playerUuid)) {
                list.add(record);
            }
        }
        return list;
    }

    /**
     * Gets all structure types discovered by a player
     */
    public Set<String> getDiscoveredTypesForPlayer(UUID playerUuid) {
        Set<String> discovered = new HashSet<>();
        for (Map.Entry<String, Map<UUID, StructureRecord>> entry : cache.entrySet()) {
            for (StructureRecord record : entry.getValue().values()) {
                if (record.getDiscoveredPlayers().contains(playerUuid)) {
                    discovered.add(entry.getKey());
                    break; // Just need to know they found at least one of this type
                }
            }
        }
        return discovered;
    }

    /**
     * Gets a list of all structure records in memory (loaded from files)
     */
    public List<StructureRecord> getAllStructures() {
        List<StructureRecord> list = new ArrayList<>();
        for (Map<UUID, StructureRecord> typeMap : cache.values()) {
            list.addAll(typeMap.values());
        }
        return list;
    }

    // Naming Session management: Map<PlayerUUID, StructureRecord>
    private final Map<UUID, StructureRecord> namingSessions = new HashMap<>();

    /**
     * Scans preloaded files to determine the next incremental number for Village NNN
     */
    public int getNextVillageNumber() {
        int count = 1;
        for (Map<UUID, StructureRecord> typeMap : cache.values()) {
            for (StructureRecord record : typeMap.values()) {
                if (record.getType().toLowerCase().contains("village")) {
                    String name = record.getCustomName();
                    if (name != null && name.startsWith("Village ")) {
                        try {
                            int num = Integer.parseInt(name.substring(8).trim());
                            if (num >= count) {
                                count = num + 1;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return count;
    }

    public void startNamingSession(UUID playerUuid, StructureRecord record) {
        namingSessions.put(playerUuid, record);
        // Automatically expire session in 60 seconds if they don't respond
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (namingSessions.containsKey(playerUuid) && namingSessions.get(playerUuid).getId().equals(record.getId())) {
                StructureRecord expiredRecord = namingSessions.remove(playerUuid);
                
                // Formulate incremental settlement name "Village NNN"
                int villageNumber = getNextVillageNumber();
                String defaultName = "Village " + villageNumber;
                expiredRecord.setCustomName(defaultName);
                saveRecord(expiredRecord);
                
                org.bukkit.entity.Player p = Bukkit.getPlayer(playerUuid);
                if (p != null) {
                    p.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Naming session timed out! Settlement registered as <bold>" + defaultName + "</bold>.</red>"
                    ));
                    p.playSound(p.getLocation(), "block.note_block.iron_xylophone", 1.0f, 1.0f);
                }
                
                // Add marker with custom default/numbered name
                if (plugin.getBlueMapHook() != null && plugin.getConfigManager().isStructureTracked(expiredRecord.getType())) {
                    plugin.getBlueMapHook().addMarker(expiredRecord);
                }
            }
        }, 1200L); // 60 seconds (20 ticks * 60)
    }

    public boolean isInNamingSession(UUID playerUuid) {
        return namingSessions.containsKey(playerUuid);
    }

    public StructureRecord getNamingSessionRecord(UUID playerUuid) {
        return namingSessions.get(playerUuid);
    }

    public void endNamingSession(UUID playerUuid) {
        namingSessions.remove(playerUuid);
    }
}
