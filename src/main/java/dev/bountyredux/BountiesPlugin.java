package dev.bountyredux;

import dev.bountyredux.commands.BountyCommand;
import dev.bountyredux.database.DatabaseManager;
import dev.bountyredux.listeners.PlayerDeathListener;
import dev.bountyredux.listeners.PlayerJoinListener;
import dev.bountyredux.managers.BountyManager;
import dev.bountyredux.managers.CooldownManager;
import dev.bountyredux.gui.BountyConfirmGUI;
import dev.bountyredux.gui.BountyMainGUI;
import dev.bountyredux.gui.TrackConfirmGUI;
import dev.bountyredux.listeners.GUIListener;
import dev.bountyredux.managers.TrackingManager;
import dev.bountyredux.vault.VaultHook;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public class BountiesPlugin extends JavaPlugin {

    private static BountiesPlugin instance;

    private VaultHook vaultHook;
    private DatabaseManager databaseManager;
    private BountyManager bountyManager;
    private CooldownManager cooldownManager;
    private TrackingManager trackingManager;
    private BountyMainGUI mainGUI;
    private BountyConfirmGUI confirmGUI;
    private TrackConfirmGUI trackConfirmGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Vault
        vaultHook = new VaultHook(this);
        if (!vaultHook.setup()) {
            getLogger().severe("Vault not found! Disabling Bounty Redux.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Database
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();
        if (!databaseManager.isConnected()) {
            getLogger().severe("SQLite failed to connect! Disabling Bounty Redux.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Managers
        bountyManager   = new BountyManager(this, databaseManager);
        cooldownManager = new CooldownManager(this);
        trackingManager = new TrackingManager(this);

        // GUIs — must be after managers
        mainGUI        = new BountyMainGUI(this);
        confirmGUI     = new BountyConfirmGUI(this);
        trackConfirmGUI = new TrackConfirmGUI(this);

        // Listeners — once, after everything is init'd
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(
                new GUIListener(this, mainGUI, confirmGUI, trackConfirmGUI), this);

        // Command — single instance for both executor and tab completer
        BountyCommand bountyCommand = new BountyCommand(this, mainGUI, confirmGUI, trackConfirmGUI);
        Objects.requireNonNull(getCommand("bounty")).setExecutor(bountyCommand);
        Objects.requireNonNull(getCommand("bounty")).setTabCompleter(bountyCommand);

        getLogger().info("Bounty Redux enabled!");
    }

    @Override
    public void onDisable() {
        if (trackingManager != null) trackingManager.cleanup();
        if (databaseManager != null) databaseManager.disconnect();
        getLogger().info("Bounty Redux disabled.");
    }

    public static BountiesPlugin getInstance() { return instance; }
    public VaultHook getVaultHook() { return vaultHook; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public BountyManager getBountyManager() { return bountyManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public TrackingManager getTrackingManager() { return trackingManager; }

    public String getMessage(String key) {
        String prefix = getConfig().getString("messages.prefix", "[Bounty Redux] ");
        String msg    = getConfig().getString("messages." + key, "§cMissing message: " + key);
        return prefix + msg;
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }
}