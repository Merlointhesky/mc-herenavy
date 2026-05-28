package com.herenavy.herenavy.gui;

import com.herenavy.herenavy.HereNavyPlugin;
import com.herenavy.herenavy.gui.ExplorerGUI.POIEntry;
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

import java.util.List;

public final class AdminGUI {

    private final HereNavyPlugin plugin;
    public static final String ADMIN_TITLE = "<red><bold>🛠 Admin: Structure Tracker 🛠</bold></red>";

    public AdminGUI(HereNavyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the administrative structure tracking control panel
     */
    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize(ADMIN_TITLE));

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

        // Symmetrically map the 28 landmarks to center grids
        List<POIEntry> structureEntries = plugin.getExplorerGUI().getStructureEntries();
        int structIndex = 0;
        
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                if (structIndex < structureEntries.size()) {
                    POIEntry entry = structureEntries.get(structIndex++);
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

    /**
     * Builds structure control panels inside the Admin GUI
     */
    private ItemStack getAdminPOIItem(POIEntry entry, boolean tracked) {
        ItemStack item = new ItemStack(entry.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(entry.getDisplayName()));

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
        // Validate slot bounds (center grid matches row 1-4, column 1-7)
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > 4 || col < 1 || col > 7) return;

        // Flatten coordinates to find matched structure
        int index = (row - 1) * 7 + (col - 1);
        List<POIEntry> structureEntries = plugin.getExplorerGUI().getStructureEntries();
        if (index < 0 || index >= structureEntries.size()) return;

        POIEntry entry = structureEntries.get(index);
        boolean currentlyTracked = plugin.getConfigManager().isStructureTracked(entry.getKey());
        
        // Toggle tracking state in ConfigManager
        plugin.getConfigManager().setStructureTracked(entry.getKey(), !currentlyTracked);
        
        player.playSound(player.getLocation(), "block.note_block.chime", 1.0f, 1.4f);
        player.sendMessage(MiniMessage.miniMessage().deserialize(
            "<yellow>[HereNavy] Toggled tracking for " + formatKey(entry.getKey()) + " to " + (!currentlyTracked ? "<green>ENABLED</green>" : "<red>DISABLED</red>") + "!</yellow>"
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
