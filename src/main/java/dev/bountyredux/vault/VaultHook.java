package dev.bountyredux.vault;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

import dev.bountyredux.BountiesPlugin;

/**
 * Hooks into Vault to provide economy functionality.
 */
public class VaultHook {

    private final BountiesPlugin plugin;
    private Economy economy;

    public VaultHook(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean has(org.bukkit.entity.Player player, double amount) {
        return economy.has(player, amount);
    }

    public void withdraw(org.bukkit.entity.Player player, double amount) {
        economy.withdrawPlayer(player, amount);
    }

    public void deposit(org.bukkit.entity.Player player, double amount) {
        economy.depositPlayer(player, amount);
    }

    public Economy getEconomy() {
        return economy;
    }
}
