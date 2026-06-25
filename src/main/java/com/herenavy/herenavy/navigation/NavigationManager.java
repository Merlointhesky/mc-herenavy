package com.herenavy.herenavy.navigation;

import com.herenavy.herenavy.HereNavyPlugin;
import com.herenavy.herenavy.progression.StructureDiscoveryManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StructureSearchResult;

import java.util.*;

public final class NavigationManager {

    private final HereNavyPlugin plugin;
    private final StructureDiscoveryManager structureDiscoveryManager;
    private final ArrowManager arrowManager;

    private final Map<UUID, NavigationSession> sessions = new HashMap<>();
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private BukkitRunnable navTickTask;
    private CompassTask compassTask;

    public static final class NavigationSession {
        private final String targetName;
        private final Location destination;
        private final boolean isManual;

        public NavigationSession(String targetName, Location destination, boolean isManual) {
            this.targetName = targetName;
            this.destination = destination;
            this.isManual = isManual;
        }

        public String getTargetName() { return targetName; }
        public Location getDestination() { return destination; }
        public boolean isManual() { return isManual; }
    }

    public NavigationManager(HereNavyPlugin plugin, StructureDiscoveryManager structureDiscoveryManager, ArrowManager arrowManager) {
        this.plugin = plugin;
        this.structureDiscoveryManager = structureDiscoveryManager;
        this.arrowManager = arrowManager;
    }

    public boolean isNavigating(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public NavigationSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public String getNavigationTargetName(Player player) {
        NavigationSession session = getSession(player);
        return session != null ? session.getTargetName() : null;
    }

    /**
     * Starts a manual coordinate navigation
     */
    public void startManualNavigation(Player player, Location target) {
        stopNavigation(player, false);

        NavigationSession session = new NavigationSession("Manual Destination", target, true);
        sessions.put(player.getUniqueId(), session);

        player.sendMessage(MiniMessage.miniMessage().deserialize(
            "<green>Navigation started to coordinates: <bold>[" + target.getBlockX() + ", " + target.getBlockY() + ", " + target.getBlockZ() + "]</bold>!</green>"
        ));

        arrowManager.createArrow(player, target);
        player.playSound(player.getLocation(), "item.eccentric_tome.use", 1.0f, 1.0f);
    }

    /**
     * Locates a biome and starts navigation to it
     */
    public boolean startBiomeNavigation(Player player, String biomeKey) {
        stopNavigation(player, false);

        NamespacedKey namespacedKey = NamespacedKey.fromString(biomeKey);
        if (namespacedKey == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid biome key: " + biomeKey + "</red>"));
            return false;
        }
        Biome biome = Registry.BIOME.get(namespacedKey);
        if (biome == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Could not find biome: " + biomeKey + "</red>"));
            return false;
        }

        String biomeName = namespacedKey.getKey();
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Locating nearest " + formatName(biomeName) + "...</gray>"));

        // Use Bukkit biome locator (2000 blocks search radius)
        org.bukkit.util.BiomeSearchResult searchResult = player.getWorld().locateNearestBiome(player.getLocation(), 2000, 32, 64, biome);
        Location biomeLoc = searchResult != null ? searchResult.getLocation() : null;
        if (biomeLoc == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Could not find a " + formatName(biomeName) + " biome within 2000 blocks!</red>"));
            return false;
        }

        NavigationSession session = new NavigationSession(biomeKey, biomeLoc, false);
        sessions.put(player.getUniqueId(), session);

        player.sendMessage(MiniMessage.miniMessage().deserialize(
            "<green>Navigation started to <bold>" + formatName(biomeName) + "</bold>!</green>"
        ));

        arrowManager.createArrow(player, biomeLoc);
        player.playSound(player.getLocation(), "item.eccentric_tome.use", 1.0f, 1.0f);
        return true;
    }

    /**
     * Starts structure navigation implementing Origin Shifting Target Selection
     */
    public boolean startStructureNavigation(Player player, String structureKey) {
        stopNavigation(player, false);

        NamespacedKey namespacedKey = NamespacedKey.fromString(structureKey);
        if (namespacedKey == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid structure key: " + structureKey + "</red>"));
            return false;
        }

        Structure structure = Registry.STRUCTURE.get(namespacedKey);
        if (structure == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Structure type not registered in this world version.</red>"));
            return false;
        }

        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Locating nearest unexplored " + formatKey(structureKey) + "...</gray>"));

        Location currentSearchOrigin = player.getLocation();
        Location targetLoc = null;
        Set<String> encounteredLocs = new HashSet<>();

        // Perform mathematical Origin Shifting (up to 10 attempts)
        for (int attempt = 1; attempt <= 10; attempt++) {
            // Search with a large 500 chunks radius (8000 blocks range)
            StructureSearchResult searchResult = player.getWorld().locateNearestStructure(currentSearchOrigin, structure, 500, false);
            if (searchResult == null) {
                break;
            }

            Location foundLoc = searchResult.getLocation();
            String locKey = foundLoc.getBlockX() + "," + foundLoc.getBlockZ();

            // If we are getting the exact same physical coordinates again, shift randomly to break any loop
            if (encounteredLocs.contains(locKey)) {
                double angle = Math.random() * 2 * Math.PI;
                currentSearchOrigin = foundLoc.clone().add(Math.cos(angle) * 3000, 0, Math.sin(angle) * 3000);
                continue;
            }
            encounteredLocs.add(locKey);

            // Check if player has already discovered this specific structure instance
            boolean alreadyFound = structureDiscoveryManager.hasPlayerDiscovered(
                player.getUniqueId(), 
                structureKey, 
                foundLoc.getX(), 
                foundLoc.getZ()
            );

            if (!alreadyFound) {
                targetLoc = foundLoc;
                break; // Found an unexplored structure!
            }

            // Unexplored structure match failed. Shift origin mathematically in a random direction to search a new quadrant
            double angle = Math.random() * 2 * Math.PI;
            currentSearchOrigin = foundLoc.clone().add(Math.cos(angle) * 3000, 0, Math.sin(angle) * 3000);
        }

        if (targetLoc == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<red>No unexplored " + formatKey(structureKey) + " could be located within searching range.</red>"
            ));
            return false;
        }

        NavigationSession session = new NavigationSession(structureKey, targetLoc, false);
        sessions.put(player.getUniqueId(), session);

        player.sendMessage(MiniMessage.miniMessage().deserialize(
            "<green>Navigation started to unexplored <bold>" + formatKey(structureKey) + "</bold> at [" + targetLoc.getBlockX() + ", " + targetLoc.getBlockZ() + "]!</green>"
        ));

        arrowManager.createArrow(player, targetLoc);
        player.playSound(player.getLocation(), "item.eccentric_tome.use", 1.0f, 1.0f);
        return true;
    }

    /**
     * Cancels player navigation session
     */
    public void stopNavigation(Player player, boolean arrived) {
        NavigationSession session = sessions.remove(player.getUniqueId());
        
        // Remove boss bar if active
        BossBar bar = activeBossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }

        // Clean up arrow displays
        arrowManager.removeArrow(player);

        if (session != null) {
            if (arrived) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "\n<green><bold>⛳ DESTINATION ARRIVED! ⛳</bold></green>\n" +
                    "<yellow>You have successfully reached: <bold>" + formatKey(session.getTargetName()) + "</bold>!</yellow>\n"
                ));
                player.playSound(player.getLocation(), "ui.toast.challenge_complete", 1.0f, 1.2f);
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Navigation course canceled.</red>"
                ));
            }
        }
    }

    public void hideBossBar(Player player) {
        BossBar bar = activeBossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public void cleanupAllNavigations() {
        for (UUID uuid : new HashSet<>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                stopNavigation(player, false);
            }
        }
        sessions.clear();
    }

    /**
     * Starts repeating visual tasks
     */
    public void startTasks() {
        // Task 1: 4-tick Skyrim compass updater task
        this.compassTask = new CompassTask(plugin, this);
        this.compassTask.runTaskTimer(plugin, 0, 4);

        // Task 2: 10-tick arrival and particle trail updates task
        this.navTickTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new HashSet<>(sessions.keySet())) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;

                    NavigationSession session = sessions.get(uuid);
                    if (session == null) continue;

                    Location dest = session.getDestination();
                    Location pLoc = player.getLocation();
                    
                    // Simple distance check in 2D to trigger manual coord or biome arrival
                    // (Proximity structure discovery will trigger its own auto-completion)
                    if (session.isManual() || session.getTargetName().contains("plains") || session.getTargetName().contains("forest") || session.getTargetName().contains("desert") || session.getTargetName().contains("ocean") || session.getTargetName().contains("swamp") || session.getTargetName().contains("jungle") || session.getTargetName().contains("savanna") || session.getTargetName().contains("badlands") || session.getTargetName().contains("cherry_grove")) {
                        double dx = dest.getX() - pLoc.getX();
                        double dz = dest.getZ() - pLoc.getZ();
                        double distance2D = Math.sqrt(dx * dx + dz * dz);
                        
                        if (distance2D <= 30) {
                            stopNavigation(player, true);
                        }
                    }
                }
            }
        };
        this.navTickTask.runTaskTimer(plugin, 0, 10);
    }

    public BossBar getBossBarForPlayer(Player player, Component title) {
        BossBar bar = activeBossBars.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(
                title, 
                1.0f, 
                BossBar.Color.BLUE, 
                BossBar.Overlay.PROGRESS
            );
            activeBossBars.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        } else {
            bar.name(title);
        }
        return bar;
    }

    public void stopTasks() {
        if (compassTask != null) {
            compassTask.cancel();
            compassTask = null;
        }
        if (navTickTask != null) {
            navTickTask.cancel();
            navTickTask = null;
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
