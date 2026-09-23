package de.omegazirkel.risingworld.landclaim.db;

import static org.junit.Assert.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import org.junit.Test;

public class PlayerLeaseServiceTest {
    @Test public void offerAssignmentAndRentCreditAreWorldScoped() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            PlayerLeaseService world = new PlayerLeaseService(connection, "world");
            PlayerLeaseService other = new PlayerLeaseService(connection, "other");
            assertTrue(world.offer(7, "landlord", 1, 100, 10, true));
            assertTrue(world.assign(7, "tenant", 2, "2026-09-20"));
            assertTrue(world.recordRent(7, 10, "2026-09-21"));
            PlayerLeaseRecord lease = world.find(7).orElseThrow();
            assertTrue(lease.occupied()); assertEquals(10, lease.rentCredit());
            assertFalse(other.find(7).isPresent());
        }
    }

    @Test public void permissionSnapshotIsWorldScopedAndCanBeRemoved() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            PlayerLeaseService world = new PlayerLeaseService(connection, "world");
            PlayerLeaseService other = new PlayerLeaseService(connection, "other");
            assertTrue(world.savePermissionSnapshot(7, Map.of(1, "ozlc-owner", 3, "ozlc-friend")));
            assertEquals("ozlc-owner", world.permissionSnapshot(7).get(1));
            assertEquals("ozlc-friend", world.permissionSnapshot(7).get(3));
            assertTrue(other.permissionSnapshot(7).isEmpty());
            assertTrue(world.removePermissionSnapshot(7));
            assertTrue(world.permissionSnapshot(7).isEmpty());
        }
    }
}
