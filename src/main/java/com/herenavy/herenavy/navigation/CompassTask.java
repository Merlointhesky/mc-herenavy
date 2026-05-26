package com.herenavy.herenavy.navigation;

import com.herenavy.herenavy.HereNavyPlugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public final class CompassTask extends BukkitRunnable {

    private final HereNavyPlugin plugin;
    private final NavigationManager navigationManager;

    // 32-point compass notches (each notch is 11.25 degrees)
    private static final String[] NOTCHES = {
        "N", "·", "·", "·", "NE", "·", "·", "·", 
        "E", "·", "·", "·", "SE", "·", "·", "·", 
        "S", "·", "·", "·", "SW", "·", "·", "·", 
        "W", "·", "·", "·", "NW", "·", "·", "·"
    };

    public CompassTask(HereNavyPlugin plugin, NavigationManager navigationManager) {
        this.plugin = plugin;
        this.navigationManager = navigationManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!navigationManager.isNavigating(player)) continue;

            NavigationManager.NavigationSession session = navigationManager.getSession(player);
            if (session == null) continue;

            com.herenavy.herenavy.progression.PlayerData data = plugin.getExplorationManager().getPlayerData(player.getUniqueId());
            if (data == null) continue;

            String style = data.getCompassStyle();
            if (style.equalsIgnoreCase("OFF")) {
                navigationManager.hideBossBar(player);
                continue;
            }

            String compassString = buildCompass(player, session.getDestination());
            Component component = MiniMessage.miniMessage().deserialize(compassString);

            if (style.equalsIgnoreCase("BOSSBAR")) {
                BossBar bar = navigationManager.getBossBarForPlayer(player, component);
                double distance = player.getLocation().distance(session.getDestination());
                float progress = (float) Math.clamp(1.0 - (distance / 2000.0), 0.0, 1.0);
                bar.progress(progress);
            } else {
                // Action bar or off. Clear any active boss bars first
                navigationManager.hideBossBar(player);
                if (style.equalsIgnoreCase("ACTIONBAR")) {
                    player.sendActionBar(component);
                }
            }
        }
    }

    /**
     * Constructs a custom scrolling Skyrim-style compass string centered around player yaw,
     * embedding a target marker [♦] at the exact relative offset angle.
     */
    private String buildCompass(Player player, Location target) {
        Location pLoc = player.getLocation();
        
        // Calculate player yaw normalized to 0..360 range
        // In Minecraft: -180 to 180 degrees. 0 = South, -90 = East, 90 = West, 180/-180 = North
        double pYaw = (player.getLocation().getYaw() + 180) % 360;
        if (pYaw < 0) pYaw += 360;

        // Calculate target heading yaw normalized to 0..360 range
        double dx = target.getX() - pLoc.getX();
        double dz = target.getZ() - pLoc.getZ();
        double targetYawRad = Math.atan2(-dx, dz);
        double tYaw = Math.toDegrees(targetYawRad); // -180 to 180
        tYaw = (tYaw + 180) % 360;
        if (tYaw < 0) tYaw += 360;

        // Calculate relative difference (heading error)
        double diff = tYaw - pYaw;
        if (diff < -180) diff += 360;
        if (diff > 180) diff -= 360;

        // Visual Slider Parameters:
        // We show 11 notches in total (5 notches left, player facing center notch, 5 notches right)
        // Total range visible is 11 * 11.25 = ~123.75 degrees
        int centerIndex = (int) Math.round(pYaw / 11.25) % 32;
        if (centerIndex < 0) centerIndex += 32;

        StringBuilder sb = new StringBuilder();
        sb.append("<white><bold>");

        for (int i = -5; i <= 5; i++) {
            int idx = (centerIndex + i) % 32;
            if (idx < 0) idx += 32;

            // Compute the angle of this specific notch relative to player's center view
            double notchAngleOffset = i * 11.25;

            // Check if the target heading diff lands inside this notch's interval (halfway to next notch)
            // Interval size is 11.25 degrees centered around the notch
            double minRange = notchAngleOffset - 5.625;
            double maxRange = notchAngleOffset + 5.625;

            // Highlight cardinal directions beautifully
            String notchText = NOTCHES[idx];
            if (notchText.equals("N") || notchText.equals("E") || notchText.equals("S") || notchText.equals("W")) {
                notchText = "<gold>" + notchText + "</gold>";
            } else if (notchText.equals("NE") || notchText.equals("SE") || notchText.equals("SW") || notchText.equals("NW")) {
                notchText = "<yellow>" + notchText + "</yellow>";
            } else {
                notchText = "<gray>" + notchText + "</gray>";
            }

            if (diff >= minRange && diff < maxRange) {
                // Target is active on this notch! Embed the indicator marker
                sb.append("<red><bold>[♦]</bold></red>");
            } else {
                sb.append(notchText);
            }

            if (i < 5) {
                sb.append("  "); // Add premium spacing between notches
            }
        }

        sb.append("</bold></white>");

        // Append coordinates and distance info at the side of the compass
        double distance = pLoc.distance(target);
        sb.append("  <gray>[<yellow>").append((int) distance).append("m</yellow>]</gray>");

        return sb.toString();
    }
}
