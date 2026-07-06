package de.omegazirkel.risingworld.landclaim.exports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.landclaim.PluginSettings;
import net.risingworld.api.utils.ColorRGBA;

public final class RenewZoneExportService {
    private static final int SCHEMA_VERSION = 1;

    private final Connection connection;
    private final PluginSettings settings;

    public RenewZoneExportService(Connection connection, PluginSettings settings) {
        this.connection = connection;
        this.settings = settings;
    }

    public RenewZonesExportResponse exportRenewZones(String worldName, Long lastChange) throws SQLException {
        String world = worldName == null ? "" : worldName.trim();
        long cursor = lastChange == null ? -1L : lastChange.longValue();
        List<RenewZoneExport> zones = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT world, area_id, interval_hours, last_reset_at
                FROM renewZoneConfigs
                WHERE world = ? AND updated_at > ?
                ORDER BY area_id;
                """)) {
            statement.setString(1, world);
            statement.setLong(2, cursor);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int intervalHours = Math.max(1, result.getInt("interval_hours"));
                    long lastResetAt = Math.max(0L, result.getLong("last_reset_at"));
                    zones.add(new RenewZoneExport(
                            result.getString("world"),
                            result.getLong("area_id"),
                            intervalHours,
                            lastResetAt,
                            nextRenewalAt(lastResetAt, intervalHours),
                            colorHex(settings.renewAreaBorderColor),
                            colorHex(settings.renewAreaFrameColor)));
                }
            }
        }
        return new RenewZonesExportResponse(SCHEMA_VERSION, world, zones);
    }

    private long nextRenewalAt(long lastResetAt, int intervalHours) {
        return lastResetAt <= 0 ? 0L : lastResetAt + intervalHours * 3_600_000L;
    }

    private String colorHex(ColorRGBA color) {
        return String.format("#%08X", color.toIntRGBA());
    }
}
