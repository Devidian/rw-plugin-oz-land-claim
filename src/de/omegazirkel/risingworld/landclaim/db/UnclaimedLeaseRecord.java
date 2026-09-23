package de.omegazirkel.risingworld.landclaim.db;

/** Persisted rent-to-own state for a claim originally leased from the world. */
public record UnclaimedLeaseRecord(long areaId, String tenantUuid, int tenantDbId, long purchasePrice,
        long dailyRent, long ancillaryCost, long rentCredit, String lastBillingDate) { }
