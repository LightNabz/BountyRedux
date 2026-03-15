package dev.bountyredux.gui;

import dev.bountyredux.BountiesPlugin;
import dev.bountyredux.managers.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class BountyMainGUI {

    public static final int SLOT_SORT   = 48;
    public static final int SLOT_ADD    = 49;
    public static final int SLOT_SEARCH = 50;
    public static final int SLOT_NEXT   = 53;
    public static final int SLOT_PREV   = 45;

    public static final int PAGE_SIZE = 45;

    private final BountiesPlugin plugin;

    public BountyMainGUI(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page, boolean sortByAmount) {
        String title = plugin.getConfig().getString("settings.gui-title", "Bounty Menu") + " (Page " + page + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        BountyManager bm = plugin.getBountyManager();
        List<Map.Entry<UUID, Double>> entries = new ArrayList<>(bm.getTopBounties(999));

        if (!sortByAmount) {
            entries.sort(Comparator.comparing(e -> bm.getTargetName(e.getKey())));
        }

        int startIndex = (page - 1) * PAGE_SIZE;
        int endIndex   = Math.min(startIndex + PAGE_SIZE, entries.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<UUID, Double> entry = entries.get(i);
            inv.setItem(i - startIndex, makeSkull(
                    bm.getTargetName(entry.getKey()), entry.getValue()));
        }

        // Slot 47 — Skeleton skull: Add bounty
        inv.setItem(SLOT_ADD, makeItem(Material.SKELETON_SKULL, "§eAdd Bounty",
                "§7Usage: §f/bounty add <player> <amount>"));

        // Slot 48 — Hopper: Sort
        String sortLabel = sortByAmount ? "§aSorting: §fAmount ▼" : "§aSorting: §fName A-Z";
        inv.setItem(SLOT_SORT, makeItem(Material.HOPPER, sortLabel, "§7Click to toggle sort."));

        // Slot 49 — Sign: Search
        inv.setItem(SLOT_SEARCH, makeItem(Material.OAK_SIGN, "§eSearch Player",
                "§7Click to search a player's bounty."));

        // Slot 53 — Arrow: Next page (only if there is one)
        if (endIndex < entries.size()) {
            inv.setItem(SLOT_NEXT, makeItem(Material.ARROW, "§eNext Page →",
                    "§7Go to page §f" + (page + 1)));
        }

        // Slot 45 — Arrow: Prev page (only if page > 1)
        if (page > 1) {
            inv.setItem(SLOT_PREV, makeItem(Material.ARROW, "§e← Previous Page",
                    "§7Go to page §f" + (page - 1)));
        }

        player.openInventory(inv);
    }

    private ItemStack makeSkull(String playerName, double bountyTotal) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        meta.setDisplayName("§c" + playerName);
        meta.setLore(List.of("§7Bounty: §6$" + String.format("%.2f", bountyTotal)));
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}