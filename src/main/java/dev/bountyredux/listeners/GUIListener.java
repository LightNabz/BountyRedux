package dev.bountyredux.listeners;

import dev.bountyredux.BountiesPlugin;
import dev.bountyredux.gui.BountyConfirmGUI;
import dev.bountyredux.gui.BountyMainGUI;
import dev.bountyredux.managers.BountyManager;
import dev.bountyredux.managers.CooldownManager;
import dev.bountyredux.vault.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class GUIListener implements Listener {

    private final BountiesPlugin plugin;
    private final BountyMainGUI mainGUI;
    private final BountyConfirmGUI confirmGUI;

    public static final Map<UUID, Object[]> pendingConfirmations = new HashMap<>();
    private final Map<UUID, Integer> playerPage      = new HashMap<>();
    private final Map<UUID, Boolean> playerSortMode  = new HashMap<>();

    // Players currently waiting to type a name in chat for search
    private final Set<UUID> awaitingChatSearch = new HashSet<>();

    public GUIListener(BountiesPlugin plugin, BountyMainGUI mainGUI, BountyConfirmGUI confirmGUI) {
        this.plugin     = plugin;
        this.mainGUI    = mainGUI;
        this.confirmGUI = confirmGUI;
    }

    // ── Inventory clicks ──────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = event.getView().getTitle();

        // ── Main GUI ──────────────────────────────────────────────────────────
        if (title.startsWith(plugin.getConfig().getString("settings.gui-title", "Bounty Menu") + " (Page ")) {
            event.setCancelled(true);
            int slot = event.getSlot();

            // Slot 47 — Add bounty
            if (slot == BountyMainGUI.SLOT_ADD) {
                player.closeInventory();
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                        + "§7Use §e/bounty add <player> <amount> §7to place a bounty.");
                return;
            }

            // Slot 48 — Sort toggle
            if (slot == BountyMainGUI.SLOT_SORT) {
                boolean current = playerSortMode.getOrDefault(player.getUniqueId(), true);
                playerSortMode.put(player.getUniqueId(), !current);
                int page = playerPage.getOrDefault(player.getUniqueId(), 1);
                mainGUI.open(player, page, !current);
                return;
            }

            // Slot 49 — Search via chat input
            if (slot == BountyMainGUI.SLOT_SEARCH) {
                player.closeInventory();
                awaitingChatSearch.add(player.getUniqueId());
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                        + "§eType the player name in chat to search. §7(or §ccancel§7)");
                return;
            }

            // Slot 53 — Next page
            if (slot == BountyMainGUI.SLOT_NEXT) {
                int page = playerPage.getOrDefault(player.getUniqueId(), 1) + 1;
                playerPage.put(player.getUniqueId(), page);
                mainGUI.open(player, page, playerSortMode.getOrDefault(player.getUniqueId(), true));
                return;
            }

            // Slot 45 — Prev page
            if (slot == BountyMainGUI.SLOT_PREV) {
                int page = Math.max(1, playerPage.getOrDefault(player.getUniqueId(), 1) - 1);
                playerPage.put(player.getUniqueId(), page);
                mainGUI.open(player, page, playerSortMode.getOrDefault(player.getUniqueId(), true));
                return;
            }

            // Player head click — show bounty details in chat
            if (slot < 45 && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    String targetName = meta.getOwningPlayer().getName();
                    if (targetName == null) return;
                    UUID targetUUID = meta.getOwningPlayer().getUniqueId();
                    double total = plugin.getBountyManager().getTotalBounty(targetUUID);
                    player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                            + "§e" + targetName + " §7— Bounty: §6$" + String.format("%.2f", total));
                }
            }
        }

        // ── Confirm GUI ───────────────────────────────────────────────────────
        if (title.startsWith(BountyConfirmGUI.CONFIRM_TITLE_PREFIX)) {
            event.setCancelled(true);
            handleConfirmClick(player, event.getSlot());
        }
    }

    // ── Chat search input ─────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingChatSearch.contains(player.getUniqueId())) return;

        event.setCancelled(true);
        awaitingChatSearch.remove(player.getUniqueId());

        String query = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (query.equalsIgnoreCase("cancel")) {
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                        + "§cSearch cancelled.");
                return;
            }

            BountyManager bm = plugin.getBountyManager();
            UUID found       = null;
            String foundName = null;

            for (var entry : bm.getTopBounties(999)) {
                String name = bm.getTargetName(entry.getKey());
                if (name.equalsIgnoreCase(query)
                        || name.toLowerCase().startsWith(query.toLowerCase())) {
                    found     = entry.getKey();
                    foundName = name;
                    break;
                }
            }

            if (found == null) {
                player.sendMessage(plugin.getMessage("bounty-not-found"));
                return;
            }

            double total = bm.getTotalBounty(found);
            var bounties = bm.getBounties(found);
            player.sendMessage("§6--- Bounties on §e" + foundName + " §6---");
            for (var b : bounties) {
                player.sendMessage("§7• §6$" + String.format("%.2f", b.getAmount())
                        + " §7by §e" + b.getPlacedByName());
            }
            player.sendMessage("§6Total: §a$" + String.format("%.2f", total));
        });
    }

    // ── Confirm GUI ───────────────────────────────────────────────────────────

    private void handleConfirmClick(Player player, int slot) {
        Object[] pending = pendingConfirmations.get(player.getUniqueId());
        if (pending == null) { player.closeInventory(); return; }

        String targetName = (String) pending[0];
        double amount     = (double) pending[1];

        if (slot == 11) {
            pendingConfirmations.remove(player.getUniqueId());
            player.closeInventory();

            VaultHook vault    = plugin.getVaultHook();
            BountyManager bm   = plugin.getBountyManager();
            CooldownManager cm = plugin.getCooldownManager();

            if (!vault.has(player, amount)) {
                player.sendMessage(plugin.getMessage("not-enough-money",
                        "{amount}", String.format("%.2f", amount)));
                return;
            }

            Player target   = plugin.getServer().getPlayerExact(targetName);
            UUID targetUUID = target != null
                    ? target.getUniqueId()
                    : Bukkit.getOfflinePlayer(targetName).getUniqueId();

            vault.withdraw(player, amount);
            bm.addBounty(targetUUID, targetName, player.getUniqueId(), player.getName(), amount);
            cm.setCooldown(player.getUniqueId());

            plugin.getServer().broadcastMessage(
                    plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                            + "§e" + player.getName() + " §aplaced a bounty of §6$"
                            + String.format("%.2f", amount) + " §aon §e" + targetName + "§a!");

        } else if (slot == 15) {
            pendingConfirmations.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(plugin.getMessage("bounty-cancelled"));
        }
    }
}