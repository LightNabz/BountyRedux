package dev.bountyredux.model;

import java.util.UUID;

/**
 * Represents a single bounty placed on a player.
 */
public class Bounty {

    private final UUID targetUUID;
    private final String targetName;
    private final UUID placedByUUID;
    private final String placedByName;
    private double amount;

    public Bounty(UUID targetUUID, String targetName, UUID placedByUUID, String placedByName, double amount) {
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.placedByUUID = placedByUUID;
        this.placedByName = placedByName;
        this.amount = amount;
    }

    public UUID getTargetUUID() { return targetUUID; }
    public String getTargetName() { return targetName; }
    public UUID getPlacedByUUID() { return placedByUUID; }
    public String getPlacedByName() { return placedByName; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    @Override
    public String toString() {
        return "Bounty{target=" + targetName + ", by=" + placedByName + ", amount=" + amount + "}";
    }
}
