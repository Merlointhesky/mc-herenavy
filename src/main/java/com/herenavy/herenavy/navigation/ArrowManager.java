package com.herenavy.herenavy.navigation;

import com.herenavy.herenavy.HereNavyPlugin;
import com.herenavy.herenavy.progression.PlayerData;
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
     * Spawns a flat TextDisplay HUD entity centered in the player's crosshair view field
     */
    private TextDisplay spawnArrowEntity(Player player) {
        // Position it exactly 1.2 blocks in front of the player's eyes
        Location loc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.2));
        
        TextDisplay arrow = player.getWorld().spawn(loc, TextDisplay.class, entity -> {
            entity.setText("●");
            entity.setBillboard(Display.Billboard.CENTER); // ALWAYS face the player (flat screen overlay)
            entity.setSeeThrough(true); // Always render on top (so it acts as a HUD element)
            entity.setShadowed(true);
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0)); // transparent box background
            
            // Set text style details
            entity.setLineWidth(200);
            entity.setGlowColorOverride(org.bukkit.Color.fromRGB(0, 255, 128)); // Glowing Cyan/Green dot
            entity.setGlowing(true);
        });

        return arrow;
    }

    public void createArrow(Player player, Location destination) {
        removeArrow(player); // Clean up if existing
        
        PlayerData data = plugin.getExplorationManager().getPlayerData(player.getUniqueId());
        if (data != null && data.isShowArrow()) {
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
                    PlayerData data = plugin.getExplorationManager().getPlayerData(player.getUniqueId());

                    // 1. TextDisplay Entity 3D HUD crosshair radar management
                    if (data != null && data.isShowArrow()) {
                        TextDisplay arrow = activeArrows.get(uuid);
                        if (arrow == null || !arrow.isValid()) {
                            arrow = spawnArrowEntity(player);
                            activeArrows.put(uuid, arrow);
                        }

                        // Get player view center vector
                        Location eyeLoc = player.getEyeLocation();
                        Vector lookDir = eyeLoc.getDirection().normalize();

                        // Get vector pointing directly towards the destination POI
                        Vector dirToTarget = dest.toVector().subtract(eyeLoc.toVector()).normalize();

                        // Calculate horizontal right vector perpendicular to look direction
                        Vector right = new Vector(-lookDir.getZ(), 0, lookDir.getX()).normalize();
                        // Calculate vertical up vector perpendicular to look and right (cross product)
                        Vector up = lookDir.clone().crossProduct(right).normalize().multiply(-1);

                        // Evaluate direction dot alignments
                        double horizontalDiff = dirToTarget.dot(right);
                        double verticalDiff = dirToTarget.dot(up);
                        double centerMatch = lookDir.dot(dirToTarget);

                        // Set baseline crosshair target (1.2 blocks directly ahead of eyes)
                        Location targetLoc = eyeLoc.clone().add(lookDir.multiply(1.2));

                        // Scale offset limits (how far the arrow shifts off center on screen)
                        double maxOffset = 0.16; // Elegant tight orbiting radius
                        double xOffset = horizontalDiff * maxOffset;
                        double yOffset = verticalDiff * maxOffset;

                        // Circular clamping to maintain clean radar HUD boundaries
                        double len = Math.sqrt(xOffset * xOffset + yOffset * yOffset);
                        if (len > maxOffset) {
                            xOffset = (xOffset / len) * maxOffset;
                            yOffset = (yOffset / len) * maxOffset;
                        }

                        // Offset the TextDisplay location horizontal/vertical relative to view screen
                        targetLoc.add(right.multiply(xOffset)).add(up.multiply(yOffset));

                        // Dynamic hud symbols
                        if (centerMatch > 0.99) { // Centered view within ~8 degrees
                            arrow.setText("●"); // Clean alignment dot
                            arrow.setGlowColorOverride(org.bukkit.Color.fromRGB(0, 255, 128)); // Glowing Cyan/Green
                        } else {
                            arrow.setGlowColorOverride(org.bukkit.Color.fromRGB(255, 128, 0)); // Warning Orange-Red
                            
                            // Check dominant direction component
                            if (Math.abs(horizontalDiff) > Math.abs(verticalDiff)) {
                                if (horizontalDiff < 0) {
                                    arrow.setText("◀"); // Target is to your left
                                } else {
                                    arrow.setText("▶"); // Target is to your right
                                }
                            } else {
                                if (verticalDiff < 0) {
                                    arrow.setText("▼"); // Target is below you
                                } else {
                                    arrow.setText("▲"); // Target is above you
                                }
                            }
                        }

                        arrow.teleport(targetLoc);
                    } else {
                        // Destroy active entity if the visual style was changed/disabled
                        TextDisplay arrow = activeArrows.get(uuid);
                        if (arrow != null) {
                            arrow.remove();
                            activeArrows.put(uuid, null);
                        }
                    }

                    // 2. Shiny Particle Trail management (Locking altitude to player Y)
                    if (data != null && data.isShowTrail()) {
                        spawnParticleTrail(player, dest);
                    }
                }
            }
        };
        tickTask.runTaskTimer(plugin, 0, 2); // Smooth updates every 2 ticks
    }

    /**
     * Spawns a series of flying sparkles pointing forward towards the target direction
     * strictly at the player's Y altitude (preventing trail from dipping under ground)
     */
    private void spawnParticleTrail(Player player, Location target) {
        Location start = player.getEyeLocation().subtract(0, 0.4, 0);
        
        // Match target elevation strictly to player altitude to keep trail horizontal in front of player
        Location horizontalTarget = new Location(target.getWorld(), target.getX(), start.getY(), target.getZ());
        Vector direction = horizontalTarget.toVector().subtract(start.toVector()).normalize();

        // Spawn a stream of beautiful green-cyan sparkles pointing the way
        for (int i = 1; i <= 6; i++) {
            Location particleLoc = start.clone().add(direction.clone().multiply(i * 2.0));
            player.spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }
}
