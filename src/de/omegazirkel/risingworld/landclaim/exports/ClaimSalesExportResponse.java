package de.omegazirkel.risingworld.landclaim.exports;

import java.util.List;

public record ClaimSalesExportResponse(
        int schemaVersion,
        String worldName,
        List<ClaimSaleExport> listings) {

    public ClaimSalesExportResponse {
        listings = List.copyOf(listings);
    }
}
