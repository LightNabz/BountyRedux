package dev.bountyredux;

import dev.bountyredux.commands.BountyCommand;
import dev.bountyredux.database.DatabaseManager;
import dev.bountyredux.listeners.PlayerDeathListener;
import dev.bountyredux.managers.BountyManager;
import dev.bountyredux.managers.CooldownManager;
import dev.bountyredux.utils.SkinRestorerHook;
import dev.bountyredux.vault.VaultHook;
import org.bukkit.plugin.java.JavaPlugin;

public class BountiesPlugin extends JavaPlugin {

    private static BountiesPlugin instance;

    private VaultHook vaultHook;
    private DatabaseManager databaseManager;
    private BountyManager bountyManager;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        vaultHook = new VaultHook(this);
        if (!vaultHook.setup()) {
            getLogger().severe("Vault not found! Disabling Bounties.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        SkinRestorerHook.setup();
        if (SkinRestorerHook.isAvailable()) {
            getLogger().info("SkinRestorer found — cracked player skins will render correctly.");
        } else {
            getLogger().info("SkinRestorer not found — only premium player skins will render.");
        }

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();
        if (!databaseManager.isConnected()) {
            getLogger().severe("SQLite failed to connect! Disabling Bounties.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        bountyManager = new BountyManager(this, databaseManager);
        cooldownManager = new CooldownManager(this);

        BountyCommand bountyCommand = new BountyCommand(this);
        getCommand("bounty").setExecutor(bountyCommand);
        getCommand("bounty").setTabCompleter(bountyCommand);

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);

        getLogger().info("Bounty Redux enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.disconnect();
        getLogger().info("Bounty Redux disabled.");
    }

    public static BountiesPlugin getInstance() { return instance; }
    public VaultHook getVaultHook() { return vaultHook; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public BountyManager getBountyManager() { return bountyManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }

    public String getMessage(String key) {
        String prefix = getConfig().getString("messages.prefix", "[Bounty Redux] ");
        String msg = getConfig().getString("messages." + key, "§cMissing message: " + key);
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