package de.omegazirkel.risingworld.landclaim.exports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.Test;

import de.omegazirkel.risingworld.landclaim.db.PlayerMapVisitStore;

public class PlayerMapVisitExportServiceTest {

    @Test
    public void exportsCompactSectorsInStablePages() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            PlayerMapVisitStore store = new PlayerMapVisitStore(connection);
            store.recordVisit("player", 7, "world", -1, -1);
            store.recordVisit("player", 7, "world", 0, 0);

            PlayerMapVisitExportService service = new PlayerMapVisitExportService(connection);
            PlayerMapVisitsExportResponse first = service.export("player", "world", 0, 1);
            PlayerMapVisitsExportResponse second = service.export("player", "world", 1, 1);

            assertEquals(1, first.schemaVersion());
            assertTrue(first.hasMore());
            assertEquals(-1, first.sectors().get(0).sectorX());
            assertEquals(-1, first.sectors().get(0).sectorZ());
            assertEquals(10_924, first.sectors().get(0).bitmap().length());
            assertFalse(second.hasMore());
            assertEquals(0, second.sectors().get(0).sectorX());
            assertEquals(0, second.sectors().get(0).sectorZ());
        }
    }

    @Test
    public void initializesExistingChunkVisitsOnlyOnce() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE chunkData (player_uuid TEXT, world TEXT, chunk_x INTEGER, chunk_z INTEGER)");
                statement.execute("INSERT INTO chunkData VALUES ('player', 'world', -1, -1), ('player', 'world', 300, 1)");
            }
            PlayerMapVisitStore store = new PlayerMapVisitStore(connection);
            assertEquals(2, store.initializeFromChunkData("player", 7, "world"));
            assertEquals(0, store.initializeFromChunkData("player", 7, "world"));
            assertEquals(2, new PlayerMapVisitExportService(connection).export("player", "world", 0, 10).sectors().size());
        }
    }
}
