package com.herenavy.herenavy.gui;

import com.herenavy.herenavy.HereNavyPlugin;
import com.herenavy.herenavy.config.ConfigManager;
import com.herenavy.herenavy.navigation.NavigationManager;
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

import java.util.*;

public final class ExplorerGUI {

    private final HereNavyPlugin plugin;
    private final ConfigManager configManager;
    private final ExplorationManager explorationManager;
    private final NavigationManager navigationManager;

    private static final String GUI_TITLE = "<blue><bold>🧭 Cartography Map 🧭</bold></blue>";

    // Biome entries mapping
    private final List<POIEntry> biomeEntries = new ArrayList<>();
    // Structure entries mapping
    private final List<POIEntry> structureEntries = new ArrayList<>();

    public static final class POIEntry {
        private final String key;
        private final Material material;
        private final String displayName;

        public POIEntry(String key, Material material, String displayName) {
            this.key = key;
            this.material = material;
            this.displayName = displayName;
        }

        public String getKey() { return key; }
        public Material getMaterial() { return material; }
        public String getDisplayName() { return displayName; }
    }

    public ExplorerGUI(HereNavyPlugin plugin, ConfigManager configManager, ExplorationManager explorationManager, NavigationManager navigationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.explorationManager = explorationManager;
        this.navigationManager = navigationManager;
        setupPOIEntries();
    }

    private void setupPOIEntries() {
        // Biomes
        biomeEntries.add(new POIEntry("minecraft:plains", Material.GRASS_BLOCK, "<green>Plains Biome</green>"));
        biomeEntries.add(new POIEntry("minecraft:forest", Material.OAK_SAPLING, "<green>Forest Biome</green>"));
        biomeEntries.add(new POIEntry("minecraft:desert", Material.SAND, "<yellow>Desert Biome</yellow>"));
        biomeEntries.add(new POIEntry("minecraft:ocean", Material.WATER_BUCKET, "<blue>Ocean Biome</blue>"));
        biomeEntries.add(new POIEntry("minecraft:dark_forest", Material.DARK_OAK_LOG, "<dark_green>Dark Forest Biome</dark_green>"));
        biomeEntries.add(new POIEntry("minecraft:swamp", Material.LILY_PAD, "<dark_aqua>Swamp Biome</dark_aqua>"));
        biomeEntries.add(new POIEntry("minecraft:jungle", Material.JUNGLE_LEAVES, "<green>Jungle Biome</green>"));
        biomeEntries.add(new POIEntry("minecraft:savanna", Material.ACACIA_LOG, "<gold>Savanna Biome</gold>"));
        biomeEntries.add(new POIEntry("minecraft:badlands", Material.TERRACOTTA, "<red>Badlands Biome</red>"));
        biomeEntries.add(new POIEntry("minecraft:cherry_grove", Material.CHERRY_SAPLING, "<light_purple>Cherry Grove Biome</light_purple>"));
        biomeEntries.add(new POIEntry("minecraft:flower_forest", Material.ROSE_BUSH, "<light_purple>Flower Forest Biome</light_purple>"));
        biomeEntries.add(new POIEntry("minecraft:nether_wastes", Material.NETHERRACK, "<dark_red>Nether Wastes Biome</dark_red>"));
        biomeEntries.add(new POIEntry("minecraft:basalt_deltas", Material.BASALT, "<gray>Basalt Deltas Biome</gray>"));
        biomeEntries.add(new POIEntry("minecraft:the_end", Material.END_STONE, "<yellow>The End Biome</yellow>"));

        // Structures
        structureEntries.add(new POIEntry("minecraft:village_plains", Material.WHEAT, "<yellow>Village Landmark</yellow>"));
        structureEntries.add(new POIEntry("minecraft:mineshaft", Material.IRON_PICKAXE, "<gray>Abandoned Mineshaft</gray>"));
        structureEntries.add(new POIEntry("minecraft:pillager_outpost", Material.CROSSBOW, "<red>Pillager Outpost</red>"));
        structureEntries.add(new POIEntry("minecraft:desert_pyramid", Material.CHISELED_SANDSTONE, "<gold>Desert Pyramid</gold>"));
        structureEntries.add(new POIEntry("minecraft:jungle_pyramid", Material.MOSSY_COBBLESTONE, "<green>Jungle Temple</green>"));
        structureEntries.add(new POIEntry("minecraft:ocean_monument", Material.PRISMARINE_BRICKS, "<aqua>Ocean Monument</aqua>"));
        structureEntries.add(new POIEntry("minecraft:mansion", Material.DARK_OAK_DOOR, "<dark_green>Woodland Mansion</dark_green>"));
        structureEntries.add(new POIEntry("minecraft:bastion_remnant", Material.GILDED_BLACKSTONE, "<gold>Bastion Remnant</gold>"));
        structureEntries.add(new POIEntry("minecraft:stronghold", Material.ENDER_EYE, "<dark_purple>Stronghold Keep</dark_purple>"));
    }

    /**
     * Creates and opens the double-chest GUI for the player
     */
    public void openGUI(Player player) {
        PlayerData data = explorationManager.getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to load profile.</red>"));
            return;
        }

        int lvl = data.getLevel();
        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize(GUI_TITLE));

        // 1. Populate top 3 rows with biomes (always unlocked)
        int biomeIndex = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                // Add borders/fillers at side edges
                if (col == 0 || col == 8) {
                    inv.setItem(slot, getBorderItem());
                    continue;
                }
                
                if (biomeIndex < biomeEntries.size()) {
                    POIEntry entry = biomeEntries.get(biomeIndex++);
                    boolean completed = data.hasDiscoveredBiome(entry.getKey());
                    inv.setItem(slot, getPOIItem(entry, true, completed, 1));
                } else {
                    inv.setItem(slot, getFillerItem());
                }
            }
        }

        // 2. Middle Row 3 Divider
        for (int col = 0; col < 9; col++) {
            inv.setItem(27 + col, getDividerItem());
        }

        // 3. Populate bottom 2 rows with structures (locked by exploration levels)
        int structIndex = 0;
        for (int row = 4; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                // Borders
                if (col == 0 || col == 8) {
                    inv.setItem(slot, getBorderItem());
                    continue;
                }

                if (structIndex < structureEntries.size()) {
                    POIEntry entry = structureEntries.get(structIndex++);
                    int reqLvl = configManager.getRequiredLevel(entry.getKey());
                    boolean unlocked = lvl >= reqLvl;
                    boolean completed = plugin.getStructureDiscoveryManager()
                        .getDiscoveredStructures(player.getUniqueId(), entry.getKey()).size() > 0;

                    inv.setItem(slot, getPOIItem(entry, unlocked, completed, reqLvl));
                } else {
                    inv.setItem(slot, getFillerItem());
                }
            }
        }

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

    private ItemStack getDividerItem() {
        ItemStack item = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gray>--- Biomes Above / Landmarks Below ---</gray>"));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Builds standard clickable POI items with visual locks/enchants
     */
    private ItemStack getPOIItem(POIEntry entry, boolean unlocked, boolean completed, int reqLevel) {
        if (!unlocked) {
            ItemStack lockedItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = lockedItem.getItemMeta();
            if (meta != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize("<red><bold>LOCKED LANDMARK</bold></red>"));
                meta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<gray>Requires Exploration Level: </gray><yellow>" + reqLevel + "</yellow>"),
                    MiniMessage.miniMessage().deserialize("<red>Explore biomes and dungeons to level up!</red>")
                ));
                lockedItem.setItemMeta(meta);
            }
            return lockedItem;
        }

        ItemStack item = new ItemStack(entry.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(entry.getDisplayName()));
            
            List<Component> lore = new ArrayList<>();
            if (completed) {
                lore.add(MiniMessage.miniMessage().deserialize("<green>✔ DISCOVERED MASTERY</green>"));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Click to track coordinates again</gray>"));
                // Add glowing enchantment
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(MiniMessage.miniMessage().deserialize("<yellow>⏰ UNVISITED LANDMARK</yellow>"));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Click to start visual navigation!</gray>"));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Intercepts InventoryClick events inside this GUI
     */
    public void handleGUIClick(Player player, int slot, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Ignore borders/fillers
        Material type = clickedItem.getType();
        if (type == Material.BLUE_STAINED_GLASS_PANE || type == Material.BLACK_STAINED_GLASS_PANE || type == Material.ORANGE_STAINED_GLASS_PANE) {
            return;
        }

        // Locked items
        if (type == Material.GRAY_STAINED_GLASS_PANE) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<red>That landmark is locked! Increase your Exploration level to track it.</red>"
            ));
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            return;
        }

        // Identify matched POI key
        POIEntry targetEntry = null;
        boolean isBiome = false;

        for (POIEntry entry : biomeEntries) {
            if (entry.getMaterial() == type) {
                targetEntry = entry;
                isBiome = true;
                break;
            }
        }

        if (targetEntry == null) {
            for (POIEntry entry : structureEntries) {
                if (entry.getMaterial() == type) {
                    targetEntry = entry;
                    break;
                }
            }
        }

        if (targetEntry == null) return;

        player.closeInventory();
        
        // Start navigation
        boolean success;
        if (isBiome) {
            success = navigationManager.startBiomeNavigation(player, targetEntry.getKey());
        } else {
            success = navigationManager.startStructureNavigation(player, targetEntry.getKey());
        }

        if (success) {
            player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 0.5f);
        }
    }
}
