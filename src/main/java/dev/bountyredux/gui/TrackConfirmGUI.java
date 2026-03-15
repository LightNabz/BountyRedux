package dev.bountyredux.gui;

import dev.bountyredux.BountiesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class TrackConfirmGUI {

    public static final String TITLE_PREFIX = "§2Track: ";
    private final BountiesPlugin plugin;

    public TrackConfirmGUI(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player tracker, String targetName, double cost) {
        String title = TITLE_PREFIX + targetName;
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Player skull info
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetName));
            skullMeta.setDisplayName("§e" + targetName);
            skullMeta.setLore(List.of(
                    "§7Bounty: §6$" + String.format("%.2f",
                            plugin.getBountyManager().getTotalBounty(
                                    Bukkit.getOfflinePlayer(targetName).getUniqueId())),
                    "",
                    "§7Tracking cost: §6$" + String.format("%.2f", cost),
                    "§7Duration: §e" + formatTime(plugin.getConfig().getInt("tracking.duration", 300))
            ));
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(13, skull);

        // Confirm
        inv.setItem(11, makeItem(Material.LIME_CONCRETE, "§a§lCONFIRM",
                "§7Pay §6$" + String.format("%.2f", cost) + " §7to start tracking."));

        // Cancel
        inv.setItem(15, makeItem(Material.RED_CONCRETE, "§c§lCANCEL",
                "§7Cancel tracking."));

        tracker.openInventory(inv);
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return m > 0 ? m + "m " + s + "s" : s + "s";
    }
}