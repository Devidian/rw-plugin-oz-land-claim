package de.omegazirkel.risingworld.landclaim.exports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class PlayerMapVisitExportService {
    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_PAGE_SIZE = 100;
    private final Connection connection;

    public PlayerMapVisitExportService(Connection connection) {
        this.connection = connection;
    }

    public synchronized PlayerMapVisitsExportResponse export(String playerUid, String world, int page, int pageSize)
            throws SQLException {
        if (playerUid == null || playerUid.isBlank() || page < 0 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException();
        }
        List<PlayerMapVisitExport> sectors = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT sector_x, sector_z, bitmap, updated_at_ms
                FROM landClaimPlayerMapVisits
                WHERE player_uuid = ? AND world = ?
                ORDER BY sector_x ASC, sector_z ASC
                LIMIT ? OFFSET ?
                """)) {
            statement.setString(1, playerUid);
            statement.setString(2, world);
            statement.setInt(3, pageSize + 1);
            statement.setLong(4, (long) page * pageSize);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    sectors.add(new PlayerMapVisitExport(result.getInt("sector_x"), result.getInt("sector_z"),
                            Base64.getEncoder().encodeToString(result.getBytes("bitmap")),
                            result.getLong("updated_at_ms")));
                }
            }
        }
        boolean hasMore = sectors.size() > pageSize;
        if (hasMore) sectors.remove(sectors.size() - 1);
        return new PlayerMapVisitsExportResponse(SCHEMA_VERSION, playerUid, world, page, pageSize, hasMore, sectors);
    }
}
