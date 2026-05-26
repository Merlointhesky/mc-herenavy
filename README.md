# HereNavy

A premium [Paper](https://papermc.io) Minecraft plugin for **RPG-style Exploration, Visual Navigation, and Skill Progression** — discover new biomes and structures to level up your Exploration skill, and unlock immersive, client-mod-free visual navigation HUD tools (including a Skyrim-style action/boss bar compass, a 3D TextDisplay pointing arrow, and glowing particle path trails)!

---

## Features

- **RPG Exploration Skill & Levels**:
  - Progress from **Level 1 to 100** by mapping out the Minecraft world.
  - **Passive Biome Discovery**: Awards Exploration EXP every time you step into a biome you've never visited before.
  - **Active Structure Discovery**: Triggers "Discovery" when you venture within 10 blocks of generated structure boundaries (Villages, Strongholds, Monuments, etc.). Awards huge EXP and broadcasts a server-wide announcement!
  - **Level-Based POI Unlock Progression**: Higher-tier biomes and structures require meeting minimum Exploration level milestones before they can be tracked.
  - **Level 100 Capstone Reward**: Grants players the unique **"World Explorer" Elytra** upon achieving mastery level.

- **Immersive Multi-Page Cartography GUI System**:
  - Open a sleek main landing selection hub using `/hn start` or `/explore`.
  - **Hub Features**:
    - Displays complete live player stats (Level, EXP progression, Biomes found, Landmarks found).
    - Links to dedicated visual navigation menus.
  - **Dedicated Biomes Map Page (`🌿 Biomes Map 🌿`)**: A symmetrical 54-slot chest containing all **28 registered biomes**, locked by exploration level locks.
  - **Dedicated Landmarks Map Page (`🏰 Landmarks Map 🏰`)**: A symmetrical 54-slot chest displaying all **28 registered vanilla structures** sorted in progression order.
  - **Interactive States**:
    - **Locked**: Represented by gray stained glass panes indicating the required Level to unlock.
    - **Unlocked / Unvisited**: Represented by unique thematic items (e.g., *Trial Key* for Trial Chambers, *Echo Shard* for Ancient Cities). Click to start tracking!
    - **Completed / Discovered**: Renders a glowing enchanted item showing that you've discovered it.

- **Visual Navigation Suite (Client-Mod-Free)**:
  - **Skyrim-Style Compass HUD**: Computes real-time angular headings towards your destination and renders a smooth scrolling directional compass on your **Action Bar** or **Boss Bar** (e.g. `W - NW - N[♦] - NE - E`).
  - **3D Unicode TomTom Arrow**: Spawns an invisible Paper `TextDisplay` entity containing a bold direction arrow (➔) exactly 3-4 blocks in front of the player. It rotates dynamically in real-time as you spin and move.
  - **Shiny Particle Trails**: Spawns a stream of guiding particles (cyan/green magic sparkles) that fly forward in the physical direction of your targeted landmark.
  - **Four Configurable Visual Styles**: Choose between Compass-Only, Arrow-Only, Trail-Only, or Combined displays via configuration.

- **Advanced Structure-Centric Database**:
  - Organizes world structures under `plugins/HereNavy/structures/[structure-type]/[uuid].yml`.
  - Records the exact physical location of each structure and holds a **shared list of players** who have successfully discovered it, preventing duplicate structure generation configurations in multiplayer environments.
  - **Origin Shifting Target Search**: Uses Bukkit's structure location API. If the nearest search coordinate matches a structure already discovered by the player, it mathematically shifts the search origin (e.g. +1000 blocks) and searches again, ensuring navigation always guides you to *new*, unexplored territory!

- **Smart Deep Configuration Merging**:
  - Modeled after premium integration standards, `ConfigManager` automatically compares your local server `config.yml` with default jar resources on startup.
  - Dynamically merges and appends new structures, biomes, or level unlock categories into existing config sections without altering or resetting any of your custom parameters.

---

## Commands

All commands can be run with `/hn` or `/explore` instead of `/herenavy`.

| Command | Description | Permission |
|---------|-------------|------------|
| `/hn start` (or `/explore`) | Opens the main Selection Cartography Hub | `herenavy.use` |
| `/hn stop` | Cancels any active navigation target | `herenavy.use` |
| `/hn info` | Displays your current Exploration Level, EXP, and progression status | `herenavy.use` |
| `/hn go <x> <y> <z>` | Starts manual coordinates navigation (Arrival-only, no EXP rewards) | `herenavy.use` |

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `herenavy.use` | Allows full access to all /hn commands and navigation menus | `true` |

---

## Building from Source

Package the plugin using Gradle:
```bash
./gradlew build
```
The compiled plugin JAR will be saved in `build/libs/`.

---

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
