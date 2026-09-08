package de.omegazirkel.risingworld.landclaim.exports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;

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
}
