package de.omegazirkel.risingworld.landclaim.exports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.landclaim.db.ClaimSaleStatus;

public final class ClaimSaleExportService {
    private static final int SCHEMA_VERSION = 1;

    private final Connection connection;

    public ClaimSaleExportService(Connection connection) {
        this.connection = connection;
    }

    public ClaimSalesExportResponse exportActiveListings(String worldName, Long lastChange) throws SQLException {
        String world = worldName == null ? "" : worldName.trim();
        long cursor = lastChange == null ? -1L : lastChange.longValue();
        List<ClaimSaleExport> listings = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, world, area_id, owner_uuid, owner_dbid, price, listed_at, status
                FROM claimSaleListings
                WHERE world = ? AND status = ? AND listed_at > ?
                ORDER BY listed_at DESC, id DESC;
                """)) {
            statement.setString(1, world);
            statement.setString(2, ClaimSaleStatus.ACTIVE.name());
            statement.setLong(3, cursor);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    listings.add(new ClaimSaleExport(
                            result.getLong("id"),
                            result.getString("world"),
                            result.getLong("area_id"),
                            result.getString("owner_uuid"),
                            result.getInt("owner_dbid"),
                            result.getLong("price"),
                            result.getLong("listed_at"),
                            result.getString("status")));
                }
            }
        }
        return new ClaimSalesExportResponse(SCHEMA_VERSION, world, listings);
    }
}
