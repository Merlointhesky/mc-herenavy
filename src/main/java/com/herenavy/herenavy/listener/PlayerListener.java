package com.herenavy.herenavy.listener;

import com.herenavy.herenavy.HereNavyPlugin;
import com.herenavy.herenavy.gui.ExplorerGUI;
import com.herenavy.herenavy.navigation.NavigationManager;
import com.herenavy.herenavy.progression.ExplorationManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public final class PlayerListener implements Listener {

    private final HereNavyPlugin plugin;
    private final ExplorationManager explorationManager;
    private final NavigationManager navigationManager;
    private final ExplorerGUI explorerGUI;
    private final com.herenavy.herenavy.gui.ConfigGUI configGUI;

    public PlayerListener(HereNavyPlugin plugin, ExplorationManager explorationManager, NavigationManager navigationManager, ExplorerGUI explorerGUI, com.herenavy.herenavy.gui.ConfigGUI configGUI) {
        this.plugin = plugin;
        this.explorationManager = explorationManager;
        this.navigationManager = navigationManager;
        this.explorerGUI = explorerGUI;
        this.configGUI = configGUI;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Load exploration profile
        explorationManager.loadPlayerData(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Stop active navigation visual tracking
        navigationManager.stopNavigation(player, false);
        // Unload and persist exploration profiles
        explorationManager.unloadPlayerData(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = MiniMessage.miniMessage().serialize(event.getView().title());
        
        // 1. Verify if it's the Cartography Map custom double chest
        if (title.contains("Cartography Map")) {
            event.setCancelled(true);

            if (event.getRawSlot() >= event.getInventory().getSize()) {
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            explorerGUI.handleGUIClick(player, event.getRawSlot(), clicked);
        }
        
        // 2. Verify if it's the Navigation Settings custom menu
        else if (title.contains("Navigation Settings")) {
            event.setCancelled(true);

            if (event.getRawSlot() >= event.getInventory().getSize()) {
                return;
            }

            configGUI.handleGUIClick(player, event.getRawSlot());
        }
    }
}
