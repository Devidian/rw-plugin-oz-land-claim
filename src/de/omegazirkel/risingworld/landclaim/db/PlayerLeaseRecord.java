package de.omegazirkel.risingworld.landclaim.db;

/** Persisted offer/tenancy state for an ordinary player claim. */
public record PlayerLeaseRecord(long areaId, String landlordUuid, int landlordDbId, String tenantUuid, int tenantDbId,
        long purchasePrice, long dailyRent, boolean purchaseAllowed, long rentCredit, String lastBillingDate) {
    public boolean occupied() { return tenantDbId > 0 && tenantUuid != null && !tenantUuid.isBlank(); }
}
