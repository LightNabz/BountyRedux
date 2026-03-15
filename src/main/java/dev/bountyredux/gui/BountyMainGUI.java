package dev.bountyredux.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.bountyredux.BountiesPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main bounty GUI — shows top bounties and nav options.
 */
public class BountyMainGUI {

    private final BountiesPlugin plugin;

    public BountyMainGUI(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = plugin.getConfig().getString("settings.gui-title", "§6§lBounty Menu");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill border with glass panes
        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, "§r");
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9) inv.setItem(i, border);
        for (int i = 17; i < 54; i += 9) inv.setItem(i, border);

        // Add bounty button
        ItemStack addBtn = makeItem(Material.GOLD_INGOT, "§6§lAdd Bounty",
                "§7Place a bounty on a player.",
                "§7Click to open.");
        inv.setItem(20, addBtn);

        // Search button
        ItemStack searchBtn = makeItem(Material.SPYGLASS, "§e§lSearch Bounties",
                "§7Search for a player's bounties.",
                "§7Click to open.");
        inv.setItem(22, searchBtn);

        // Top bounties section — slots 28-36
        int topCount = plugin.getConfig().getInt("settings.top-bounties-display", 9);
        List<Map.Entry<UUID, Double>> top = plugin.getBountyManager().getTopBounties(topCount);

        int slot = 28;
        for (Map.Entry<UUID, Double> entry : top) {
            if (slot > 43) break;
            String name = plugin.getBountyManager().getTargetName(entry.getKey());
            double amount = entry.getValue();
            ItemStack skull = makeItem(Material.PLAYER_HEAD, "§c" + name,
                    "§7Total Bounty: §6$" + String.format("%.2f", amount));
            inv.setItem(slot, skull);
            slot++;
        }

        player.openInventory(inv);
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
