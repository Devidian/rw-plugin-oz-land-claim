package de.omegazirkel.risingworld.landclaim.exports;

public record ClaimSaleExport(
        long id,
        String world,
        long areaId,
        String ownerUuid,
        int ownerDbId,
        long price,
        long listedAt,
        String status) {
}
