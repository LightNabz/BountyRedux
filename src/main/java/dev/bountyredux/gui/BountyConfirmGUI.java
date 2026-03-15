package dev.bountyredux.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.bountyredux.BountiesPlugin;

import java.util.Arrays;

/**
 * Confirmation GUI — prevent accidental bounty placements.
 */
public class BountyConfirmGUI {

    public static final String CONFIRM_TITLE_PREFIX = "§aConfirm Bounty: ";
    private final BountiesPlugin plugin;

    public BountyConfirmGUI(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player placer, String targetName, double amount) {
        String title = CONFIRM_TITLE_PREFIX + "§6$" + String.format("%.2f", amount);
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Fill with glass
        // ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, "§r");
        // for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Info item
        ItemStack info = makeItem(Material.GOLD_INGOT, "§6§lBounty Details",
                "§7Target: §e" + targetName,
                "§7Amount: §6$" + String.format("%.2f", amount),
                "",
                "§7Confirm or cancel below.");
        inv.setItem(13, info);

        // Confirm — green
        ItemStack confirm = makeItem(Material.LIME_CONCRETE, "§a§lCONFIRM",
                "§7Click to place the bounty.");
        inv.setItem(11, confirm);

        // Cancel — red
        ItemStack cancel = makeItem(Material.RED_CONCRETE, "§c§lCANCEL",
                "§7Click to cancel.");
        inv.setItem(15, cancel);

        placer.openInventory(inv);
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
