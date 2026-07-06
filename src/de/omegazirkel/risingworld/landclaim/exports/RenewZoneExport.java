package de.omegazirkel.risingworld.landclaim.exports;

public record RenewZoneExport(
        String world,
        long areaId,
        int intervalHours,
        long lastResetAt,
        long nextRenewalAt,
        String borderColor,
        String frameColor) {
}
