package com.herenavy.herenavy.progression;

import com.herenavy.herenavy.HereNavyPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

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
        private final Set<UUID> discoveredPlayers;

        public StructureRecord(UUID id, String type, double x, double y, double z, Set<UUID> discoveredPlayers) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.discoveredPlayers = discoveredPlayers;
        }

        public UUID getId() { return id; }
        public String getType() { return type; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
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
                    
                    List<String> uuids = yaml.getStringList("discovered-by");
                    Set<UUID> players = new HashSet<>();
                    for (String s : uuids) {
                        try {
                            players.add(UUID.fromString(s));
                        } catch (IllegalArgumentException ignored) {}
                    }

                    StructureRecord record = new StructureRecord(id, type, x, y, z, players);
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
    private void saveRecord(StructureRecord record) {
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
    public boolean registerDiscovery(UUID playerUuid, String type, double x, double y, double z) {
        Map<UUID, StructureRecord> typeCache = cache.computeIfAbsent(type, k -> new HashMap<>());
        StructureRecord record = findNearbyStructure(type, x, z);

        if (record == null) {
            // Brand new physical structure instance found in the world
            UUID id = UUID.randomUUID();
            Set<UUID> players = new HashSet<>();
            players.add(playerUuid);
            
            StructureRecord newRecord = new StructureRecord(id, type, x, y, z, players);
            typeCache.put(id, newRecord);
            saveRecord(newRecord);
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
}
