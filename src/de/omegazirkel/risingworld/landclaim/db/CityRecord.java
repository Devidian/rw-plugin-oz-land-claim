package de.omegazirkel.risingworld.landclaim.db;

import net.risingworld.api.utils.Vector3i;

public record CityRecord(long areaId, String world, String name, Vector3i center, int radius, long foundedAt,
        Boolean allowPrivateClaimsOverride, Long privateClaimPriceOverride) {
}
