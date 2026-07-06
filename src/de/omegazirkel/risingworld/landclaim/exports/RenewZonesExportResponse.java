package de.omegazirkel.risingworld.landclaim.exports;

import java.util.List;

public record RenewZonesExportResponse(
        int schemaVersion,
        String worldName,
        List<RenewZoneExport> zones) {

    public RenewZonesExportResponse {
        zones = List.copyOf(zones);
    }
}
