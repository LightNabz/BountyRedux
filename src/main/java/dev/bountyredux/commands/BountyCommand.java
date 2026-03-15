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

    public BountyCommand(BountiesPlugin plugin) {
        this.plugin = plugin;
        this.mainGUI = new BountyMainGUI(plugin);
        this.confirmGUI = new BountyConfirmGUI(plugin);

        // Register GUI listener here
        plugin.getServer().getPluginManager().registerEvents(new GUIListener(plugin, mainGUI, confirmGUI), plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            mainGUI.open(player, 1, true); // page 1, sort by amount
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> handleAdd(player, args);
            case "search" -> handleSearch(player, args);
            case "clear" -> handleClear(player, args);
            case "reload" -> handleReload(player);
            default -> mainGUI.open(player, 1, true);
        }
        return true;
    }

    private void handleAdd(Player player, String[] args) {
        if (!player.hasPermission("bounties.add")) {
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

        // Open confirm GUI
        GUIListener.pendingConfirmations.put(player.getUniqueId(), new Object[]{targetName, amount});
        confirmGUI.open(player, targetName, amount);
    }

    private void handleSearch(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /bounty search <player>");
            return;
        }

        String targetName = args[1];
        // Try to find by online player first
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
        if (!player.hasPermission("bounties.clear") && !player.hasPermission("bounties.admin")) {
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
        if (!player.hasPermission("bounties.reload") && !player.hasPermission("bounties.admin")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return;
        }
        plugin.reloadConfig();
        player.sendMessage(plugin.getMessage("plugin-reloaded"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "search", "clear", "reload")
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
}
