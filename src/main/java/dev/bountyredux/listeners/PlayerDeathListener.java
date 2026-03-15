package dev.bountyredux.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import dev.bountyredux.BountiesPlugin;

/**
 * Listens for player deaths to award bounties to the killer.
 */
public class PlayerDeathListener implements Listener {

    private final BountiesPlugin plugin;

    public PlayerDeathListener(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;
        if (killer.equals(victim)) return;

        if (!plugin.getBountyManager().hasBounty(victim.getUniqueId())) return;

        double total = plugin.getBountyManager().clearBounties(victim.getUniqueId());
        if (total <= 0) return;

        // Pay the killer
        plugin.getVaultHook().deposit(killer, total);

        // Broadcast
        plugin.getServer().broadcastMessage(
                plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                        + "§6" + killer.getName()
                        + " §acollected the bounty of §6$" + String.format("%.2f", total)
                        + " §aon §e" + victim.getName() + "§a!");
    }
}
