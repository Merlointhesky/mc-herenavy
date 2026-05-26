package com.herenavy.herenavy.command;

import com.herenavy.herenavy.HereNavyPlugin;
import com.herenavy.herenavy.config.ConfigManager;
import com.herenavy.herenavy.gui.ExplorerGUI;
import com.herenavy.herenavy.navigation.NavigationManager;
import com.herenavy.herenavy.progression.ExplorationManager;
import com.herenavy.herenavy.progression.PlayerData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class HereNavyCommand implements CommandExecutor, TabCompleter {

    private final HereNavyPlugin plugin;
    private final ConfigManager configManager;
    private final ExplorationManager explorationManager;
    private final NavigationManager navigationManager;
    private final ExplorerGUI explorerGUI;
    private final com.herenavy.herenavy.gui.ConfigGUI configGUI;

    public HereNavyCommand(HereNavyPlugin plugin, ConfigManager configManager, ExplorationManager explorationManager, NavigationManager navigationManager, ExplorerGUI explorerGUI, com.herenavy.herenavy.gui.ConfigGUI configGUI) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.explorationManager = explorationManager;
        this.navigationManager = navigationManager;
        this.explorerGUI = explorerGUI;
        this.configGUI = configGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can execute HereNavy commands.</red>"));
            return true;
        }

        if (!player.hasPermission("herenavy.use")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You do not have permission to use HereNavy.</red>"));
            return true;
        }

        if (args.length == 0) {
            // Default: open the Explorer GUI
            explorerGUI.openGUI(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start":
                explorerGUI.openGUI(player);
                break;

            case "config":
                configGUI.openGUI(player);
                break;

            case "stop":
                if (!navigationManager.isNavigating(player)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>You are not currently navigating to any POI.</yellow>"));
                } else {
                    navigationManager.stopNavigation(player, false);
                }
                break;

            case "info":
                sendPlayerInfo(player);
                break;

            case "go":
                handleManualGo(player, args);
                break;

            default:
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Unknown subcommand! Use: /hn <info|start|stop|go|config></red>"
                ));
                break;
        }

        return true;
    }

    /**
     * Renders a premium status and experience progress bar for /hn info
     */
    private void sendPlayerInfo(Player player) {
        PlayerData data = explorationManager.getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to load your exploration profile.</red>"));
            return;
        }

        int maxLvl = configManager.getMaxLevel();
        int lvl = data.getLevel();
        int currentExp = data.getExp();
        int reqExp = explorationManager.getRequiredExpForNextLevel(lvl);

        // Progress bar formatting
        int totalSegments = 20;
        float percent = (lvl >= maxLvl) ? 1.0f : (float) currentExp / reqExp;
        int activeSegments = Math.round(percent * totalSegments);

        StringBuilder bar = new StringBuilder();
        bar.append("<gold>[");
        for (int i = 0; i < totalSegments; i++) {
            if (i < activeSegments) {
                bar.append("=");
            } else if (i == activeSegments) {
                bar.append(">");
            } else {
                bar.append("-");
            }
        }
        bar.append("]</gold>");

        String expText = (lvl >= maxLvl) ? "MAX LEVEL" : currentExp + " / " + reqExp + " EXP";
        String targetName = navigationManager.isNavigating(player) ? navigationManager.getNavigationTargetName(player) : "None";
        if (targetName.contains(":")) targetName = targetName.split(":")[1];

        player.sendMessage(MiniMessage.miniMessage().deserialize(
            "\n<gold><bold>🧭 EXPLORER PROFILE: " + player.getName() + " 🧭</bold></gold>\n" +
            "<gray>Exploration Level: </gray><yellow><bold>" + lvl + "</bold></yellow> / <yellow>" + maxLvl + "</yellow>\n" +
            "<gray>Progression: </gray>" + bar.toString() + " <yellow>" + (int)(percent * 100) + "%</yellow> (" + expText + ")\n" +
            "<gray>Discovered Biomes: </gray><yellow>" + data.getDiscoveredBiomes().size() + "</yellow>\n" +
            "<gray>Current Navigation: </gray><green>" + formatName(targetName) + "</green>\n"
        ));
    }

    /**
     * Handles starting a manual coordinate tracking session
     */
    private void handleManualGo(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /hn go <x> <y> <z></red>"));
            return;
        }

        try {
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);

            Location target = new Location(player.getWorld(), x, y, z);
            navigationManager.startManualNavigation(player, target);
        } catch (NumberFormatException e) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Coordinates must be valid numbers!</red>"));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (!sender.hasPermission("herenavy.use")) return list;

        if (args.length == 1) {
            List<String> subs = Arrays.asList("info", "start", "stop", "go", "config");
            String query = args[0].toLowerCase();
            for (String s : subs) {
                if (s.startsWith(query)) {
                    list.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("go")) {
            if (sender instanceof Player player) {
                list.add(String.valueOf(player.getLocation().getBlockX()));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("go")) {
            if (sender instanceof Player player) {
                list.add(String.valueOf(player.getLocation().getBlockY()));
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("go")) {
            if (sender instanceof Player player) {
                list.add(String.valueOf(player.getLocation().getBlockZ()));
            }
        }
        return list;
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
}
