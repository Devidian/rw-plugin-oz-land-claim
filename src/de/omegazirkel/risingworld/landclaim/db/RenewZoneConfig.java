package de.omegazirkel.risingworld.landclaim.db;

public record RenewZoneConfig(
        long areaId,
        int intervalHours,
        long lastResetAt) {
}
