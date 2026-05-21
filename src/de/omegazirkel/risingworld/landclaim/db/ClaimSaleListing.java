package de.omegazirkel.risingworld.landclaim.db;

public record ClaimSaleListing(
        long id,
        String world,
        long areaId,
        String ownerUuid,
        int ownerDbId,
        long price,
        long listedAt,
        String buyerUuid,
        int buyerDbId,
        long purchasedAt,
        ClaimSaleStatus status) {

    public boolean isActive() {
        return status == ClaimSaleStatus.ACTIVE;
    }
}
