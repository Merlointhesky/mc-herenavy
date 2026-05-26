package com.herenavy.herenavy.navigation;

import com.herenavy.herenavy.HereNavyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ArrowManager {

    private final HereNavyPlugin plugin;
    private final Map<UUID, TextDisplay> activeArrows = new HashMap<>();
    private BukkitRunnable tickTask;

    public ArrowManager(HereNavyPlugin plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    /**
     * Spawns a TextDisplay arrow entity for a player
     */
    private TextDisplay spawnArrowEntity(Player player) {
        Location loc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(3.0)).subtract(0, 0.4, 0);
        
        TextDisplay arrow = player.getWorld().spawn(loc, TextDisplay.class, entity -> {
            entity.setText("➔");
            entity.setBillboard(Display.Billboard.FIXED); // Respect raw pitch and yaw rotations
            entity.setSeeThrough(true); // Render through solid blocks so player never loses track
            entity.setShadowed(true);
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0)); // Fully transparent box background
            
            // Set text style details
            entity.setLineWidth(200);
            entity.setGlowColorOverride(org.bukkit.Color.fromRGB(0, 255, 128)); // Glowing Cyan/Green arrow
            entity.setGlowing(true);
        });

        return arrow;
    }

    public void createArrow(Player player, Location destination) {
        removeArrow(player); // Clean up if existing
        
        String style = plugin.getConfigManager().getArrowStyle();
        if (style.equalsIgnoreCase("TEXT_DISPLAY") || style.equalsIgnoreCase("BOTH")) {
            TextDisplay arrow = spawnArrowEntity(player);
            activeArrows.put(player.getUniqueId(), arrow);
        } else {
            activeArrows.put(player.getUniqueId(), null);
        }
    }

    public void removeArrow(Player player) {
        TextDisplay arrow = activeArrows.remove(player.getUniqueId());
        if (arrow != null) {
            arrow.remove();
        }
    }

    private void startTickTask() {
        if (tickTask != null) return;

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new ArrayList<>(activeArrows.keySet())) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        activeArrows.remove(uuid);
                        continue;
                    }

                    NavigationManager.NavigationSession session = plugin.getNavigationManager().getSession(player);
                    if (session == null) {
                        removeArrow(player);
                        continue;
                    }

                    Location dest = session.getDestination();
                    com.herenavy.herenavy.progression.PlayerData data = plugin.getExplorationManager().getPlayerData(player.getUniqueId());

                    // 1. TextDisplay Entity arrow management
                    if (data != null && data.isShowArrow()) {
                        TextDisplay arrow = activeArrows.get(uuid);
                        if (arrow == null || !arrow.isValid()) {
                            arrow = spawnArrowEntity(player);
                            activeArrows.put(uuid, arrow);
                        }

                        // Maintain position exactly 3 blocks ahead of player's head and slightly down
                        Location eyeLoc = player.getEyeLocation();
                        Location targetLoc = eyeLoc.clone().add(eyeLoc.getDirection().multiply(3.0)).subtract(0, 0.35, 0);

                        // Rotate the arrow to point directly towards the destination location
                        double dx = dest.getX() - targetLoc.getX();
                        double dy = dest.getY() - targetLoc.getY();
                        double dz = dest.getZ() - targetLoc.getZ();

                        Location directionVector = new Location(player.getWorld(), 0, 0, 0);
                        directionVector.setDirection(new Vector(dx, dy, dz));

                        targetLoc.setYaw(directionVector.getYaw());
                        targetLoc.setPitch(directionVector.getPitch());

                        arrow.teleport(targetLoc);
                    } else {
                        // Destroy active entity if the visual style was changed/disabled
                        TextDisplay arrow = activeArrows.get(uuid);
                        if (arrow != null) {
                            arrow.remove();
                            activeArrows.put(uuid, null);
                        }
                    }

                    // 2. Shiny Particle Trail management
                    if (data != null && data.isShowTrail()) {
                        spawnParticleTrail(player, dest);
                    }
                }
            }
        };
        tickTask.runTaskTimer(plugin, 0, 2); // Ultra smooth tick every 2 ticks (10 updates per sec)
    }

    /**
     * Spawns a series of flying sparkles pointing forward towards the target direction
     */
    private void spawnParticleTrail(Player player, Location target) {
        Location start = player.getEyeLocation().subtract(0, 0.4, 0);
        Vector direction = target.toVector().subtract(start.toVector()).normalize();

        // Spawn a stream of beautiful cyan sparkles pointing the way
        for (int i = 1; i <= 6; i++) {
            Location particleLoc = start.clone().add(direction.clone().multiply(i * 2.0));
            // HAPPY_VILLAGER spawns stunning bright green-cyan sparkles that pop beautifully in survival!
            player.spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }
}
