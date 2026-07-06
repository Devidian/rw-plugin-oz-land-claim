package de.omegazirkel.risingworld.landclaim.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import org.junit.Test;

public class RenewZoneConfigServiceTest {

    @Test
    public void savesAndUpdatesRenewZoneConfig() throws Exception {
        try (Connection connection = database()) {
            RenewZoneConfigService service = new RenewZoneConfigService(connection, "world");

            RenewZoneConfig created = service.save(42L, 12, 1000L);
            RenewZoneConfig updated = service.save(42L, 24, 2000L);

            assertEquals(42L, created.areaId());
            assertEquals(12, created.intervalHours());
            assertEquals(1000L, created.lastResetAt());
            assertEquals(24, updated.intervalHours());
            assertEquals(2000L, updated.lastResetAt());
            assertEquals(1, service.list().size());
        }
    }

    @Test
    public void listsDueRenewZones() throws Exception {
        try (Connection connection = database()) {
            RenewZoneConfigService service = new RenewZoneConfigService(connection, "world");
            service.save(1L, 1, 0L);
            service.save(2L, 2, 1L);
            service.save(3L, 2, 7_200_001L);

            List<RenewZoneConfig> due = service.dueAt(7_200_001L);

            assertEquals(2, due.size());
            assertEquals(1L, due.get(0).areaId());
            assertEquals(2L, due.get(1).areaId());
        }
    }

    @Test
    public void isolatesConfigsByWorld() throws Exception {
        try (Connection connection = database()) {
            RenewZoneConfigService world = new RenewZoneConfigService(connection, "world");
            RenewZoneConfigService otherWorld = new RenewZoneConfigService(connection, "other-world");

            world.save(42L, 12, 1000L);
            otherWorld.save(42L, 48, 4000L);

            assertEquals(12, world.find(42L).orElseThrow().intervalHours());
            assertEquals(48, otherWorld.find(42L).orElseThrow().intervalHours());
            assertTrue(world.delete(42L));
            assertFalse(world.find(42L).isPresent());
            assertTrue(otherWorld.find(42L).isPresent());
        }
    }

    private static Connection database() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite::memory:");
    }
}
