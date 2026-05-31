package com.herenavy.herenavy.gui;

import com.herenavy.herenavy.HereNavyPlugin;
import com.herenavy.herenavy.gui.ExplorerGUI.POIEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class AdminGUI {

    private final HereNavyPlugin plugin;
    public static final String ADMIN_TITLE = "<red><bold>🛠 Admin: Structure Tracker 🛠</bold></red>";

    // Player UI Session tracking for admin
    private final Map<UUID, String> activeAdminSessions = new HashMap<>();

    public AdminGUI(HereNavyPlugin plugin) {
        this.plugin = plugin;
    }

    private String getOrCreateAdminSession(UUID uuid) {
        return activeAdminSessions.computeIfAbsent(uuid, k -> "OVERWORLD");
    }

    /**
     * Opens the administrative structure tracking control panel
     */
    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize(ADMIN_TITLE));

        String cat = getOrCreateAdminSession(player.getUniqueId());

        // Fill borders & fillers
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, getFillerItem(Material.BLACK_STAINED_GLASS_PANE));
        }
        for (int col = 0; col < 9; col++) {
            inv.setItem(col, getBorderItem());
            inv.setItem(45 + col, getBorderItem());
        }
        inv.setItem(9, getBorderItem());
        inv.setItem(17, getBorderItem());
        inv.setItem(18, getBorderItem());
        inv.setItem(26, getBorderItem());
        inv.setItem(27, getBorderItem());
        inv.setItem(35, getBorderItem());
        inv.setItem(36, getBorderItem());
        inv.setItem(44, getBorderItem());

        // Draw Dimension Tabs (Row 0: slots 2, 4, 6)
        inv.setItem(2, getTabItem(Material.GRASS_BLOCK, "<green><bold>Overworld Structures</bold></green>", cat.equals("OVERWORLD")));
        inv.setItem(4, getTabItem(Material.NETHERRACK, "<red><bold>Nether Structures</bold></red>", cat.equals("NETHER")));
        inv.setItem(6, getTabItem(Material.END_STONE, "<yellow><bold>The End Structures</bold></yellow>", cat.equals("THE_END")));

        // Return button in slot 49
        inv.setItem(49, getBackItem());

        // Filter and map landmarks matching active category
        List<POIEntry> structureEntries = plugin.getExplorerGUI().getStructureEntries();
        List<POIEntry> activeStructures = new ArrayList<>();
        for (POIEntry entry : structureEntries) {
            if (entry.getDimension().equals(cat)) {
                activeStructures.add(entry);
            }
        }

        // Fill grid symmetrically (Row 1-4, column 1-7 = 28 slots per page)
        int structIndex = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                if (structIndex < activeStructures.size()) {
                    POIEntry entry = activeStructures.get(structIndex++);
                    boolean tracked = plugin.getConfigManager().isStructureTracked(entry.getKey());
                    inv.setItem(row * 9 + col, getAdminPOIItem(entry, tracked));
                }
            }
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.0f);
    }

    private ItemStack getBorderItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(""));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getFillerItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(""));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getBackItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<red><bold>◀ Close Admin Menu</bold></red>"));
            meta.lore(List.of(
                MiniMessage.miniMessage().deserialize("<gray>Click to close this screen.</gray>")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getTabItem(Material material, String name, boolean active) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            List<Component> lore = new ArrayList<>();
            if (active) {
                lore.add(MiniMessage.miniMessage().deserialize("<green>● SELECTED VIEW ●</green>"));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Click to view this dimension.</gray>"));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Builds structure control panels inside the Admin GUI
     */
    private ItemStack getAdminPOIItem(POIEntry entry, boolean tracked) {
        ItemStack item = new ItemStack(entry.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(entry.getDisplayName()));
            
            // Set NamespacedKey metadata tag
            NamespacedKey key = plugin.getExplorerGUI().getPoiKey();
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, entry.getKey());

            if (tracked) {
                meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<green><bold>✔ STATUS: TRACKED</bold></green>"),
                    MiniMessage.miniMessage().deserialize("<gray>Players can navigate to and discover this structure.</gray>"),
                    MiniMessage.miniMessage().deserialize(""),
                    MiniMessage.miniMessage().deserialize("<red>Click to DISABLE tracking</red>")
                ));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<red><bold>❌ STATUS: DISABLED</bold></red>"),
                    MiniMessage.miniMessage().deserialize("<gray>Disabled structures are hidden from player maps</gray>"),
                    MiniMessage.miniMessage().deserialize("<gray>and skip discovery/BlueMap registration checks.</gray>"),
                    MiniMessage.miniMessage().deserialize(""),
                    MiniMessage.miniMessage().deserialize("<green>Click to ENABLE tracking</green>")
                ));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Handles administrative toggles inside the tracker dashboard
     */
    public void handleGUIClick(Player player, int slot) {
        ItemStack clickedItem = player.getOpenInventory().getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Material type = clickedItem.getType();
        if (type == Material.RED_STAINED_GLASS_PANE || type == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        // Close/Exit button
        if (type == Material.BARRIER) {
            activeAdminSessions.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }

        // Handle category tab clicks (slots 2, 4, 6)
        if (slot == 2) {
            activeAdminSessions.put(player.getUniqueId(), "OVERWORLD");
            player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.2f);
            openGUI(player);
            return;
        }
        if (slot == 4) {
            activeAdminSessions.put(player.getUniqueId(), "NETHER");
            player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.2f);
            openGUI(player);
            return;
        }
        if (slot == 6) {
            activeAdminSessions.put(player.getUniqueId(), "THE_END");
            player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.2f);
            openGUI(player);
            return;
        }

        // Retrieve key from PersistentDataContainer
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        
        NamespacedKey pKey = plugin.getExplorerGUI().getPoiKey();
        String targetKey = meta.getPersistentDataContainer().get(pKey, PersistentDataType.STRING);
        if (targetKey == null) return;

        // Toggle tracking state in ConfigManager
        boolean currentlyTracked = plugin.getConfigManager().isStructureTracked(targetKey);
        plugin.getConfigManager().setStructureTracked(targetKey, !currentlyTracked);
        
        player.playSound(player.getLocation(), "block.note_block.chime", 1.0f, 1.4f);
        player.sendMessage(MiniMessage.miniMessage().deserialize(
            "<yellow>[HereNavy] Toggled tracking for " + formatKey(targetKey) + " to " + (!currentlyTracked ? "<green>ENABLED</green>" : "<red>DISABLED</red>") + "!</yellow>"
        ));

        // Re-open to refresh the GUI updates in real-time
        openGUI(player);
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
