package dev.bountyredux.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.bountyredux.BountiesPlugin;

/**
 * Tracks per-player cooldowns for placing bounties.
 */
public class CooldownManager {

    private final BountiesPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CooldownManager(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(UUID uuid) {
        if (!cooldowns.containsKey(uuid)) return false;
        long cooldownSeconds = plugin.getConfig().getLong("settings.add-cooldown", 60);
        long elapsed = (System.currentTimeMillis() - cooldowns.get(uuid)) / 1000;
        return elapsed < cooldownSeconds;
    }

    public long getRemainingSeconds(UUID uuid) {
        if (!cooldowns.containsKey(uuid)) return 0;
        long cooldownSeconds = plugin.getConfig().getLong("settings.add-cooldown", 60);
        long elapsed = (System.currentTimeMillis() - cooldowns.get(uuid)) / 1000;
        return Math.max(0, cooldownSeconds - elapsed);
    }

    public void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }
}
