package de.omegazirkel.risingworld.landclaim.db;

public record LeaseholdRecord(long areaId, long cityAreaId, long purchasePrice, long dailyRent,
        boolean purchaseAllowed, boolean rentAllowed, String ownerUuid, int ownerDbId, String ownershipType,
        long paidRentCredit, String lastBillingDate) {
    public boolean occupied() {
        return ownerDbId > 0 && ownerUuid != null && !ownerUuid.isBlank();
    }

    public boolean rented() {
        return occupied() && "RENTED".equals(ownershipType);
    }
}
