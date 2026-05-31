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
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class ExplorerGUI {

    private final HereNavyPlugin plugin;
    private final ConfigManager configManager;
    private final ExplorationManager explorationManager;
    private final NavigationManager navigationManager;
    private final NamespacedKey poiKey;

    public static final String SELECTION_TITLE = "<blue><bold>🧭 Cartography Map 🧭</bold></blue>";
    public static final String BIOMES_TITLE = "<green><bold>🌿 Biomes Map 🌿</bold></green>";
    public static final String LANDMARKS_TITLE = "<gold><bold>🏰 Landmarks Map 🏰</bold></gold>";

    // Biome entries mapping
    private final List<POIEntry> biomeEntries = new ArrayList<>();
    // Structure entries mapping
    private final List<POIEntry> structureEntries = new ArrayList<>();

    // Player UI Session tracking
    private final Map<UUID, GUISession> activeSessions = new HashMap<>();

    public static final class GUISession {
        private String category; // "OVERWORLD", "NETHER", "THE_END"
        private int page;        // 1-indexed

        public GUISession(String category, int page) {
            this.category = category;
            this.page = page;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
    }

    public static final class POIEntry {
        private final String key;
        private final Material material;
        private final String displayName;
        private final String dimension; // "OVERWORLD", "NETHER", "THE_END"

        public POIEntry(String key, Material material, String displayName, String dimension) {
            this.key = key;
            this.material = material;
            this.displayName = displayName;
            this.dimension = dimension;
        }

        public String getKey() { return key; }
        public Material getMaterial() { return material; }
        public String getDisplayName() { return displayName; }
        public String getDimension() { return dimension; }
    }

    public ExplorerGUI(HereNavyPlugin plugin, ConfigManager configManager, ExplorationManager explorationManager, NavigationManager navigationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.explorationManager = explorationManager;
        this.navigationManager = navigationManager;
        this.poiKey = new NamespacedKey(plugin, "poi_key");
        setupPOIEntries();
    }

    private void setupPOIEntries() {
        // -- 64 Biomes --
        // OVERWORLD (54)
        biomeEntries.add(new POIEntry("minecraft:plains", Material.GRASS_BLOCK, "<green>Plains Biome</green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:sunflower_plains", Material.SUNFLOWER, "<green>Sunflower Plains</green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:snowy_plains", Material.SNOW_BLOCK, "<white>Snowy Plains</white>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:ice_spikes", Material.PACKED_ICE, "<aqua>Ice Spikes</aqua>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:forest", Material.OAK_SAPLING, "<green>Forest Biome</green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:flower_forest", Material.ROSE_BUSH, "<light_purple>Flower Forest</light_purple>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:birch_forest", Material.BIRCH_SAPLING, "<green>Birch Forest</green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:old_growth_birch_forest", Material.BIRCH_LOG, "<dark_green>Old Growth Birch Forest</dark_green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:dark_forest", Material.DARK_OAK_LOG, "<dark_green>Dark Forest</dark_green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:cherry_grove", Material.CHERRY_SAPLING, "<light_purple>Cherry Grove</light_purple>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:pale_garden", Material.MOSS_BLOCK, "<gray>Pale Garden</gray>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:taiga", Material.SPRUCE_SAPLING, "<green>Taiga Biome</green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:snowy_taiga", Material.SNOWBALL, "<white>Snowy Taiga</white>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:old_growth_pine_taiga", Material.SPRUCE_LOG, "<dark_green>Old Growth Pine Taiga</dark_green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:old_growth_spruce_taiga", Material.SPRUCE_WOOD, "<dark_green>Old Growth Spruce Taiga</dark_green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:grove", Material.SNOW_BLOCK, "<white>Snowy Grove</white>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:snowy_slopes", Material.SNOW, "<white>Snowy Slopes</white>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:windswept_hills", Material.COBBLESTONE, "<gray>Windswept Hills</gray>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:windswept_forest", Material.OAK_LOG, "<gray>Windswept Forest</gray>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:windswept_gravelly_hills", Material.GRAVEL, "<gray>Windswept Gravelly Hills</gray>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:meadow", Material.SUNFLOWER, "<yellow>Meadow Biome</yellow>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:frozen_peaks", Material.BLUE_ICE, "<aqua>Frozen Peaks</aqua>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:jagged_peaks", Material.PACKED_ICE, "<aqua>Jagged Peaks</aqua>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:stony_peaks", Material.STONE, "<gray>Stony Peaks</gray>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:desert", Material.SAND, "<yellow>Desert Biome</yellow>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:savanna", Material.ACACIA_LOG, "<gold>Savanna Biome</gold>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:savanna_plateau", Material.ACACIA_WOOD, "<gold>Savanna Plateau</gold>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:windswept_savanna", Material.ACACIA_LEAVES, "<gold>Windswept Savanna</gold>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:badlands", Material.TERRACOTTA, "<red>Badlands Biome</red>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:wooded_badlands", Material.COARSE_DIRT, "<red>Wooded Badlands</red>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:eroded_badlands", Material.RED_SAND, "<red>Eroded Badlands</red>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:swamp", Material.LILY_PAD, "<dark_aqua>Swamp Biome</dark_aqua>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:mangrove_swamp", Material.MANGROVE_PROPAGULE, "<dark_red>Mangrove Swamp</dark_red>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:river", Material.WATER_BUCKET, "<blue>River</blue>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:frozen_river", Material.ICE, "<aqua>Frozen River</aqua>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:beach", Material.SAND, "<yellow>Sandy Beach</yellow>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:snowy_beach", Material.SNOW, "<white>Snowy Beach</white>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:stony_shore", Material.STONE, "<gray>Stony Shore</gray>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:jungle", Material.JUNGLE_LEAVES, "<green>Jungle Biome</green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:sparse_jungle", Material.JUNGLE_SAPLING, "<green>Sparse Jungle</green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:bamboo_jungle", Material.BAMBOO, "<green>Bamboo Jungle</green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:ocean", Material.WATER_BUCKET, "<blue>Ocean Biome</blue>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:deep_ocean", Material.PRISMARINE, "<blue>Deep Ocean</blue>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:warm_ocean", Material.BRAIN_CORAL, "<red>Warm Ocean</red>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:lukewarm_ocean", Material.KELP, "<aqua>Lukewarm Ocean</aqua>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:deep_lukewarm_ocean", Material.SEAGRASS, "<aqua>Deep Lukewarm Ocean</aqua>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:cold_ocean", Material.PRISMARINE_SHARD, "<blue>Cold Ocean</blue>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:deep_cold_ocean", Material.PRISMARINE_BRICKS, "<blue>Deep Cold Ocean</blue>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:frozen_ocean", Material.ICE, "<aqua>Frozen Ocean</aqua>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:deep_frozen_ocean", Material.PACKED_ICE, "<aqua>Deep Frozen Ocean</aqua>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:mushroom_fields", Material.RED_MUSHROOM_BLOCK, "<light_purple>Mushroom Fields</light_purple>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:lush_caves", Material.GLOW_BERRIES, "<green>Lush Caves</green>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:dripstone_caves", Material.POINTED_DRIPSTONE, "<gray>Dripstone Caves</gray>", "OVERWORLD"));
        biomeEntries.add(new POIEntry("minecraft:deep_dark", Material.SCULK, "<dark_blue>Deep Dark</dark_blue>", "OVERWORLD"));

        // NETHER (5)
        biomeEntries.add(new POIEntry("minecraft:nether_wastes", Material.NETHERRACK, "<dark_red>Nether Wastes</dark_red>", "NETHER"));
        biomeEntries.add(new POIEntry("minecraft:soul_sand_valley", Material.SOUL_SAND, "<brown>Soul Sand Valley</brown>", "NETHER"));
        biomeEntries.add(new POIEntry("minecraft:crimson_forest", Material.CRIMSON_FUNGUS, "<red>Crimson Forest</red>", "NETHER"));
        biomeEntries.add(new POIEntry("minecraft:warped_forest", Material.WARPED_FUNGUS, "<cyan>Warped Forest</cyan>", "NETHER"));
        biomeEntries.add(new POIEntry("minecraft:basalt_deltas", Material.BASALT, "<gray>Basalt Deltas</gray>", "NETHER"));

        // THE END (5)
        biomeEntries.add(new POIEntry("minecraft:the_end", Material.END_STONE, "<yellow>The End</yellow>", "THE_END"));
        biomeEntries.add(new POIEntry("minecraft:small_end_islands", Material.CHORUS_FLOWER, "<yellow>Small End Islands</yellow>", "THE_END"));
        biomeEntries.add(new POIEntry("minecraft:end_midlands", Material.CHORUS_FRUIT, "<yellow>End Midlands</yellow>", "THE_END"));
        biomeEntries.add(new POIEntry("minecraft:end_highlands", Material.CHORUS_PLANT, "<yellow>End Highlands</yellow>", "THE_END"));
        biomeEntries.add(new POIEntry("minecraft:end_barrens", Material.END_STONE_BRICKS, "<yellow>End Barrens</yellow>", "THE_END"));

        // -- 29 Structures --
        // OVERWORLD (24)
        structureEntries.add(new POIEntry("minecraft:village_plains", Material.WHEAT, "<yellow>Plains Village</yellow>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:village_desert", Material.CACTUS, "<yellow>Desert Village</yellow>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:village_savanna", Material.ACACIA_SAPLING, "<yellow>Savanna Village</yellow>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:village_taiga", Material.SWEET_BERRIES, "<yellow>Taiga Village</yellow>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:village_snowy", Material.SNOWBALL, "<yellow>Snowy Village</yellow>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:mineshaft", Material.IRON_PICKAXE, "<gray>Abandoned Mineshaft</gray>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:mineshaft_mesa", Material.GOLDEN_PICKAXE, "<gold>Badlands Mineshaft</gold>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:swamp_hut", Material.BREWING_STAND, "<dark_green>Swamp Witch Hut</dark_green>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:igloo", Material.ICE, "<aqua>Ice Igloo</aqua>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:pillager_outpost", Material.CROSSBOW, "<red>Pillager Outpost</red>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:shipwreck", Material.OAK_BOAT, "<aqua>Ocean Shipwreck</aqua>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:shipwreck_beached", Material.CHEST_MINECART, "<gold>Beached Shipwreck</gold>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:ruined_portal", Material.CRYING_OBSIDIAN, "<dark_purple>Ruined Portal</dark_purple>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:buried_treasure", Material.HEART_OF_THE_SEA, "<light_purple>Buried Treasure</light_purple>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:ocean_ruin_cold", Material.PRISMARINE_CRYSTALS, "<blue>Cold Ocean Ruin</blue>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:ocean_ruin_warm", Material.BRAIN_CORAL, "<red>Warm Ocean Ruin</red>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:desert_pyramid", Material.CHISELED_SANDSTONE, "<gold>Desert Pyramid</gold>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:jungle_pyramid", Material.MOSSY_COBBLESTONE, "<green>Jungle Temple</green>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:trail_ruins", Material.BRUSH, "<yellow>Trail Ruins</yellow>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:monument", Material.PRISMARINE_BRICKS, "<aqua>Ocean Monument</aqua>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:mansion", Material.DARK_OAK_DOOR, "<dark_green>Woodland Mansion</dark_green>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:trial_chambers", Material.TRIAL_KEY, "<gold>Trial Chambers</gold>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:ancient_city", Material.ECHO_SHARD, "<dark_aqua>Ancient City Ruins</dark_aqua>", "OVERWORLD"));
        structureEntries.add(new POIEntry("minecraft:stronghold", Material.ENDER_EYE, "<dark_purple>Stronghold Keep</dark_purple>", "OVERWORLD"));

        // NETHER (4)
        structureEntries.add(new POIEntry("minecraft:fortress", Material.NETHER_BRICK, "<red>Nether Fortress</red>", "NETHER"));
        structureEntries.add(new POIEntry("minecraft:bastion_remnant", Material.GILDED_BLACKSTONE, "<gold>Bastion Remnant</gold>", "NETHER"));
        structureEntries.add(new POIEntry("minecraft:nether_fossil", Material.BONE_BLOCK, "<gray>Nether Fossil</gray>", "NETHER"));
        structureEntries.add(new POIEntry("minecraft:ruined_portal_nether", Material.CRYING_OBSIDIAN, "<dark_purple>Nether Ruined Portal</dark_purple>", "NETHER"));

        // THE END (1)
        structureEntries.add(new POIEntry("minecraft:end_city", Material.CHORUS_FRUIT, "<dark_purple>End City Spire</dark_purple>", "THE_END"));
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

    private GUISession getOrCreateSession(UUID uuid) {
        return activeSessions.computeIfAbsent(uuid, k -> new GUISession("OVERWORLD", 1));
    }

    public void removeSession(UUID uuid) {
        activeSessions.remove(uuid);
    }

    /**
     * Entrypoint: Opens the custom Selection Cartography Hub
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

        // Count unique completed biomes dynamically
        int biomesCount = 0;
        for (POIEntry entry : biomeEntries) {
            if (data.hasDiscoveredBiome(entry.getKey())) {
                biomesCount++;
            }
        }
        
        // Count unique completed structures dynamically
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
                MiniMessage.miniMessage().deserialize("<gray>Discover and track 64 unique biomes.</gray>"),
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
                MiniMessage.miniMessage().deserialize("<gray>Biomes Discovered: </gray><yellow>" + biomesCount + " / 64</yellow>"),
                MiniMessage.miniMessage().deserialize("<gray>Landmarks Discovered: </gray><yellow>" + structuresCount + " / 29</yellow>"),
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
     * Opens the dedicated 54-slot Biomes Map Page (Tabbed & Paginated Layout)
     */
    public void openBiomesGUI(Player player) {
        PlayerData data = explorationManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        GUISession session = getOrCreateSession(player.getUniqueId());
        String cat = session.getCategory();
        int page = session.getPage();
        int lvl = data.getLevel();

        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize(BIOMES_TITLE));

        // Fill background with black glass panes
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, getFillerItem(Material.BLACK_STAINED_GLASS_PANE));
        }
        // Blue border layout
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
        inv.setItem(2, getTabItem(Material.GRASS_BLOCK, "<green><bold>Overworld Biomes</bold></green>", cat.equals("OVERWORLD")));
        inv.setItem(4, getTabItem(Material.NETHERRACK, "<red><bold>Nether Biomes</bold></red>", cat.equals("NETHER")));
        inv.setItem(6, getTabItem(Material.END_STONE, "<yellow><bold>The End Biomes</bold></yellow>", cat.equals("THE_END")));

        // Filter biomes by dimension category
        List<POIEntry> activeBiomes = new ArrayList<>();
        for (POIEntry entry : biomeEntries) {
            if (entry.getDimension().equals(cat)) {
                activeBiomes.add(entry);
            }
        }

        // Calculate pagination (28 slots per page)
        int totalItems = activeBiomes.size();
        int itemsPerPage = 28;
        int maxPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (maxPages == 0) maxPages = 1;

        // Render previous/next arrow icons
        if (page > 1) {
            inv.setItem(45, getPageArrowItem(true, page - 1));
        }
        if (page < maxPages) {
            inv.setItem(53, getPageArrowItem(false, page + 1));
        }

        // Return Button in bottom-middle (slot 49)
        inv.setItem(49, getBackItem());

        // Fill grid (Row 1-4, column 1-7 = 28 slots per page)
        int startIndex = (page - 1) * itemsPerPage;
        int gridIndex = 0;

        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                int itemIndex = startIndex + gridIndex;
                if (itemIndex < totalItems) {
                    POIEntry entry = activeBiomes.get(itemIndex);
                    int reqLvl = configManager.getRequiredLevel(entry.getKey());
                    boolean unlocked = lvl >= reqLvl;
                    boolean completed = data.hasDiscoveredBiome(entry.getKey());

                    inv.setItem(row * 9 + col, getPOIItem(entry, unlocked, completed, reqLvl));
                }
                gridIndex++;
            }
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.0f);
    }

    /**
     * Opens the dedicated 54-slot Landmarks Map Page (Tabbed & Paginated Layout)
     */
    public void openLandmarksGUI(Player player) {
        PlayerData data = explorationManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        GUISession session = getOrCreateSession(player.getUniqueId());
        String cat = session.getCategory();
        int page = session.getPage();
        int lvl = data.getLevel();

        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize(LANDMARKS_TITLE));

        // Fill background with black glass panes
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, getFillerItem(Material.BLACK_STAINED_GLASS_PANE));
        }
        // Blue border layout
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
        inv.setItem(2, getTabItem(Material.GRASS_BLOCK, "<green><bold>Overworld Landmarks</bold></green>", cat.equals("OVERWORLD")));
        inv.setItem(4, getTabItem(Material.NETHERRACK, "<red><bold>Nether Landmarks</bold></red>", cat.equals("NETHER")));
        inv.setItem(6, getTabItem(Material.END_STONE, "<yellow><bold>The End Landmarks</bold></yellow>", cat.equals("THE_END")));

        // Filter and compile active structures
        List<POIEntry> activeStructures = new ArrayList<>();
        for (POIEntry entry : structureEntries) {
            if (entry.getDimension().equals(cat) && configManager.isStructureTracked(entry.getKey())) {
                activeStructures.add(entry);
            }
        }

        // Calculate pagination (28 slots per page)
        int totalItems = activeStructures.size();
        int itemsPerPage = 28;
        int maxPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (maxPages == 0) maxPages = 1;

        // Render previous/next arrow icons
        if (page > 1) {
            inv.setItem(45, getPageArrowItem(true, page - 1));
        }
        if (page < maxPages) {
            inv.setItem(53, getPageArrowItem(false, page + 1));
        }

        // Return Button in bottom-middle (slot 49)
        inv.setItem(49, getBackItem());

        // Fill grid (Row 1-4, column 1-7 = 28 slots per page)
        int startIndex = (page - 1) * itemsPerPage;
        int gridIndex = 0;

        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                int itemIndex = startIndex + gridIndex;
                if (itemIndex < totalItems) {
                    POIEntry entry = activeStructures.get(itemIndex);
                    int reqLvl = configManager.getRequiredLevel(entry.getKey());
                    boolean unlocked = lvl >= reqLvl;
                    boolean completed = plugin.getStructureDiscoveryManager()
                        .getDiscoveredStructures(player.getUniqueId(), entry.getKey()).size() > 0;

                    inv.setItem(row * 9 + col, getPOIItem(entry, unlocked, completed, reqLvl));
                }
                gridIndex++;
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

    private ItemStack getPageArrowItem(boolean isPrevious, int targetPage) {
        ItemStack item = new ItemStack(isPrevious ? Material.FEATHER : Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String title = isPrevious ? "<yellow><bold>◀ Previous Page</bold></yellow>" : "<yellow><bold>Next Page ▶</bold></yellow>";
            meta.displayName(MiniMessage.miniMessage().deserialize(title));
            meta.lore(List.of(
                MiniMessage.miniMessage().deserialize("<gray>Go to Page " + targetPage + "</gray>")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Builds standard clickable POI items with visual locks/enchants and persistent tags
     */
    private ItemStack getPOIItem(POIEntry entry, boolean unlocked, boolean completed, int reqLevel) {
        if (!unlocked) {
            ItemStack lockedItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = lockedItem.getItemMeta();
            if (meta != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize("<red><bold>LOCKED POI</bold></red>"));
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
            
            // Embed NamespacedKey tag dynamically using PersistentDataContainer
            meta.getPersistentDataContainer().set(poiKey, org.bukkit.persistence.PersistentDataType.STRING, entry.getKey());
            
            List<Component> lore = new ArrayList<>();
            if (completed) {
                lore.add(MiniMessage.miniMessage().deserialize("<green>✔ DISCOVERED MASTERY</green>"));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Click to track coordinates again</gray>"));
                // Add glowing enchantment
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                lore.add(MiniMessage.miniMessage().deserialize("<yellow>⏰ UNVISITED POI</yellow>"));
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Click to start visual navigation!</gray>"));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public NamespacedKey getPoiKey() {
        return poiKey;
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
                getOrCreateSession(player.getUniqueId()).setCategory("OVERWORLD");
                getOrCreateSession(player.getUniqueId()).setPage(1);
                openBiomesGUI(player);
            } else if (slot == 16) { // Open Landmarks Map
                getOrCreateSession(player.getUniqueId()).setCategory("OVERWORLD");
                getOrCreateSession(player.getUniqueId()).setPage(1);
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
                removeSession(player.getUniqueId()); // Clear page session
                openGUI(player);
                return;
            }

            GUISession session = getOrCreateSession(player.getUniqueId());

            // Handle Tab Clicks (slots 2, 4, 6 in Row 0)
            if (slot == 2) {
                session.setCategory("OVERWORLD");
                session.setPage(1);
                player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.2f);
                if (isBiomesMap) openBiomesGUI(player); else openLandmarksGUI(player);
                return;
            }
            if (slot == 4) {
                session.setCategory("NETHER");
                session.setPage(1);
                player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.2f);
                if (isBiomesMap) openBiomesGUI(player); else openLandmarksGUI(player);
                return;
            }
            if (slot == 6) {
                session.setCategory("THE_END");
                session.setPage(1);
                player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.2f);
                if (isBiomesMap) openBiomesGUI(player); else openLandmarksGUI(player);
                return;
            }

            // Handle Pagination Arrow clicks
            if (slot == 45 && type == Material.FEATHER) {
                session.setPage(session.getPage() - 1);
                player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.0f);
                if (isBiomesMap) openBiomesGUI(player); else openLandmarksGUI(player);
                return;
            }
            if (slot == 53 && type == Material.ARROW) {
                session.setPage(session.getPage() + 1);
                player.playSound(player.getLocation(), "ui.button.click", 1.0f, 1.0f);
                if (isBiomesMap) openBiomesGUI(player); else openLandmarksGUI(player);
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

            // Retrieve hidden key via PersistentDataContainer
            ItemMeta clickedMeta = clickedItem.getItemMeta();
            if (clickedMeta == null) return;
            String targetKey = clickedMeta.getPersistentDataContainer().get(poiKey, org.bukkit.persistence.PersistentDataType.STRING);
            if (targetKey == null) return;

            POIEntry targetEntry = null;
            boolean isBiome = false;

            if (isBiomesMap) {
                for (POIEntry entry : biomeEntries) {
                    if (entry.getKey().equalsIgnoreCase(targetKey)) {
                        targetEntry = entry;
                        isBiome = true;
                        break;
                    }
                }
            } else {
                for (POIEntry entry : structureEntries) {
                    if (entry.getKey().equalsIgnoreCase(targetKey)) {
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
