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

    public static final String SELECTION_TITLE = "<blue><bold>🧭 Cartography Map 🧭</bold></blue>";
    public static final String BIOMES_TITLE = "<green><bold>🌿 Biomes Map 🌿</bold></green>";
    public static final String LANDMARKS_TITLE = "<gold><bold>🏰 Landmarks Map 🏰</bold></gold>";

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
        // 28 Biomes
        biomeEntries.add(new POIEntry("minecraft:plains", Material.GRASS_BLOCK, "<green>Plains Biome</green>"));
        biomeEntries.add(new POIEntry("minecraft:forest", Material.OAK_SAPLING, "<green>Forest Biome</green>"));
        biomeEntries.add(new POIEntry("minecraft:desert", Material.SAND, "<yellow>Desert Biome</yellow>"));
        biomeEntries.add(new POIEntry("minecraft:ocean", Material.WATER_BUCKET, "<blue>Ocean Biome</blue>"));
        biomeEntries.add(new POIEntry("minecraft:meadow", Material.SUNFLOWER, "<yellow>Meadow Biome</yellow>"));
        biomeEntries.add(new POIEntry("minecraft:dark_forest", Material.DARK_OAK_LOG, "<dark_green>Dark Forest Biome</dark_green>"));
        biomeEntries.add(new POIEntry("minecraft:swamp", Material.LILY_PAD, "<dark_aqua>Swamp Biome</dark_aqua>"));
        biomeEntries.add(new POIEntry("minecraft:jungle", Material.JUNGLE_LEAVES, "<green>Jungle Biome</green>"));
        biomeEntries.add(new POIEntry("minecraft:dripstone_caves", Material.POINTED_DRIPSTONE, "<gray>Dripstone Caves</gray>"));
        biomeEntries.add(new POIEntry("minecraft:savanna", Material.ACACIA_LOG, "<gold>Savanna Biome</gold>"));
        biomeEntries.add(new POIEntry("minecraft:badlands", Material.TERRACOTTA, "<red>Badlands Biome</red>"));
        biomeEntries.add(new POIEntry("minecraft:lush_caves", Material.GLOW_BERRIES, "<green>Lush Caves</green>"));
        biomeEntries.add(new POIEntry("minecraft:mangrove_swamp", Material.MANGROVE_PROPAGULE, "<dark_red>Mangrove Swamp</dark_red>"));
        biomeEntries.add(new POIEntry("minecraft:grove", Material.SNOW_BLOCK, "<white>Snowy Grove</white>"));
        biomeEntries.add(new POIEntry("minecraft:cherry_grove", Material.CHERRY_SAPLING, "<light_purple>Cherry Grove Biome</light_purple>"));
        biomeEntries.add(new POIEntry("minecraft:ice_spikes", Material.PACKED_ICE, "<aqua>Ice Spikes</aqua>"));
        biomeEntries.add(new POIEntry("minecraft:frozen_peaks", Material.BLUE_ICE, "<aqua>Frozen Peaks</aqua>"));
        biomeEntries.add(new POIEntry("minecraft:mushroom_fields", Material.RED_MUSHROOM_BLOCK, "<light_purple>Mushroom Fields</light_purple>"));
        biomeEntries.add(new POIEntry("minecraft:flower_forest", Material.ROSE_BUSH, "<light_purple>Flower Forest Biome</light_purple>"));
        biomeEntries.add(new POIEntry("minecraft:nether_wastes", Material.NETHERRACK, "<dark_red>Nether Wastes Biome</dark_red>"));
        biomeEntries.add(new POIEntry("minecraft:basalt_deltas", Material.BASALT, "<gray>Basalt Deltas Biome</gray>"));
        biomeEntries.add(new POIEntry("minecraft:crimson_forest", Material.CRIMSON_FUNGUS, "<red>Crimson Forest</red>"));
        biomeEntries.add(new POIEntry("minecraft:warped_forest", Material.WARPED_FUNGUS, "<cyan>Warped Forest</cyan>"));
        biomeEntries.add(new POIEntry("minecraft:soul_sand_valley", Material.SOUL_SAND, "<brown>Soul Sand Valley</brown>"));
        biomeEntries.add(new POIEntry("minecraft:the_end", Material.END_STONE, "<yellow>The End Biome</yellow>"));
        biomeEntries.add(new POIEntry("minecraft:deep_dark", Material.SCULK, "<dark_blue>Deep Dark Biome</dark_blue>"));
        biomeEntries.add(new POIEntry("minecraft:small_end_islands", Material.CHORUS_FLOWER, "<yellow>End Islands Biome</yellow>"));
        biomeEntries.add(new POIEntry("minecraft:end_highlands", Material.CHORUS_PLANT, "<yellow>End Highlands Biome</yellow>"));

        // 28 Structures (Perfect Grid Sizing)
        structureEntries.add(new POIEntry("minecraft:village_plains", Material.WHEAT, "<yellow>Plains Village</yellow>"));
        structureEntries.add(new POIEntry("minecraft:village_desert", Material.CACTUS, "<yellow>Desert Village</yellow>"));
        structureEntries.add(new POIEntry("minecraft:village_savanna", Material.ACACIA_SAPLING, "<yellow>Savanna Village</yellow>"));
        structureEntries.add(new POIEntry("minecraft:village_taiga", Material.SWEET_BERRIES, "<yellow>Taiga Village</yellow>"));
        structureEntries.add(new POIEntry("minecraft:village_snowy", Material.SNOWBALL, "<yellow>Snowy Village</yellow>"));
        structureEntries.add(new POIEntry("minecraft:mineshaft", Material.IRON_PICKAXE, "<gray>Abandoned Mineshaft</gray>"));
        structureEntries.add(new POIEntry("minecraft:mineshaft_mesa", Material.GOLDEN_PICKAXE, "<gold>Badlands Mineshaft</gold>"));
        structureEntries.add(new POIEntry("minecraft:swamp_hut", Material.BREWING_STAND, "<dark_green>Swamp Witch Hut</dark_green>"));
        structureEntries.add(new POIEntry("minecraft:igloo", Material.ICE, "<aqua>Ice Igloo</aqua>"));
        structureEntries.add(new POIEntry("minecraft:pillager_outpost", Material.CROSSBOW, "<red>Pillager Outpost</red>"));
        structureEntries.add(new POIEntry("minecraft:shipwreck", Material.OAK_BOAT, "<aqua>Ocean Shipwreck</aqua>"));
        structureEntries.add(new POIEntry("minecraft:shipwreck_beached", Material.CHEST_MINECART, "<gold>Beached Shipwreck</gold>"));
        structureEntries.add(new POIEntry("minecraft:ruined_portal", Material.CRYING_OBSIDIAN, "<dark_purple>Ruined Portal</dark_purple>"));
        structureEntries.add(new POIEntry("minecraft:buried_treasure", Material.HEART_OF_THE_SEA, "<light_purple>Buried Treasure</light_purple>"));
        structureEntries.add(new POIEntry("minecraft:ocean_ruin_cold", Material.PRISMARINE_CRYSTALS, "<blue>Cold Ocean Ruin</blue>"));
        structureEntries.add(new POIEntry("minecraft:ocean_ruin_warm", Material.BRAIN_CORAL, "<red>Warm Ocean Ruin</red>"));
        structureEntries.add(new POIEntry("minecraft:desert_pyramid", Material.CHISELED_SANDSTONE, "<gold>Desert Pyramid</gold>"));
        structureEntries.add(new POIEntry("minecraft:jungle_pyramid", Material.MOSSY_COBBLESTONE, "<green>Jungle Temple</green>"));
        structureEntries.add(new POIEntry("minecraft:trail_ruins", Material.BRUSH, "<yellow>Trail Ruins (Archaeology)</yellow>"));
        structureEntries.add(new POIEntry("minecraft:fortress", Material.NETHER_BRICK, "<red>Nether Fortress</red>"));
        structureEntries.add(new POIEntry("minecraft:monument", Material.PRISMARINE_BRICKS, "<aqua>Ocean Monument</aqua>"));
        structureEntries.add(new POIEntry("minecraft:mansion", Material.DARK_OAK_DOOR, "<dark_green>Woodland Mansion</dark_green>"));
        structureEntries.add(new POIEntry("minecraft:trial_chambers", Material.TRIAL_KEY, "<gold>Trial Chambers</gold>"));
        structureEntries.add(new POIEntry("minecraft:nether_fossil", Material.BONE_BLOCK, "<gray>Nether Fossil</gray>"));
        structureEntries.add(new POIEntry("minecraft:bastion_remnant", Material.GILDED_BLACKSTONE, "<gold>Bastion Remnant</gold>"));
        structureEntries.add(new POIEntry("minecraft:stronghold", Material.ENDER_EYE, "<dark_purple>Stronghold Keep</dark_purple>"));
        structureEntries.add(new POIEntry("minecraft:ancient_city", Material.ECHO_SHARD, "<dark_aqua>Ancient City Ruins</dark_aqua>"));
        structureEntries.add(new POIEntry("minecraft:end_city", Material.CHORUS_FRUIT, "<dark_purple>End City Spire</dark_purple>"));
    }

    public List<POIEntry> getStructureEntries() {
        return Collections.unmodifiableList(structureEntries);
    }

    public List<POIEntry> getBiomeEntries() {
        return Collections.unmodifiableList(biomeEntries);
    }

    public boolean isRegisteredStructure(String key) {
        for (POIEntry entry : structureEntries) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Entrypoint: Opens the custom double-chest GUI for the player (Main Selection Hub)
     */
    public void openGUI(Player player) {
        PlayerData data = explorationManager.getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to load profile.</red>"));
            return;
        }

        int lvl = data.getLevel();
        int exp = data.getExp();
        int reqExp = explorationManager.getRequiredExpForNextLevel(lvl);

        // Count unique completed biomes
        int biomesCount = data.getDiscoveredBiomes().size();
        
        // Count unique completed structures
        int structuresCount = 0;
        for (POIEntry entry : structureEntries) {
            if (plugin.getStructureDiscoveryManager().getDiscoveredStructures(player.getUniqueId(), entry.getKey()).size() > 0) {
                structuresCount++;
            }
        }

        Inventory inv = Bukkit.createInventory(null, 27, MiniMessage.miniMessage().deserialize(SELECTION_TITLE));

        // Fill borders with dark glass
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, getFillerItem(Material.BLACK_STAINED_GLASS_PANE));
        }
        for (int col = 0; col < 9; col++) {
            inv.setItem(col, getBorderItem());
            inv.setItem(18 + col, getBorderItem());
        }
        inv.setItem(9, getBorderItem());
        inv.setItem(17, getBorderItem());

        // Slot 10: Biomes Page Button
        ItemStack biomeBtn = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta biomeMeta = biomeBtn.getItemMeta();
        if (biomeMeta != null) {
            biomeMeta.displayName(MiniMessage.miniMessage().deserialize("<green><bold>Explore Biomes</bold></green>"));
            biomeMeta.lore(List.of(
                MiniMessage.miniMessage().deserialize("<gray>Discover and track 28 unique biomes.</gray>"),
                MiniMessage.miniMessage().deserialize("<gray>Earn massive EXP for each new biome visited!</gray>"),
                Component.text(""),
                MiniMessage.miniMessage().deserialize("<yellow>Click to open the Biomes Map!</yellow>")
            ));
            biomeBtn.setItemMeta(biomeMeta);
        }
        inv.setItem(10, biomeBtn);

        // Slot 13: Exploration Statistics Profile Card
        ItemStack statsCard = new ItemStack(Material.SPYGLASS);
        ItemMeta statsMeta = statsCard.getItemMeta();
        if (statsMeta != null) {
            statsMeta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>Your Exploration Profile</bold></gold>"));
            statsMeta.lore(List.of(
                MiniMessage.miniMessage().deserialize("<gray>Current Level: </gray><yellow><bold>" + lvl + "</bold></yellow>"),
                MiniMessage.miniMessage().deserialize("<gray>Experience Points: </gray><yellow>" + exp + " / " + reqExp + " EXP</yellow>"),
                MiniMessage.miniMessage().deserialize("<gray>Biomes Discovered: </gray><yellow>" + biomesCount + " / 28</yellow>"),
                MiniMessage.miniMessage().deserialize("<gray>Landmarks Discovered: </gray><yellow>" + structuresCount + " / 28</yellow>"),
                Component.text(""),
                MiniMessage.miniMessage().deserialize("<gray>Explore the physical world to gain mastery levels!</gray>")
            ));
            statsCard.setItemMeta(statsMeta);
        }
        inv.setItem(13, statsCard);

        // Slot 16: Landmarks Page Button
        ItemStack landmarkBtn = new ItemStack(Material.CHISELED_STONE_BRICKS);
        ItemMeta landmarkMeta = landmarkBtn.getItemMeta();
        if (landmarkMeta != null) {
            landmarkMeta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>Track Landmarks</bold></gold>"));
            landmarkMeta.lore(List.of(
                MiniMessage.miniMessage().deserialize("<gray>Locate major dungeons, keeps, and ruins.</gray>"),
                MiniMessage.miniMessage().deserialize("<gray>Requires progression levels to track!</gray>"),
                Component.text(""),
                MiniMessage.miniMessage().deserialize("<yellow>Click to open the Landmarks Map!</yellow>")
            ));
            landmarkBtn.setItemMeta(landmarkMeta);
        }
        inv.setItem(16, landmarkBtn);

        // Slot 22: Settings Button
        ItemStack settingsBtn = new ItemStack(Material.COMPASS);
        ItemMeta settingsMeta = settingsBtn.getItemMeta();
        if (settingsMeta != null) {
            settingsMeta.displayName(MiniMessage.miniMessage().deserialize("<aqua><bold>Navigation Settings</bold></aqua>"));
            settingsMeta.lore(List.of(
                MiniMessage.miniMessage().deserialize("<gray>Configure Skyrim actionbar compass,</gray>"),
                MiniMessage.miniMessage().deserialize("<gray>floating 3D pointer arrows, and trails.</gray>"),
                Component.text(""),
                MiniMessage.miniMessage().deserialize("<yellow>Click to open Navigation Settings!</yellow>")
            ));
            settingsBtn.setItemMeta(settingsMeta);
        }
        inv.setItem(22, settingsBtn);

        player.openInventory(inv);
        player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.0f);
    }

    /**
     * Opens the dedicated 54-slot Biomes Map Page (Pagination Redesign)
     */
    public void openBiomesGUI(Player player) {
        PlayerData data = explorationManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        int lvl = data.getLevel();
        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize(BIOMES_TITLE));

        // Fill borders
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, getFillerItem(Material.BLACK_STAINED_GLASS_PANE));
        }
        for (int col = 0; col < 9; col++) {
            inv.setItem(col, getBorderItem());
        }
        inv.setItem(9, getBorderItem());
        inv.setItem(17, getBorderItem());
        inv.setItem(18, getBorderItem());
        inv.setItem(26, getBorderItem());
        inv.setItem(27, getBorderItem());
        inv.setItem(35, getBorderItem());
        inv.setItem(36, getBorderItem());
        inv.setItem(44, getBorderItem());
        for (int col = 0; col < 9; col++) {
            inv.setItem(45 + col, getBorderItem());
        }

        // Return Button in bottom-middle (slot 49)
        inv.setItem(49, getBackItem());

        // Fill biomes (now locked and grouped by progression level locks!)
        int biomeIndex = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                if (biomeIndex < biomeEntries.size()) {
                    POIEntry entry = biomeEntries.get(biomeIndex++);
                    int reqLvl = configManager.getRequiredLevel(entry.getKey());
                    boolean unlocked = lvl >= reqLvl;
                    boolean completed = data.hasDiscoveredBiome(entry.getKey());

                    inv.setItem(row * 9 + col, getPOIItem(entry, unlocked, completed, reqLvl));
                }
            }
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.0f);
    }

    /**
     * Opens the dedicated 54-slot Landmarks Map Page
     */
    public void openLandmarksGUI(Player player) {
        PlayerData data = explorationManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        int lvl = data.getLevel();
        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize(LANDMARKS_TITLE));

        // Fill borders
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, getFillerItem(Material.BLACK_STAINED_GLASS_PANE));
        }
        for (int col = 0; col < 9; col++) {
            inv.setItem(col, getBorderItem());
        }
        inv.setItem(9, getBorderItem());
        inv.setItem(17, getBorderItem());
        inv.setItem(18, getBorderItem());
        inv.setItem(26, getBorderItem());
        inv.setItem(27, getBorderItem());
        inv.setItem(35, getBorderItem());
        inv.setItem(36, getBorderItem());
        inv.setItem(44, getBorderItem());
        for (int col = 0; col < 9; col++) {
            inv.setItem(45 + col, getBorderItem());
        }

        // Return Button in bottom-middle (slot 49)
        inv.setItem(49, getBackItem());

        // Filter out untracked structures
        List<POIEntry> activeStructures = new ArrayList<>();
        for (POIEntry entry : structureEntries) {
            if (configManager.isStructureTracked(entry.getKey())) {
                activeStructures.add(entry);
            }
        }

        // Fill structures
        int structIndex = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                if (structIndex < activeStructures.size()) {
                    POIEntry entry = activeStructures.get(structIndex++);
                    int reqLvl = configManager.getRequiredLevel(entry.getKey());
                    boolean unlocked = lvl >= reqLvl;
                    boolean completed = plugin.getStructureDiscoveryManager()
                        .getDiscoveredStructures(player.getUniqueId(), entry.getKey()).size() > 0;

                    inv.setItem(row * 9 + col, getPOIItem(entry, unlocked, completed, reqLvl));
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
            meta.displayName(MiniMessage.miniMessage().deserialize("<red><bold>◀ Return to Menu</bold></red>"));
            meta.lore(List.of(
                MiniMessage.miniMessage().deserialize("<gray>Click to return to the Selection Screen.</gray>")
            ));
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
     * Intercepts InventoryClick events inside these GUIs
     */
    public void handleGUIClick(Player player, String menuTitle, int slot, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Material type = clickedItem.getType();

        // 1. Clicks in Main Selection Hub
        if (menuTitle.contains("Cartography Map")) {
            if (type == Material.BLUE_STAINED_GLASS_PANE || type == Material.BLACK_STAINED_GLASS_PANE) {
                return;
            }
            if (slot == 10) { // Open Biomes Map
                openBiomesGUI(player);
            } else if (slot == 16) { // Open Landmarks Map
                openLandmarksGUI(player);
            } else if (slot == 22) { // Open Settings Menu
                plugin.getConfigGUI().openGUI(player);
            }
            return;
        }

        // 2. Clicks in sub-menus (Biomes Map / Landmarks Map)
        boolean isBiomesMap = menuTitle.contains("Biomes Map");
        boolean isLandmarksMap = menuTitle.contains("Landmarks Map");

        if (isBiomesMap || isLandmarksMap) {
            // Ignore borders/fillers
            if (type == Material.BLUE_STAINED_GLASS_PANE || type == Material.BLACK_STAINED_GLASS_PANE) {
                return;
            }

            // Back button
            if (type == Material.BARRIER) {
                openGUI(player);
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

            if (isBiomesMap) {
                for (POIEntry entry : biomeEntries) {
                    if (entry.getMaterial() == type) {
                        targetEntry = entry;
                        isBiome = true;
                        break;
                    }
                }
            } else {
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
}
