package dev.bountyredux.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dev.bountyredux.BountiesPlugin;
import dev.bountyredux.gui.BountyConfirmGUI;
import dev.bountyredux.gui.BountyMainGUI;
import dev.bountyredux.listeners.GUIListener;
import dev.bountyredux.managers.BountyManager;
import dev.bountyredux.managers.CooldownManager;
import dev.bountyredux.model.Bounty;
import dev.bountyredux.managers.TrackingManager;
import dev.bountyredux.gui.TrackConfirmGUI;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles /bounty and all its subcommands.
 */
public class BountyCommand implements CommandExecutor, TabCompleter {

    private final BountiesPlugin plugin;
    private final BountyMainGUI mainGUI;
    private final BountyConfirmGUI confirmGUI;
    private final TrackConfirmGUI trackGUI;

    public BountyCommand(BountiesPlugin plugin, BountyMainGUI mainGUI, BountyConfirmGUI confirmGUI, TrackConfirmGUI trackGUI) {
        this.plugin     = plugin;
        this.mainGUI    = mainGUI;
        this.confirmGUI = confirmGUI;
        this.trackGUI   = trackGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            mainGUI.open(player, 1, true);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add"    -> handleAdd(player, args);
            case "search" -> handleSearch(player, args);
            case "clear"  -> handleClear(player, args);
            case "reload" -> handleReload(player);
            case "track"  -> handleTrack(player, args);
            default       -> mainGUI.open(player, 1, true);
        }
        return true;
    }

    private void handleAdd(Player player, String[] args) {
        if (!player.hasPermission("bountyredux.add")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: /bounty add <player> <amount>");
            return;
        }

        CooldownManager cm = plugin.getCooldownManager();
        if (cm.isOnCooldown(player.getUniqueId())) {
            player.sendMessage(plugin.getMessage("on-cooldown",
                    "{seconds}", String.valueOf(cm.getRemainingSeconds(player.getUniqueId()))));
            return;
        }

        String targetName = args[1];
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("invalid-amount",
                    "{min}", String.valueOf(plugin.getConfig().getDouble("settings.min-bounty", 100)),
                    "{max}", String.valueOf(plugin.getConfig().getDouble("settings.max-bounty", 1000000))));
            return;
        }

        double min = plugin.getConfig().getDouble("settings.min-bounty", 100);
        double max = plugin.getConfig().getDouble("settings.max-bounty", 1000000);
        if (amount < min || amount > max) {
            player.sendMessage(plugin.getMessage("invalid-amount",
                    "{min}", String.valueOf(min),
                    "{max}", String.valueOf(max)));
            return;
        }

        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getMessage("cannot-bounty-self"));
            return;
        }

        if (!plugin.getVaultHook().has(player, amount)) {
            player.sendMessage(plugin.getMessage("not-enough-money",
                    "{amount}", String.format("%.2f", amount)));
            return;
        }

        GUIListener.pendingConfirmations.put(player.getUniqueId(), new Object[]{targetName, amount});
        confirmGUI.open(player, targetName, amount);
    }

    private void handleSearch(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /bounty search <player>");
            return;
        }

        String targetName = args[1];
        Player target = plugin.getServer().getPlayerExact(targetName);
        java.util.UUID targetUUID = target != null
                ? target.getUniqueId()
                : java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetName).getBytes());

        BountyManager bm = plugin.getBountyManager();
        if (!bm.hasBounty(targetUUID)) {
            player.sendMessage(plugin.getMessage("no-bounties"));
            return;
        }

        List<Bounty> list = bm.getBounties(targetUUID);
        player.sendMessage("§6--- Bounties on §e" + targetName + " §6---");
        for (Bounty b : list) {
            player.sendMessage("§7• §6$" + String.format("%.2f", b.getAmount())
                    + " §7by §e" + b.getPlacedByName());
        }
        player.sendMessage("§6Total: §a$" + String.format("%.2f", bm.getTotalBounty(targetUUID)));
    }

    private void handleClear(Player player, String[] args) {
        if (!player.hasPermission("bountyredux.clear") && !player.hasPermission("bountyredux.admin")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /bounty clear <player>");
            return;
        }

        String targetName = args[1];
        Player target = plugin.getServer().getPlayerExact(targetName);
        java.util.UUID targetUUID = target != null
                ? target.getUniqueId()
                : java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetName).getBytes());

        BountyManager bm = plugin.getBountyManager();
        if (!bm.hasBounty(targetUUID)) {
            player.sendMessage(plugin.getMessage("bounty-not-found"));
            return;
        }

        bm.clearBounties(targetUUID);
        player.sendMessage(plugin.getMessage("bounty-cleared", "{target}", targetName));
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("bountyredux.reload") && !player.hasPermission("bountyredux.admin")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return;
        }
        plugin.reloadConfig();
        player.sendMessage(plugin.getMessage("plugin-reloaded"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "search", "clear", "reload", "track")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add")
                || args[0].equalsIgnoreCase("search")
                || args[0].equalsIgnoreCase("clear"))) {
            return plugin.getServer().getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void handleTrack(Player player, String[] args) {
        if (!player.hasPermission("bountyredux.track")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cUsage: /bounty track <player|cancel>");
            return;
        }

        TrackingManager tm = plugin.getTrackingManager();

        if (args[1].equalsIgnoreCase("cancel")) {
            if (!tm.isTracking(player.getUniqueId())) {
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounty] ")
                        + "§cYou are not tracking anyone.");
                return;
            }
            tm.stopTracking(player, true);
            return;
        }

        // FIX: use args[1] (the name string) for the error message, NOT target.getName()
        // so we never call .getName() on a potentially null Player object
        String targetName = args[1];
        Player target = plugin.getServer().getPlayerExact(targetName);

        if (target == null) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                    + "§c" + targetName + " is not found or not online — cannot track.");
            return;
        }

        // target is guaranteed non-null here, safe to call isOnline()
        if (!target.isOnline()) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounties] ")
                    + "§c" + targetName + " is not online — cannot track.");
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounty] ")
                    + "§cYou can't track yourself!");
            return;
        }

        if (!plugin.getBountyManager().hasBounty(target.getUniqueId())) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix", "[Bounty] ")
                    + "§c" + targetName + " §chas no bounty.");
            return;
        }

        double cost = tm.calculateCost(target.getUniqueId());
        GUIListener.pendingTrack.put(player.getUniqueId(), target.getName());
        trackGUI.open(player, target.getName(), cost);
    }

}