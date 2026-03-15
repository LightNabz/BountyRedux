package dev.bountyredux.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import dev.bountyredux.BountiesPlugin;
import dev.bountyredux.gui.BountyConfirmGUI;
import dev.bountyredux.managers.BountyManager;
import dev.bountyredux.managers.CooldownManager;
import dev.bountyredux.vault.VaultHook;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all GUI interactions for the bounty menus.
 */
public class GUIListener implements Listener {

    private final BountiesPlugin plugin;

    // Stores pending bounty confirmations: placerUUID -> {targetName, amount}
    public static final Map<UUID, Object[]> pendingConfirmations = new HashMap<>();

    public GUIListener(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = event.getView().getTitle();

        // Main bounty menu
        if (title.equals(plugin.getConfig().getString("settings.gui-title", "§6§lBounty Menu"))) {
            event.setCancelled(true);
            handleMainGUIClick(player, event.getSlot());
            return;
        }

        // Confirm GUI
        if (title.startsWith(BountyConfirmGUI.CONFIRM_TITLE_PREFIX)) {
            event.setCancelled(true);
            handleConfirmGUIClick(player, event.getSlot());
        }
    }

    private void handleMainGUIClick(Player player, int slot) {
        switch (slot) {
            case 20 -> player.sendMessage(plugin.getMessage("use-chat-for-add",
                    // fallback inline message
                    "{msg}", "§7Type §e/bounty add <player> <amount> §7to place a bounty."));
            case 22 -> player.sendMessage("§7Type §e/bounty search <player> §7to search bounties.");
            default -> { /* top bounties slots — no action yet */ }
        }
    }

    private void handleConfirmGUIClick(Player player, int slot) {
        Object[] pending = pendingConfirmations.get(player.getUniqueId());
        if (pending == null) {
            player.closeInventory();
            return;
        }

        String targetName = (String) pending[0];
        double amount = (double) pending[1];

        if (slot == 11) {
            // CONFIRM
            pendingConfirmations.remove(player.getUniqueId());
            player.closeInventory();

            VaultHook vault = plugin.getVaultHook();
            BountyManager bm = plugin.getBountyManager();
            CooldownManager cm = plugin.getCooldownManager();

            if (!vault.has(player, amount)) {
                player.sendMessage(plugin.getMessage("not-enough-money",
                        "{amount}", String.format("%.2f", amount)));
                return;
            }

            // Find target UUID — try online players first
            Player target = plugin.getServer().getPlayerExact(targetName);
            UUID targetUUID = target != null ? target.getUniqueId() : UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetName).getBytes());

            vault.withdraw(player, amount);
            bm.addBounty(targetUUID, targetName, player.getUniqueId(), player.getName(), amount);
            cm.setCooldown(player.getUniqueId());

            player.sendMessage(plugin.getMessage("bounty-placed",
                    "{amount}", String.format("%.2f", amount),
                    "{target}", targetName));

            // Broadcast to the server
            String broadcast = plugin.getMessage("bounty-placed",
                    "{amount}", String.format("%.2f", amount),
                    "{target}", targetName)
                    .replace(plugin.getMessage("prefix"), "");
            plugin.getServer().broadcastMessage(
                    plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                            + "§e" + player.getName() + " §aplaced a bounty of §6$"
                            + String.format("%.2f", amount) + " §aon §e" + targetName + "§a!");

        } else if (slot == 15) {
            // CANCEL
            pendingConfirmations.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(plugin.getMessage("bounty-cancelled"));
        }
    }
}
