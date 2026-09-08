package de.omegazirkel.risingworld.landclaim.exports;

import java.util.List;

public record PlayerMapVisitsExportResponse(int schemaVersion, String playerUid, String world, int page,
        int pageSize, boolean hasMore, List<PlayerMapVisitExport> sectors) { }
