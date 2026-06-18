package com.herenavy.herenavy.listener;

import com.herenavy.herenavy.HereNavyPlugin;
import com.herenavy.herenavy.gui.ExplorerGUI;
import com.herenavy.herenavy.navigation.NavigationManager;
import com.herenavy.herenavy.progression.ExplorationManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
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
        
        // 1. Verify if it's any of our Cartography-related custom menus
        if (title.contains("Cartography Map") || title.contains("Biomes Map") || title.contains("Landmarks Map")) {
            event.setCancelled(true);

            if (event.getRawSlot() >= event.getInventory().getSize()) {
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            explorerGUI.handleGUIClick(player, title, event.getRawSlot(), clicked);
        }
        
        // 2. Verify if it's the Navigation Settings custom menu
        else if (title.contains("Navigation Settings")) {
            event.setCancelled(true);

            if (event.getRawSlot() >= event.getInventory().getSize()) {
                return;
            }

            configGUI.handleGUIClick(player, event.getRawSlot());
        }
        
        // 3. Verify if it's the Admin Structure Tracker custom menu
        else if (title.contains("Admin: Structure Tracker")) {
            event.setCancelled(true);

            if (event.getRawSlot() >= event.getInventory().getSize()) {
                return;
            }

            plugin.getAdminGUI().handleGUIClick(player, event.getRawSlot());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = MiniMessage.miniMessage().serialize(event.getView().title());
        if (title.contains("Cartography Map") || title.contains("Biomes Map") || title.contains("Landmarks Map") ||
            title.contains("Navigation Settings") || title.contains("Admin: Structure Tracker")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getStructureDiscoveryManager().isInNamingSession(player.getUniqueId())) {
            event.setCancelled(true);
            
            // Extract plaintext from Adventure Component
            String nameInput = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            
            // Run processing on the main thread safely
            Bukkit.getScheduler().runTask(plugin, () -> {
                com.herenavy.herenavy.progression.StructureDiscoveryManager.StructureRecord record = 
                        plugin.getStructureDiscoveryManager().getNamingSessionRecord(player.getUniqueId());
                
                if (record == null) return;
                
                plugin.getStructureDiscoveryManager().endNamingSession(player.getUniqueId());
                
                String finalName = nameInput;
                if (finalName.equalsIgnoreCase("cancel") || finalName.equalsIgnoreCase("default") || finalName.isEmpty()) {
                    int villageNumber = plugin.getStructureDiscoveryManager().getNextVillageNumber();
                    finalName = "Village " + villageNumber;
                    record.setCustomName(finalName);
                    plugin.getStructureDiscoveryManager().saveRecord(record);
                    
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Settlement registered with the default name: </green><yellow><bold>" + finalName + "</bold></yellow>!"));
                    player.playSound(player.getLocation(), "block.note_block.iron_xylophone", 1.0f, 1.0f);
                } else {
                    // Truncate length to prevent map overflow
                    if (finalName.length() > 32) {
                        finalName = finalName.substring(0, 32);
                    }
                    record.setCustomName(finalName);
                    plugin.getStructureDiscoveryManager().saveRecord(record);
                    
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Successfully named the village to: </green><yellow><bold>" + finalName + "</bold></yellow>!"));
                    player.playSound(player.getLocation(), "ui.toast.challenge_complete", 1.0f, 1.4f);
                    
                    // Broadcast the epic exploration achievement!
                    Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                        "<gold><bold>✍ HISTORIC NAMING! ✍</bold></gold> " +
                        "<yellow><bold>" + player.getName() + "</bold> has named the newly discovered settlement at " +
                        "[" + (int)record.getX() + ", " + (int)record.getY() + ", " + (int)record.getZ() + "]: <gold><bold>" + finalName + "</bold></gold>!</yellow>"
                    ));
                }
                
                // Finally, add it to BlueMap with the custom name!
                if (plugin.getBlueMapHook() != null && plugin.getConfigManager().isStructureTracked(record.getType())) {
                    plugin.getBlueMapHook().addMarker(record);
                }
            });
        }
    }
}
