package de.omegazirkel.risingworld.landclaim.exports;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.junit.Test;

public class ClaimSaleExportServiceTest {

    @Test
    public void exportsActiveListingsForWorld() throws Exception {
        try (Connection connection = database()) {
            seed(connection);

            ClaimSalesExportResponse response =
                    new ClaimSaleExportService(connection).exportActiveListings("world", null);

            assertEquals(1, response.schemaVersion());
            assertEquals("world", response.worldName());
            assertEquals(2, response.listings().size());
            assertEquals(3L, response.listings().get(0).id());
            assertEquals(43L, response.listings().get(0).areaId());
            assertEquals("owner-3", response.listings().get(0).ownerUuid());
            assertEquals(700L, response.listings().get(0).price());
            assertEquals("ACTIVE", response.listings().get(0).status());
            assertEquals(1L, response.listings().get(1).id());
        }
    }

    @Test
    public void filtersByLastChange() throws Exception {
        try (Connection connection = database()) {
            seed(connection);

            ClaimSalesExportResponse response =
                    new ClaimSaleExportService(connection).exportActiveListings("world", 1000L);

            assertEquals(1, response.listings().size());
            assertEquals(3L, response.listings().get(0).id());
        }
    }

    private static Connection database() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE claimSaleListings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        world TEXT NOT NULL,
                        area_id BIGINT NOT NULL,
                        owner_uuid TEXT NOT NULL,
                        owner_dbid INTEGER NOT NULL DEFAULT 0,
                        price BIGINT NOT NULL,
                        listed_at BIGINT NOT NULL,
                        buyer_uuid TEXT NOT NULL DEFAULT '',
                        buyer_dbid INTEGER NOT NULL DEFAULT 0,
                        purchased_at BIGINT NOT NULL DEFAULT 0,
                        status TEXT NOT NULL
                    );
                    """);
        }
        return connection;
    }

    private static void seed(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO claimSaleListings
                (id, world, area_id, owner_uuid, owner_dbid, price, listed_at, status)
                VALUES (1, 'world', 42, 'owner-1', 7, 1000, 1000, 'ACTIVE'),
                       (2, 'other-world', 99, 'owner-2', 8, 500, 2000, 'ACTIVE'),
                       (3, 'world', 43, 'owner-3', 9, 700, 3000, 'ACTIVE'),
                       (4, 'world', 44, 'owner-4', 10, 900, 4000, 'SOLD');
                """)) {
            statement.executeUpdate();
        }
    }
}
