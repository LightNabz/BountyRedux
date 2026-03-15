package dev.bountyredux.managers;

import dev.bountyredux.BountiesPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrackingManager {

    private final BountiesPlugin plugin;

    // trackerUUID -> targetUUID
    private final Map<UUID, UUID> activeTrackers = new HashMap<>();
    // trackerUUID -> task
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    // trackerUUID -> seconds remaining
    private final Map<UUID, Integer> timeLeft = new HashMap<>();

    public TrackingManager(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean startTracking(Player tracker, Player target) {
        stopTracking(tracker, false);

        double cost = calculateCost(target.getUniqueId());

        if (!plugin.getVaultHook().has(tracker, cost)) {
            tracker.sendMessage(prefix()
                    + "§cYou need §6$" + String.format("%.2f", cost)
                    + " §cto track §e" + target.getName() + "§c.");
            return false;
        }

        plugin.getVaultHook().withdraw(tracker, cost);

        int duration = plugin.getConfig().getInt("tracking.duration", 300);
        activeTrackers.put(tracker.getUniqueId(), target.getUniqueId());
        timeLeft.put(tracker.getUniqueId(), duration);

        // Notify tracked player
        target.sendMessage(prefix() + "§c⚠ Someone is tracking you!");

        // Notify tracker
        tracker.sendMessage(prefix()
                + "§aNow tracking §e" + target.getName()
                + " §7for §6$" + String.format("%.2f", cost)
                + "§7. Expires in §e" + formatTime(duration) + "§7.");

        // Start tick task — runs every second
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int remaining = timeLeft.getOrDefault(tracker.getUniqueId(), 0) - 1;
            timeLeft.put(tracker.getUniqueId(), remaining);

            Player t = Bukkit.getPlayer(target.getUniqueId());

            // Target went offline
            if (t == null || !t.isOnline()) {
                tracker.sendMessage(prefix()
                        + "§eYour tracked target §c" + target.getName()
                        + " §ewent offline. Tracking cancelled.");
                stopTracking(tracker, false);
                return;
            }

            // 30 second warning
            if (remaining == 30) {
                tracker.sendMessage(prefix()
                        + "§e⚠ Tracking §c" + t.getName()
                        + " §ewill expire in §c30 seconds§e!");
            }

            // Expired
            if (remaining <= 0) {
                tracker.sendMessage(prefix()
                        + "§cTracking of §e" + t.getName() + " §chas expired.");
                stopTracking(tracker, false);
                return;
            }

            // Action bar: "Tracking PlayerName | 142 m"
            double distance = tracker.getWorld().equals(t.getWorld())
                    ? tracker.getLocation().distance(t.getLocation())
                    : -1;

            String distStr = distance < 0
                    ? "§7different world"
                    : "§e" + (int) distance + " m";

            String bar = "§6Tracking §e" + t.getName()
                    + " §7| " + distStr
                    + " §7| §e" + formatTime(remaining);

            tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(bar));

        }, 20L, 20L);

        tasks.put(tracker.getUniqueId(), task);
        return true;
    }

    public void stopTracking(Player tracker, boolean sendMessage) {
        UUID targetUUID = activeTrackers.remove(tracker.getUniqueId());
        timeLeft.remove(tracker.getUniqueId());

        BukkitTask task = tasks.remove(tracker.getUniqueId());
        if (task != null) task.cancel();

        // Clear action bar
        tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));

        if (sendMessage && targetUUID != null) {
            tracker.sendMessage(prefix() + "§cTracking cancelled.");
        }
    }

    public boolean isTracking(UUID trackerUUID) {
        return activeTrackers.containsKey(trackerUUID);
    }

    public void onTargetDeath(Player target) {
        UUID targetUUID = target.getUniqueId();
        activeTrackers.entrySet().stream()
                .filter(e -> e.getValue().equals(targetUUID))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(trackerUUID -> {
                    Player tracker = Bukkit.getPlayer(trackerUUID);
                    if (tracker != null) {
                        tracker.sendMessage(prefix()
                                + "§cYour tracked target §e" + target.getName()
                                + " §chas been killed. Tracking stopped.");
                        stopTracking(tracker, false);
                    } else {
                        activeTrackers.remove(trackerUUID);
                        timeLeft.remove(trackerUUID);
                        BukkitTask task = tasks.remove(trackerUUID);
                        if (task != null) task.cancel();
                    }
                });
    }

    public double calculateCost(UUID targetUUID) {
        String mode = plugin.getConfig().getString("tracking.cost-mode", "fixed");
        if (mode.equalsIgnoreCase("percentage")) {
            double total = plugin.getBountyManager().getTotalBounty(targetUUID);
            double pct   = plugin.getConfig().getDouble("tracking.percentage-cost", 5);
            return total * (pct / 100.0);
        }
        return plugin.getConfig().getDouble("tracking.fixed-cost", 500);
    }

    public void cleanup() {
        for (UUID uuid : new HashMap<>(activeTrackers).keySet()) {
            Player tracker = Bukkit.getPlayer(uuid);
            if (tracker != null) stopTracking(tracker, false);
        }
        activeTrackers.clear();
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
        timeLeft.clear();
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return m > 0 ? m + "m " + s + "s" : s + "s";
    }

    private String prefix() {
        return plugin.getConfig().getString("messages.prefix", "[Bounties] ");
    }
}