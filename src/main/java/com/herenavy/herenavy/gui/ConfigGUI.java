package com.herenavy.herenavy.gui;

import com.herenavy.herenavy.HereNavyPlugin;
import com.herenavy.herenavy.progression.ExplorationManager;
import com.herenavy.herenavy.progression.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ConfigGUI {

    private final HereNavyPlugin plugin;
    private final ExplorationManager explorationManager;

    private static final String GUI_TITLE = "<blue><bold>🛠 Navigation Settings 🛠</bold></blue>";

    public ConfigGUI(HereNavyPlugin plugin, ExplorationManager explorationManager) {
        this.plugin = plugin;
        this.explorationManager = explorationManager;
    }

    /**
     * Opens the personal preferences GUI for the player
     */
    public void openGUI(Player player) {
        PlayerData data = explorationManager.getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to load profile.</red>"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 9, MiniMessage.miniMessage().deserialize(GUI_TITLE));

        // Slot 0: Border
        inv.setItem(0, getBorderItem());
        
        // Slot 1: 3D Arrow toggle
        inv.setItem(1, getArrowToggleItem(data));

        // Slot 2: Filler
        inv.setItem(2, getFillerItem());

        // Slot 3: Particle Trail toggle
        inv.setItem(3, getTrailToggleItem(data));

        // Slot 4: Filler
        inv.setItem(4, getFillerItem());

        // Slot 5: Skyrim Compass toggle
        inv.setItem(5, getCompassCycleItem(data));

        // Slot 6: Filler
        inv.setItem(6, getFillerItem());

        // Slot 7: Back to Cartography Map
        inv.setItem(7, getBackItem());

        // Slot 8: Border
        inv.setItem(8, getBorderItem());

        player.openInventory(inv);
        player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.0f);
    }

    private ItemStack getBorderItem() {
        ItemStack item = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(""));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getFillerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(""));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getArrowToggleItem(PlayerData data) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (data.isShowArrow()) {
                meta.displayName(MiniMessage.miniMessage().deserialize("<green><bold>3D Pointer Arrow: ENABLED</bold></green>"));
                meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>A glowing floating 3D arrow points the way</gray>"),
                    MiniMessage.miniMessage().deserialize("<gray>in front of your head in the world.</gray>"),
                    MiniMessage.miniMessage().deserialize(""),
                    MiniMessage.miniMessage().deserialize("<yellow>Click to DISABLE arrow!</yellow>")
                ));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.displayName(MiniMessage.miniMessage().deserialize("<red><bold>3D Pointer Arrow: DISABLED</bold></red>"));
                meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>No floating 3D pointing arrow will spawn</gray>"),
                    MiniMessage.miniMessage().deserialize("<gray>in front of your head.</gray>"),
                    MiniMessage.miniMessage().deserialize(""),
                    MiniMessage.miniMessage().deserialize("<yellow>Click to ENABLE arrow!</yellow>")
                ));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getTrailToggleItem(PlayerData data) {
        ItemStack item = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (data.isShowTrail()) {
                meta.displayName(MiniMessage.miniMessage().deserialize("<green><bold>Sparkle Trail: ENABLED</bold></green>"));
                meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>A trail of glowing green/cyan sparkles</gray>"),
                    MiniMessage.miniMessage().deserialize("<gray>flies toward the destination location.</gray>"),
                    MiniMessage.miniMessage().deserialize(""),
                    MiniMessage.miniMessage().deserialize("<yellow>Click to DISABLE trail!</yellow>")
                ));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.displayName(MiniMessage.miniMessage().deserialize("<red><bold>Sparkle Trail: DISABLED</bold></red>"));
                meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>No traveling visual particle sparkles will</gray>"),
                    MiniMessage.miniMessage().deserialize("<gray>fly toward your destination.</gray>"),
                    MiniMessage.miniMessage().deserialize(""),
                    MiniMessage.miniMessage().deserialize("<yellow>Click to ENABLE trail!</yellow>")
                ));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getCompassCycleItem(PlayerData data) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>Skyrim Compass HUD Style</bold></gold>"));
            
            List<Component> lore = new ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize("<gray>Current Mode: </gray><yellow><bold>" + data.getCompassStyle() + "</bold></yellow>"));
            lore.add(MiniMessage.miniMessage().deserialize(""));
            
            String comp = data.getCompassStyle();
            lore.add(MiniMessage.miniMessage().deserialize(comp.equals("ACTIONBAR") ? "<green>✔ Action Bar</green>" : "<gray>  Action Bar (AuraSkills conflicts)</gray>"));
            lore.add(MiniMessage.miniMessage().deserialize(comp.equals("BOSSBAR") ? "<green>✔ Boss Bar (Top Screen)</green>" : "<gray>  Boss Bar (Top Screen)</gray>"));
            lore.add(MiniMessage.miniMessage().deserialize(comp.equals("OFF") ? "<green>✔ Disabled</green>" : "<gray>  Disabled</gray>"));
            
            lore.add(MiniMessage.miniMessage().deserialize(""));
            lore.add(MiniMessage.miniMessage().deserialize("<yellow>Click to cycle styles!</yellow>"));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getBackItem() {
        ItemStack item = new ItemStack(Material.MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<yellow><bold>◀ Return to Map</bold></yellow>"));
            meta.lore(List.of(
                MiniMessage.miniMessage().deserialize("<gray>Click to open the Cartography Map GUI</gray>"),
                MiniMessage.miniMessage().deserialize("<gray>to select a landmark or track new biomes.</gray>")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Intercepts InventoryClicks inside this settings GUI
     */
    public void handleGUIClick(Player player, int slot) {
        PlayerData data = explorationManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        switch (slot) {
            case 1:
                data.setShowArrow(!data.isShowArrow());
                explorationManager.savePlayerData(data);
                player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                openGUI(player); // Refresh
                break;

            case 3:
                data.setShowTrail(!data.isShowTrail());
                explorationManager.savePlayerData(data);
                player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.0f);
                openGUI(player); // Refresh
                break;

            case 5:
                String next = "ACTIONBAR";
                String current = data.getCompassStyle();
                if (current.equals("ACTIONBAR")) {
                    next = "BOSSBAR";
                } else if (current.equals("BOSSBAR")) {
                    next = "OFF";
                }
                data.setCompassStyle(next);
                explorationManager.savePlayerData(data);
                
                player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.2f);
                openGUI(player); // Refresh
                break;

            case 7:
                plugin.getExplorerGUI().openGUI(player);
                break;
        }
    }
}
