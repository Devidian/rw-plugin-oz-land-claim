package de.omegazirkel.risingworld.landclaim.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Neutralizes historical player references while leaving live leases and settlements intact. */
public final class LandClaimErasureStore {
    private final Connection database;

    public LandClaimErasureStore(Connection database) {
        this.database = database;
    }

    public boolean ready(int playerDbId) {
        try (var statement = database.prepareStatement("""
                SELECT
                    EXISTS (SELECT 1 FROM leaseholds WHERE owner_dbid = ?1)
                    OR EXISTS (SELECT 1 FROM playerLeaseholds WHERE landlord_dbid = ?1 OR tenant_dbid = ?1)
                    OR EXISTS (SELECT 1 FROM unclaimedLeaseholds WHERE tenant_dbid = ?1)
                    OR EXISTS (SELECT 1 FROM landClaimEconomyOperations WHERE player_dbid = ?1
                               AND status NOT IN ('COMPLETED','REVERSED','FAILED'))
                """)) {
            statement.setInt(1, playerDbId);
            try (var row = statement.executeQuery()) { return row.next() && !row.getBoolean(1); }
        } catch (SQLException ex) {
            return false;
        }
    }

    public boolean erase(int playerDbId) throws SQLException {
        boolean autoCommit = database.getAutoCommit();
        database.setAutoCommit(false);
        try {
            if (!ready(playerDbId)) {
                database.rollback();
                return false;
            }
            for (String sql : List.of(
                    "UPDATE claimSaleListings SET status = 'WITHDRAWN' WHERE owner_dbid = ?1 AND status = 'ACTIVE'",
                    "UPDATE claimSaleListings SET owner_dbid = 0, owner_uuid = '' WHERE owner_dbid = ?1",
                    "UPDATE claimSaleListings SET buyer_dbid = 0, buyer_uuid = '' WHERE buyer_dbid = ?1",
                    "UPDATE landClaimEconomyOperations SET player_dbid = 0 WHERE player_dbid = ?1",
                    "DELETE FROM playerLeasePermissionSnapshots WHERE player_dbid = ?1",
                    "DELETE FROM landClaimPendingNotifications WHERE player_dbid = ?1",
                    "DELETE FROM landClaimPlayerLocales WHERE player_dbid = ?1")) {
                try (var statement = database.prepareStatement(sql)) {
                    statement.setInt(1, playerDbId);
                    statement.executeUpdate();
                }
            }
            database.commit();
            return true;
        } catch (SQLException | RuntimeException ex) {
            database.rollback();
            throw ex;
        } finally {
            database.setAutoCommit(autoCommit);
        }
    }
}
