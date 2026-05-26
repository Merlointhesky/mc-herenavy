# Plugin Specification: HereNavy (HN)
**Target API:** Paper 26.1.2
**Plugin Focus:** RPG-style Exploration, Visual Navigation, & Progression

## 1. Core Concept
HereNavy transforms the game into a rewarding, RPG-style exploration loop. Players level up an "Exploration" skill by discovering new biomes and structures. Progression unlocks the ability to track higher-tier POIs using immersive, client-mod-free visual navigation tools (Skyrim-style compass and a 3D TomTom arrow). 

## 2. Navigation & Visuals
Players can track a specific biome or structure. The plugin guides them using purely server-side techniques.

*   **The Skyrim Compass (UI):**
    *   Runs on a repeating task (~4 ticks).
    *   Calculates the yaw angle between the player and the target location.
    *   Generates a string (e.g., `NW - N[♦] - NE`) and displays it via the Action Bar or Boss Bar.
*   **The TomTom Arrow (World):**
    *   Uses 1.19.4+ `TextDisplay` entities for zero-lag rendering.
    *   Spawns an invisible entity 3-4 blocks in front of the player containing a large Unicode arrow (➔).
    *   Updates its transformation/rotation dynamically to always point directly toward the target location.

## 3. The RPG Progression System
Players earn Exploration EXP and level up from 1 to 100. Levels dictate which structures and biomes they are permitted to track.

*   **Earning EXP:**
    *   **Passive Biomes:** A 1-second (20-tick) repeating task checks `player.getLocation().getBlock().getBiome()`. If it's a new biome for that player, award EXP.
    *   **Active Discovery (Structures):** The same 20-tick task checks `world.getGeneratedStructures(chunk)` for the chunk the player is standing in. If the player's distance to the structure's `BoundingBox` is $\le 10$ blocks, trigger "Discovery", award massive EXP, and broadcast a chat prompt.
*   **Capstone Reward:**
    *   Reaching Level 100 grants a custom `ItemStack` Elytra with the display name "World Explorer".

## 4. The Explorer GUI (`/hn` or `/explore`)
A 54-slot double-chest inventory acting as the player's Cartography Map.

*   **Top Rows:** Biome tracking.
*   **Bottom Rows:** Structure tracking.
*   **Visual Item States:**
    *   **Locked:** Gray Stained Glass Pane or Barrier block (Lore: "Requires Exploration Level [X]").
    *   **Unlocked / Unvisited:** The representative item (e.g., Chiseled Stone Bricks for Stronghold). Clicking this sets the navigation target.
    *   **Discovered / Completed:** The representative item with the `Enchantment.GLOW` hidden flag to show mastery.

## 5. Blacklisting & Target Selection
To prevent the `world.locateNearestStructure()` method from pointing to a POI the player has already found:

*   **Player Blacklist:** A data file (YAML or SQLite) stores the X/Z chunk coordinates of every structure a player has discovered.
*   **Origin Shifting:** If a search returns a blacklisted coordinate, the plugin increments the search origin mathematically (e.g., adding +1000 blocks to X/Z) and searches again, ensuring the player is always guided to a *new* location.

## 6. Configuration Structure (`config.yml`)

```yaml
# ==========================================
#          HereNavy Configuration
# ==========================================

# -- Navigation Toggles --
navigation-visuals:
  skyrim-compass: true
  skyrim-compass-style: ACTIONBAR # Options: BOSSBAR, ACTIONBAR
  tomtom-arrow: true
  arrow-style: TEXT_DISPLAY # Options: TEXT_DISPLAY, PARTICLES

# -- Exploration Progression --
max-level: 100
exp-per-new-biome: 50
exp-per-new-structure: 500

# -- Level Unlocks (Examples) --
unlocks:
  level_1:
    - "minecraft:village_plains"
    - "minecraft:plains"
  level_10:
    - "minecraft:mineshaft"
    - "minecraft:dark_forest"
  level_80:
    - "minecraft:bastion_remnant"
  level_90:
    - "minecraft:stronghold"
  level_100:
    reward_item: ELYTRA
    reward_name: "<gold><bold>World Explorer</bold></gold>"

## 7. Manual Navigation
Add a command that allows to seet a coord x y z that will just lead the player to that location, without exp reward when arriving at that destination but just a message in chat saying that you arrived. 

## 8. Commands
/herenavy will have an alias /hn. 
/hn info will provide the player current level and experience. 
/hn start will show teh GUI to select which type of adventure (biome, structure etc.. ) and then show which available (all biomes will always be available, structures will depend on player exploration level).
/hn stop will stop the current navigation.
/hn go x y z will start a manual navigation to the coordinates x y z. When the player arrives at the location, it will send a message in chat saying that the player arrived.
