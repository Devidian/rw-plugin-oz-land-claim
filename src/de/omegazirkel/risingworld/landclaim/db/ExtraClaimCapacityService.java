package de.omegazirkel.risingworld.landclaim.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import de.omegazirkel.risingworld.LandClaim;
import net.risingworld.api.objects.Player;

public class ExtraClaimCapacityService {
    private static final String TABLE = "extraClaimCapacity";

    private final Connection connection;

    public ExtraClaimCapacityService(Connection connection) throws SQLException {
        this.connection = connection;
        init();
    }

    public int getPurchasedCapacity(Player player) {
        if (player == null) {
            return 0;
        }
        return getPurchasedCapacity(player.getUID());
    }

    public int getPurchasedCapacity(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) {
            return 0;
        }
        String sql = "SELECT purchased_capacity FROM " + TABLE + " WHERE player_uuid = ?;";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Math.max(0, result.getInt("purchased_capacity")) : 0;
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not read extra claim capacity: " + ex.getMessage());
            return 0;
        }
    }

    public int addPurchasedCapacity(Player player, int amount) {
        if (player == null || amount <= 0) {
            return getPurchasedCapacity(player);
        }
        long now = System.currentTimeMillis();
        String sql = """
                INSERT INTO extraClaimCapacity(player_uuid, player_dbid, purchased_capacity, updated_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET
                    player_dbid = excluded.player_dbid,
                    purchased_capacity = purchased_capacity + excluded.purchased_capacity,
                    updated_at = excluded.updated_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getUID());
            statement.setInt(2, player.getDbID());
            statement.setInt(3, amount);
            statement.setLong(4, now);
            statement.executeUpdate();
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not add extra claim capacity: " + ex.getMessage());
        }
        return getPurchasedCapacity(player);
    }

    private void init() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + " player_uuid TEXT NOT NULL PRIMARY KEY,"
                + " player_dbid INTEGER NOT NULL DEFAULT 0,"
                + " purchased_capacity INTEGER NOT NULL DEFAULT 0,"
                + " updated_at BIGINT NOT NULL DEFAULT 0"
                + ");";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
