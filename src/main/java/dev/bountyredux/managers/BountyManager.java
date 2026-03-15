package dev.bountyredux.managers;

import dev.bountyredux.BountiesPlugin;
import dev.bountyredux.database.DatabaseManager;
import dev.bountyredux.model.Bounty;

import java.util.*;

/**
 * Write-through cache — all reads hit memory, all writes hit DB first then cache.
 */
public class BountyManager {

    private final BountiesPlugin plugin;
    private final DatabaseManager db;

    private final Map<UUID, List<Bounty>> cache = new HashMap<>();

    public BountyManager(BountiesPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        loadFromDatabase();
    }

    private void loadFromDatabase() {
        cache.clear();
        List<Bounty> all = db.loadAllBounties();
        for (Bounty b : all) {
            cache.computeIfAbsent(b.getTargetUUID(), k -> new ArrayList<>()).add(b);
        }
        plugin.getLogger().info("Loaded " + all.size() + " bounty/bounties from database.");
    }

    public void addBounty(UUID targetUUID, String targetName, UUID placerUUID, String placerName, double amount) {
        Bounty bounty = new Bounty(targetUUID, targetName, placerUUID, placerName, amount);
        db.insertBounty(bounty);
        cache.computeIfAbsent(targetUUID, k -> new ArrayList<>()).add(bounty);
    }

    public double clearBounties(UUID targetUUID) {
        List<Bounty> removed = cache.remove(targetUUID);
        if (removed == null || removed.isEmpty()) return 0;
        db.deleteBounties(targetUUID);
        return removed.stream().mapToDouble(Bounty::getAmount).sum();
    }

    public boolean hasBounty(UUID targetUUID) {
        List<Bounty> list = cache.get(targetUUID);
        return list != null && !list.isEmpty();
    }

    public double getTotalBounty(UUID targetUUID) {
        List<Bounty> list = cache.get(targetUUID);
        if (list == null || list.isEmpty()) return 0;
        return list.stream().mapToDouble(Bounty::getAmount).sum();
    }

    public List<Bounty> getBounties(UUID targetUUID) {
        return cache.getOrDefault(targetUUID, Collections.emptyList());
    }

    public String getTargetName(UUID targetUUID) {
        List<Bounty> list = cache.get(targetUUID);
        if (list == null || list.isEmpty()) return targetUUID.toString();
        return list.get(0).getTargetName();
    }

    public List<Map.Entry<UUID, Double>> getTopBounties(int limit) {
        Map<UUID, Double> totals = new HashMap<>();
        for (Map.Entry<UUID, List<Bounty>> entry : cache.entrySet()) {
            double total = entry.getValue().stream().mapToDouble(Bounty::getAmount).sum();
            if (total > 0) totals.put(entry.getKey(), total);
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }
}