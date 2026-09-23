package de.omegazirkel.risingworld.landclaim.db;

import static org.junit.Assert.*;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.Test;

public class UnclaimedLeaseServiceTest {
    @Test public void persistsDailyCostsAndCreditWithinItsWorld() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            UnclaimedLeaseService world = new UnclaimedLeaseService(connection, "world");
            UnclaimedLeaseService other = new UnclaimedLeaseService(connection, "other");
            assertTrue(world.create(9, "tenant", 2, 1000, 25, 4, "2026-09-20"));
            assertTrue(world.recordRent(9, 25, "2026-09-21"));
            UnclaimedLeaseRecord lease = world.active().get(0);
            assertEquals(25, lease.rentCredit());
            assertEquals(4, lease.ancillaryCost());
            assertTrue(other.active().isEmpty());
            assertTrue(world.remove(9));
            assertTrue(world.active().isEmpty());
        }
    }
}
