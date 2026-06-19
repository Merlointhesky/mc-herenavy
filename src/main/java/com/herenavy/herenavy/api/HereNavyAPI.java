package com.herenavy.herenavy.api;

import com.herenavy.herenavy.HereNavyPlugin;
import com.herenavy.herenavy.progression.StructureDiscoveryManager.StructureRecord;
import org.bukkit.Location;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public final class HereNavyAPI {

    private static HereNavyAPI instance;
    private final HereNavyPlugin plugin;

    public HereNavyAPI(HereNavyPlugin plugin) {
        this.plugin = plugin;
    }

    public static HereNavyAPI getInstance() {
        return instance;
    }

    public static void setInstance(HereNavyAPI apiInstance) {
        instance = apiInstance;
    }

    /**
     * Registers or updates a town/village marker in HereNavy.
     * Checks if there is a village structure record within 120 blocks.
     * If so, updates its name and marker.
     * If not, creates a new village structure record, saves it, and adds the marker.
     *
     * @param townName the custom name of the town
     * @param loc the location of the town center
     * @param creatorId the UUID of the player who created the town (can be null)
     * @return true if successfully registered or updated, false otherwise
     */
    public boolean registerOrUpdateTownMarker(String townName, Location loc, UUID creatorId) {
        try {
            double bestDistSq = 14400.0; // 120 blocks
            StructureRecord closest = null;
            for (StructureRecord record : plugin.getStructureDiscoveryManager().getAllStructures()) {
                if (record.getType().toLowerCase().contains("village")) {
                    String rWorld = record.getWorldName();
                    if (rWorld != null && !rWorld.isEmpty() && !rWorld.equalsIgnoreCase("null") && !rWorld.equals(loc.getWorld().getName())) {
                        continue;
                    }
                    double dx = record.getX() - loc.getX();
                    double dz = record.getZ() - loc.getZ();
                    double distSq = dx * dx + dz * dz;
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        closest = record;
                    }
                }
            }

            if (closest != null) {
                String rWorld = closest.getWorldName();
                if (rWorld == null || rWorld.isEmpty() || rWorld.equalsIgnoreCase("null") || closest.getY() <= 0.0) {
                    StructureRecord upgraded = new StructureRecord(
                        closest.getId(),
                        closest.getType(),
                        closest.getX(),
                        loc.getY(),
                        closest.getZ(),
                        loc.getWorld().getName(),
                        townName,
                        closest.getDiscoveredPlayers()
                    );
                    plugin.getStructureDiscoveryManager().addStructureRecord(upgraded);
                    closest = upgraded;
                } else {
                    closest.setCustomName(townName);
                    plugin.getStructureDiscoveryManager().saveRecord(closest);
                }
                if (plugin.getBlueMapHook() != null) {
                    plugin.getBlueMapHook().addMarker(closest, true);
                }
                return true;
            } else {
                // Determine appropriate village type based on biome or location
                String structType = "minecraft:village_plains";
                try {
                    org.bukkit.Chunk chunk = loc.getChunk();
                    for (org.bukkit.generator.structure.GeneratedStructure genStruct : loc.getWorld().getStructures(chunk.getX(), chunk.getZ())) {
                        String key = genStruct.getStructure().getKey().toString();
                        if (key.contains("village")) {
                            structType = key;
                            break;
                        }
                    }
                } catch (Throwable ignored) {}

                if (structType.equals("minecraft:village_plains")) {
                    String biomeName = loc.getBlock().getBiome().name().toLowerCase();
                    if (biomeName.contains("desert")) {
                        structType = "minecraft:village_desert";
                    } else if (biomeName.contains("savanna")) {
                        structType = "minecraft:village_savanna";
                    } else if (biomeName.contains("snow") || biomeName.contains("frozen") || biomeName.contains("ice")) {
                        structType = "minecraft:village_snowy";
                    } else if (biomeName.contains("taiga")) {
                        structType = "minecraft:village_taiga";
                    }
                }

                Set<UUID> discovered = new HashSet<>();
                if (creatorId != null) {
                    discovered.add(creatorId);
                }

                StructureRecord newRecord = new StructureRecord(
                    UUID.randomUUID(),
                    structType,
                    loc.getX(),
                    loc.getY(),
                    loc.getZ(),
                    loc.getWorld().getName(),
                    townName,
                    discovered
                );

                // Add to cache and save
                plugin.getStructureDiscoveryManager().addStructureRecord(newRecord);
                
                if (plugin.getBlueMapHook() != null) {
                    plugin.getBlueMapHook().addMarker(newRecord, true);
                }
                return true;
            }
        } catch (Throwable e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to register or update town marker in HereNavy: ", e);
        }
        return false;
    }

    /**
     * Resets the custom name of the closest HereNavy village to null when the town is deleted,
     * and updates its BlueMap marker accordingly.
     *
     * @param loc the location of the town center
     */
    public void handleTownDisband(Location loc) {
        try {
            double bestDistSq = 14400.0; // 120 blocks
            StructureRecord closest = null;
            for (StructureRecord record : plugin.getStructureDiscoveryManager().getAllStructures()) {
                if (record.getType().toLowerCase().contains("village") && record.getWorldName() != null && record.getWorldName().equals(loc.getWorld().getName())) {
                    double dx = record.getX() - loc.getX();
                    double dz = record.getZ() - loc.getZ();
                    double distSq = dx * dx + dz * dz;
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        closest = record;
                    }
                }
            }
            if (closest != null) {
                closest.setCustomName(null);
                plugin.getStructureDiscoveryManager().saveRecord(closest);
                if (plugin.getBlueMapHook() != null) {
                    plugin.getBlueMapHook().addMarker(closest, true);
                }
            }
        } catch (Throwable e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to handle town disband in HereNavy: ", e);
        }
    }
}
