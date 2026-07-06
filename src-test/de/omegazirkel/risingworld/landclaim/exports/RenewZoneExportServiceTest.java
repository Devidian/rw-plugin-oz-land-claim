package de.omegazirkel.risingworld.landclaim.exports;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.junit.Test;

import de.omegazirkel.risingworld.landclaim.PluginSettings;
import net.risingworld.api.utils.ColorRGBA;

public class RenewZoneExportServiceTest {

    @Test
    public void exportsRenewZonesForWorld() throws Exception {
        try (Connection connection = database()) {
            seed(connection);
            PluginSettings settings = PluginSettings.getInstance();
            settings.renewAreaBorderColor = new ColorRGBA(0x00C2A89c);
            settings.renewAreaFrameColor = new ColorRGBA(0x00C2A8AA);

            RenewZonesExportResponse response =
                    new RenewZoneExportService(connection, settings).exportRenewZones("world", null);

            assertEquals(1, response.schemaVersion());
            assertEquals("world", response.worldName());
            assertEquals(2, response.zones().size());
            assertEquals(42L, response.zones().get(0).areaId());
            assertEquals(12, response.zones().get(0).intervalHours());
            assertEquals(1000L, response.zones().get(0).lastResetAt());
            assertEquals(43_201_000L, response.zones().get(0).nextRenewalAt());
            assertEquals("#00C2A89C", response.zones().get(0).borderColor());
            assertEquals("#00C2A8AA", response.zones().get(0).frameColor());
        }
    }

    @Test
    public void filtersByLastChange() throws Exception {
        try (Connection connection = database()) {
            seed(connection);

            RenewZonesExportResponse response =
                    new RenewZoneExportService(connection, PluginSettings.getInstance()).exportRenewZones("world", 2000L);

            assertEquals(1, response.zones().size());
            assertEquals(43L, response.zones().get(0).areaId());
        }
    }

    private static Connection database() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE renewZoneConfigs (
                        world TEXT NOT NULL,
                        area_id BIGINT NOT NULL,
                        interval_hours INTEGER NOT NULL,
                        last_reset_at BIGINT NOT NULL DEFAULT 0,
                        updated_at BIGINT NOT NULL DEFAULT 0,
                        PRIMARY KEY(world, area_id)
                    );
                    """);
        }
        return connection;
    }

    private static void seed(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO renewZoneConfigs
                (world, area_id, interval_hours, last_reset_at, updated_at)
                VALUES ('world', 42, 12, 1000, 1000),
                       ('other-world', 99, 24, 2000, 2000),
                       ('world', 43, 1, 0, 3000);
                """)) {
            statement.executeUpdate();
        }
    }
}
