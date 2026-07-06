package de.omegazirkel.risingworld.landclaim.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.omegazirkel.risingworld.LandClaim;
import net.risingworld.api.World;

public class RenewZoneConfigService {
    private static final String TABLE = "renewZoneConfigs";

    private final Connection connection;
    private final String world;

    public RenewZoneConfigService(Connection connection) throws SQLException {
        this(connection, World.getName());
    }

    public RenewZoneConfigService(Connection connection, String world) throws SQLException {
        this.connection = connection;
        this.world = world == null || world.isBlank() ? "" : world;
        init();
    }

    public Optional<RenewZoneConfig> find(long areaId) {
        if (areaId <= 0) {
            return Optional.empty();
        }
        String sql = "SELECT area_id, interval_hours, last_reset_at FROM " + TABLE
                + " WHERE world = ? AND area_id = ?;";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setLong(2, areaId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not read renew zone config: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public List<RenewZoneConfig> list() {
        String sql = "SELECT area_id, interval_hours, last_reset_at FROM " + TABLE
                + " WHERE world = ? ORDER BY area_id;";
        List<RenewZoneConfig> configs = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    configs.add(read(result));
                }
            }
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not list renew zone configs: " + ex.getMessage());
        }
        return configs;
    }

    public List<RenewZoneConfig> dueAt(long nowMs) {
        return list().stream()
                .filter(config -> config.intervalHours() > 0)
                .filter(config -> config.lastResetAt() <= 0
                        || config.lastResetAt() + config.intervalHours() * 3_600_000L <= nowMs)
                .toList();
    }

    public RenewZoneConfig save(long areaId, int intervalHours, long lastResetAt) {
        if (areaId <= 0) {
            return null;
        }
        int normalizedIntervalHours = Math.max(1, intervalHours);
        long normalizedLastResetAt = Math.max(0L, lastResetAt);
        String sql = """
                INSERT INTO renewZoneConfigs(world, area_id, interval_hours, last_reset_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(world, area_id) DO UPDATE SET
                    interval_hours = excluded.interval_hours,
                    last_reset_at = excluded.last_reset_at,
                    updated_at = excluded.updated_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setLong(2, areaId);
            statement.setInt(3, normalizedIntervalHours);
            statement.setLong(4, normalizedLastResetAt);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not save renew zone config: " + ex.getMessage());
            return null;
        }
        return find(areaId).orElse(null);
    }

    public boolean markReset(long areaId, long resetAt) {
        String sql = "UPDATE " + TABLE + " SET last_reset_at = ?, updated_at = ? WHERE world = ? AND area_id = ?;";
        long normalizedResetAt = Math.max(0L, resetAt);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, normalizedResetAt);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, world);
            statement.setLong(4, areaId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not mark renew zone reset: " + ex.getMessage());
            return false;
        }
    }

    public boolean delete(long areaId) {
        String sql = "DELETE FROM " + TABLE + " WHERE world = ? AND area_id = ?;";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setLong(2, areaId);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            LandClaim.logger().error("Could not delete renew zone config: " + ex.getMessage());
            return false;
        }
    }

    private RenewZoneConfig read(ResultSet result) throws SQLException {
        return new RenewZoneConfig(
                result.getLong("area_id"),
                Math.max(1, result.getInt("interval_hours")),
                Math.max(0L, result.getLong("last_reset_at")));
    }

    private void init() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + " world TEXT NOT NULL,"
                + " area_id BIGINT NOT NULL,"
                + " interval_hours INTEGER NOT NULL,"
                + " last_reset_at BIGINT NOT NULL DEFAULT 0,"
                + " updated_at BIGINT NOT NULL DEFAULT 0,"
                + " PRIMARY KEY(world, area_id)"
                + ");";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_renewZoneConfigs_world_interval ON "
                    + TABLE + "(world, interval_hours, last_reset_at);");
        }
    }
}
