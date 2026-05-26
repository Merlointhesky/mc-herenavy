package com.herenavy.herenavy;

import com.herenavy.herenavy.command.HereNavyCommand;
import com.herenavy.herenavy.config.ConfigManager;
import com.herenavy.herenavy.gui.ExplorerGUI;
import com.herenavy.herenavy.listener.PlayerListener;
import com.herenavy.herenavy.navigation.ArrowManager;
import com.herenavy.herenavy.navigation.NavigationManager;
import com.herenavy.herenavy.progression.ExplorationManager;
import com.herenavy.herenavy.progression.StructureDiscoveryManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class HereNavyPlugin extends JavaPlugin {

    private static HereNavyPlugin instance;

    private ConfigManager configManager;
    private StructureDiscoveryManager structureDiscoveryManager;
    private ExplorationManager explorationManager;
    private NavigationManager navigationManager;
    private ArrowManager arrowManager;
    private ExplorerGUI explorerGUI;
    private com.herenavy.herenavy.gui.ConfigGUI configGUI;

    @Override
    public void onEnable() {
        instance = this;

        // Load Configuration
        this.configManager = new ConfigManager(this);

        // Load Managers
        this.structureDiscoveryManager = new StructureDiscoveryManager(this);
        this.explorationManager = new ExplorationManager(this, structureDiscoveryManager);
        this.arrowManager = new ArrowManager(this);
        this.navigationManager = new NavigationManager(this, structureDiscoveryManager, arrowManager);
        this.explorerGUI = new ExplorerGUI(this, configManager, explorationManager, navigationManager);
        this.configGUI = new com.herenavy.herenavy.gui.ConfigGUI(this, explorationManager);

        // Startup Progression loops (e.g. 20-tick discovery tasks)
        explorationManager.startTasks();
        navigationManager.startTasks();

        // Register Command
        HereNavyCommand commandExecutor = new HereNavyCommand(this, configManager, explorationManager, navigationManager, explorerGUI, configGUI);
        getCommand("herenavy").setExecutor(commandExecutor);
        getCommand("herenavy").setTabCompleter(commandExecutor);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, explorationManager, navigationManager, explorerGUI, configGUI), this);

        getLogger().info("HereNavy " + getDescription().getVersion() + " has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        // Stop active tasks
        if (explorationManager != null) {
            explorationManager.stopTasks();
        }
        if (navigationManager != null) {
            navigationManager.stopTasks();
            navigationManager.cleanupAllNavigations();
        }

        getLogger().info("HereNavy disabled!");
    }

    public static HereNavyPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StructureDiscoveryManager getStructureDiscoveryManager() {
        return structureDiscoveryManager;
    }

    public ExplorationManager getExplorationManager() {
        return explorationManager;
    }

    public NavigationManager getNavigationManager() {
        return navigationManager;
    }

    public ArrowManager getArrowManager() {
        return arrowManager;
    }

    public ExplorerGUI getExplorerGUI() {
        return explorerGUI;
    }

    public com.herenavy.herenavy.gui.ConfigGUI getConfigGUI() {
        return configGUI;
    }
}
