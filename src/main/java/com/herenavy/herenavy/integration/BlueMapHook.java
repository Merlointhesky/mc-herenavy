package com.herenavy.herenavy.integration;

import com.herenavy.herenavy.HereNavyPlugin;
import com.herenavy.herenavy.progression.StructureDiscoveryManager.StructureRecord;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.Bukkit;
import com.flowpowered.math.vector.Vector3d;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public final class BlueMapHook {

    private static final String[] ICON_FILENAMES = {
        "blaze_spawn_egg.png",
        "breeze_spawn_egg.png",
        "chest.png",
        "chest_minecart.png",
        "drowned_spawn_egg.png",
        "elder_guardian_spawn_egg.png",
        "evoker_spawn_egg.png",
        "eye_of_ender.png",
        "husk_spawn_egg.png",
        "minecart.png",
        "oak_boat.png",
        "ocelot_spawn_egg.png",
        "piglin_brute_spawn_egg.png",
        "pillager_spawn_egg.png",
        "shulker_spawn_egg.png",
        "sniffer_spawn_egg.png",
        "stray_spawn_egg.png",
        "villager_spawn_egg.png",
        "warden_spawn_egg.png",
        "witch_spawn_egg.png",
        "wither_skeleton_spawn_egg.png",
        "zombified_piglin_spawn_egg.png"
    };

    private final HereNavyPlugin plugin;
    
    // Cache map for created MarkerSets: Map<MapId_MarkerSetId, MarkerSet>
    // This segregation guarantees distinct MarkerSet instances across dimensions/maps!
    private final Map<String, MarkerSet> segregatedMarkerSets = new HashMap<>();

    public BlueMapHook(HereNavyPlugin plugin) {
        this.plugin = plugin;
        
        // Extract packaged icons to BlueMap web directory on startup
        copyPackagedIconsToWebRoot();
        
        // Register API enable callback safely
        BlueMapAPI.onEnable(api -> {
            plugin.getLogger().info("[HereNavy] BlueMap API detected! Loading structure markers...");
            registerAllExistingMarkers(api);
        });
    }

    /**
     * Extracts and copies packaged 32x32 structure icons into the BlueMap web root directory.
     */
    private void copyPackagedIconsToWebRoot() {
        File targetDir = new File("bluemap/web/icons");
        if (!targetDir.exists()) {
            if (targetDir.mkdirs()) {
                plugin.getLogger().info("[HereNavy] Created BlueMap web icons directory: " + targetDir.getAbsolutePath());
            }
        }
        
        int copiedCount = 0;
        for (String filename : ICON_FILENAMES) {
            try (InputStream in = plugin.getResource("web/icons/" + filename)) {
                if (in == null) {
                    plugin.getLogger().warning("[HereNavy] Could not find packaged icon resource: web/icons/" + filename);
                    continue;
                }
                File outFile = new File(targetDir, filename);
                Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copiedCount++;
            } catch (IOException e) {
                plugin.getLogger().severe("[HereNavy] Failed to copy icon " + filename + ": " + e.getMessage());
            }
        }
        if (copiedCount > 0) {
            plugin.getLogger().info("[HereNavy] Successfully extracted " + copiedCount + " structure icons to " + targetDir.getPath());
        }
    }

    /**
     * Helper to dynamically check if BMarker (or other BlueMap markers plugin like BMMarker) is loaded
     */
    public boolean isBMarkerLoaded() {
        return Bukkit.getPluginManager().isPluginEnabled("BMarker") 
            || Bukkit.getPluginManager().isPluginEnabled("bmarker")
            || Bukkit.getPluginManager().isPluginEnabled("BMMarker")
            || Bukkit.getPluginManager().isPluginEnabled("bmmarker")
            || Bukkit.getPluginManager().isPluginEnabled("BlueMap-Markers")
            || Bukkit.getPluginManager().isPluginEnabled("BlueMapMarkers");
    }

    /**
     * Scans preloaded structure records and registers them as markers on startup
     */
    private void registerAllExistingMarkers(BlueMapAPI api) {
        segregatedMarkerSets.clear();

        // Clear existing markers from BlueMap's marker sets to prevent ghost/deleted markers
        for (BlueMapMap map : api.getMaps()) {
            for (String setId : new ArrayList<>(map.getMarkerSets().keySet())) {
                if (setId.startsWith("hn-set-")) {
                    MarkerSet set = map.getMarkerSets().get(setId);
                    if (set != null) {
                        set.getMarkers().clear();
                    }
                }
            }
        }

        List<StructureRecord> allRecords = plugin.getStructureDiscoveryManager().getAllStructures();
        for (StructureRecord record : allRecords) {
            // Only add if tracking is enabled by administration
            if (plugin.getConfigManager().isStructureTracked(record.getType())) {
                addMarker(record, true); // Force-update on startup scan
            }
        }
    }

    /**
     * Programmatically places a landmark marker on the appropriate BlueMap world maps
     */
    public void addMarker(StructureRecord record) {
        addMarker(record, false);
    }

    /**
     * Programmatically places a landmark marker on the appropriate BlueMap world maps.
     * If forceUpdate is true, it overwrites the marker even if it already exists.
     */
    public void addMarker(StructureRecord record, boolean forceUpdate) {
        BlueMapAPI.getInstance().ifPresent(api -> {
            String worldName = record.getWorldName();
            if (worldName == null || worldName.isEmpty() || worldName.equalsIgnoreCase("null")) {
                worldName = inferWorldFromStructureType(record.getType());
            }

            // Grouping: Determine target MarkerSet ID and label
            String rawId = plugin.getConfig().getString("bluemap.marker-sets." + record.getType() + ".id");
            String setLabel = plugin.getConfig().getString("bluemap.marker-sets." + record.getType() + ".label");
            
            if (rawId == null || rawId.isEmpty()) {
                rawId = getCleanSlug(record.getType());
            }
            if (setLabel == null || setLabel.isEmpty()) {
                setLabel = formatKey(record.getType());
            }

            String markerSetId = "hn-set-" + rawId;

            // Iterate over all loaded BlueMap maps
            for (BlueMapMap map : api.getMaps()) {
                String mapWorldName = map.getWorld().getSaveFolder().getFileName().toString();
                
                // Symmetrical dimensional mapping: match world name to the map's world name or map ID safely
                boolean isMatch = false;
                
                // Determine Marker Dimension
                String markerDim = "normal";
                org.bukkit.World markerWorld = Bukkit.getWorld(worldName);
                if (markerWorld != null) {
                    if (markerWorld.getEnvironment() == org.bukkit.World.Environment.NETHER) {
                        markerDim = "nether";
                    } else if (markerWorld.getEnvironment() == org.bukkit.World.Environment.THE_END) {
                        markerDim = "end";
                    }
                } else {
                    if (worldName.toLowerCase().contains("nether")) {
                        markerDim = "nether";
                    } else if (worldName.toLowerCase().contains("end")) {
                        markerDim = "end";
                    }
                }
                
                // Determine Map Dimension
                String mapDim = "normal";
                if (map.getId().toLowerCase().contains("nether") || mapWorldName.toLowerCase().contains("nether")) {
                    mapDim = "nether";
                } else if (map.getId().toLowerCase().contains("end") || mapWorldName.toLowerCase().contains("end")) {
                    mapDim = "end";
                }
                
                // Dimensions must match
                if (markerDim.equals(mapDim)) {
                    // Check if base names match to support multi-world environments correctly (e.g. survival vs creative)
                    String markerBase = getBaseWorldName(worldName);
                    String mapWorldBase = getBaseWorldName(mapWorldName);
                    String mapIdBase = getBaseWorldName(map.getId());
                    
                    if (markerBase.equalsIgnoreCase(mapWorldBase) || markerBase.equalsIgnoreCase(mapIdBase) 
                            || mapWorldName.equalsIgnoreCase(worldName) || map.getId().equalsIgnoreCase(worldName)) {
                        isMatch = true;
                    }
                }

                if (isMatch) {
                    String mapId = map.getId();
                    String cacheKey = mapId + "_" + markerSetId;

                    // Get or create a separate/segregated MarkerSet for this specific map!
                    MarkerSet markerSet = segregatedMarkerSets.get(cacheKey);
                    if (markerSet == null) {
                        markerSet = map.getMarkerSets().get(markerSetId);
                        if (markerSet == null) {
                            markerSet = MarkerSet.builder()
                                    .label(setLabel)
                                    .toggleable(true)
                                    .defaultHidden(false)
                                    .build();
                            
                            map.getMarkerSets().put(markerSetId, markerSet);
                        }
                        segregatedMarkerSets.put(cacheKey, markerSet);
                    }

                    // Create POIMarker
                    String markerId = "hn-marker-" + record.getId().toString();
                    
                    // Compare to existing markers to ensure we only batch-add missing ones (unless forced)
                    if (!forceUpdate && markerSet.getMarkers().containsKey(markerId)) {
                        continue;
                    }
                    
                    String displayName = record.getCustomName();
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = formatKey(record.getType());
                    }
                    
                    double y = record.getY();
                    boolean isNether = worldName.toLowerCase().contains("nether") 
                            || (Bukkit.getWorld(worldName) != null && Bukkit.getWorld(worldName).getEnvironment() == org.bukkit.World.Environment.NETHER);
                    
                    if (y <= 0 || (isNether && y == 127)) {
                        org.bukkit.World bukkitWorld = Bukkit.getWorld(worldName);
                        if (bukkitWorld != null && bukkitWorld.getEnvironment() == org.bukkit.World.Environment.NETHER) {
                            y = 64; // Nether structures should default to Y 64 to avoid roof
                        } else if (bukkitWorld != null) {
                            y = bukkitWorld.getHighestBlockYAt((int) record.getX(), (int) record.getZ());
                        } else {
                            y = 64; // Safe fallback
                        }
                    }

                    String markerLabel = displayName + " [" + (int) record.getX() + ", " + (int) y + ", " + (int) record.getZ() + "]";
                    
                    POIMarker.Builder builder = POIMarker.builder()
                            .label(markerLabel)
                            .position(new Vector3d(record.getX(), y, record.getZ()));

                    // Customize icon offset anchor and URL if BMarker is loaded
                    if (isBMarkerLoaded()) {
                        String iconUrl = plugin.getConfigManager().getStructureIcon(record.getType());
                        if (iconUrl != null && !iconUrl.isEmpty()) {
                            int anchorX = plugin.getConfig().getInt("bluemap.icon-anchor.x", 16);
                            int anchorY = plugin.getConfig().getInt("bluemap.icon-anchor.y", 16);
                            builder.icon(iconUrl, anchorX, anchorY);
                        }
                    }

                    markerSet.getMarkers().put(markerId, builder.build());
                }
            }
        });
    }

    /**
     * Backward-compatible helper to infer world from standard structure type strings
     */
    private String inferWorldFromStructureType(String type) {
        String lower = type.toLowerCase();
        org.bukkit.World.Environment targetEnv = org.bukkit.World.Environment.NORMAL;
        if (lower.contains("fortress") || lower.contains("bastion") || lower.contains("nether")) {
            targetEnv = org.bukkit.World.Environment.NETHER;
        } else if (lower.contains("end")) {
            targetEnv = org.bukkit.World.Environment.THE_END;
        }

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == targetEnv) {
                return world.getName();
            }
        }
        
        // Fallback to default Minecraft names if environment isn't found
        if (targetEnv == org.bukkit.World.Environment.NETHER) {
            return "world_nether";
        } else if (targetEnv == org.bukkit.World.Environment.THE_END) {
            return "world_the_end";
        }
        return "world";
    }

    /**
     * Extracts the base world name by stripping out dimension suffixes (e.g., nether, the_nether, end, the_end)
     */
    private String getBaseWorldName(String name) {
        if (name == null) return "";
        String lower = name.toLowerCase();
        lower = lower.replace("the_nether", "")
                     .replace("nether", "")
                     .replace("the_end", "")
                     .replace("end", "");
        // Remove trailing/leading underscores or hyphens
        lower = lower.replaceAll("^[_\\-]+|[_\\-]+$", "");
        return lower;
    }

    private String getCleanSlug(String type) {
        if (type.contains(":")) {
            type = type.split(":")[1];
        }
        return type.toLowerCase().replace("_", "-");
    }

    private String formatKey(String key) {
        if (key.contains(":")) {
            key = key.split(":")[1];
        }
        String[] split = key.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            if (!s.isEmpty()) {
                sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
