package dev.bountyredux.listeners;

import dev.bountyredux.BountiesPlugin;
import dev.bountyredux.gui.BountyConfirmGUI;
import dev.bountyredux.gui.BountyMainGUI;
import dev.bountyredux.managers.BountyManager;
import dev.bountyredux.managers.CooldownManager;
import dev.bountyredux.gui.TrackConfirmGUI;
import dev.bountyredux.managers.TrackingManager;
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
    private final TrackConfirmGUI trackConfirmGUI;

    public static final Map<UUID, Object[]> pendingConfirmations = new HashMap<>();
    public static final Map<UUID, String> pendingTrack = new HashMap<>();
    private final Map<UUID, Integer> playerPage     = new HashMap<>();
    private final Map<UUID, Boolean> playerSortMode = new HashMap<>();

    // Players currently waiting to type a name in chat for search
    private final Set<UUID> awaitingChatSearch = new HashSet<>();

    public GUIListener(BountiesPlugin plugin, BountyMainGUI mainGUI, BountyConfirmGUI confirmGUI, TrackConfirmGUI trackConfirmGUI) {
        this.plugin          = plugin;
        this.mainGUI         = mainGUI;
        this.confirmGUI      = confirmGUI;
        this.trackConfirmGUI = trackConfirmGUI;
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

            // ── Player head click — open track confirm ────────────────────────
            if (slot < 45 && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                if (!player.hasPermission("bountyredux.track")) {
                    player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                            + "§cYou don't have permission to track players.");
                    return;
                }

                SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
                if (meta == null) return;

                // FIX: Use owning player profile first — way more reliable than parsing display name
                String targetName = null;
                if (meta.getOwningPlayer() != null && meta.getOwningPlayer().getName() != null) {
                    targetName = meta.getOwningPlayer().getName();
                } else {
                    // Fallback: strip ALL color/format codes from display name, not just §c
                    String displayName = meta.getDisplayName();
                    if (displayName == null || displayName.isEmpty()) return;
                    targetName = displayName.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                }

                if (targetName.isEmpty()) return;

                if (targetName.equalsIgnoreCase(player.getName())) {
                    player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                            + "§cYou can't track yourself!");
                    return;
                }

                // FIX: Null-check target BEFORE calling any method on it
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                            + "§c" + targetName + " is not found or not online — cannot track.");
                    return;
                }

                // At this point target is guaranteed non-null, safe to call isOnline()
                if (!target.isOnline()) {
                    player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                            + "§c" + targetName + " is not online — cannot track.");
                    return;
                }

                player.closeInventory();
                double cost = plugin.getTrackingManager().calculateCost(target.getUniqueId());
                pendingTrack.put(player.getUniqueId(), targetName);
                trackConfirmGUI.open(player, targetName, cost);
            }
        }

        // ── Confirm GUI ───────────────────────────────────────────────────────
        if (title.startsWith(BountyConfirmGUI.CONFIRM_TITLE_PREFIX)) {
            event.setCancelled(true);
            handleConfirmClick(player, event.getSlot());
        }

        // ── Track Confirm GUI ─────────────────────────────────────────────────
        if (title.startsWith(TrackConfirmGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
            String targetName = pendingTrack.get(player.getUniqueId());
            if (targetName == null) { player.closeInventory(); return; }

            if (event.getSlot() == 11) {
                // CONFIRM
                pendingTrack.remove(player.getUniqueId());
                player.closeInventory();

                // FIX: Re-validate target is still online at confirm time
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                            + "§c" + targetName + " went offline.");
                    return;
                }
                plugin.getTrackingManager().startTracking(player, target);

            } else if (event.getSlot() == 15) {
                // CANCEL
                pendingTrack.remove(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                        + "§cTracking cancelled.");
            }
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

    // ── Confirm GUI handler ───────────────────────────────────────────────────

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
                    plugin.getConfig().getString("messages.prefix", "[Bounty] ")
                            + "§e" + player.getName() + " §aplaced a bounty of §6$"
                            + String.format("%.2f", amount) + " §aon §e" + targetName + "§a!");

        } else if (slot == 15) {
            pendingConfirmations.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(plugin.getMessage("bounty-cancelled"));
        }
    }
}