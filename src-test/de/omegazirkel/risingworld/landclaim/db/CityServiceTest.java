package de.omegazirkel.risingworld.landclaim.db;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.risingworld.api.utils.Vector3i;

public class CityServiceTest {
    @Test
    public void membershipUsesAllThreeAxes() {
        CityRecord city = new CityRecord(1, "world", "City", new Vector3i(8, 20, 8), 5, 1L, null, null);
        assertTrue(CityService.contains(city, new Vector3i(3, 15, 13)));
        assertFalse(CityService.contains(city, new Vector3i(8, 26, 8)));
    }

    @Test
    public void radiusMustRemainInsideHorizontalSector() {
        assertTrue(CityService.withinSector(new Vector3i(109, 100, 33), 5));
        assertFalse(CityService.withinSector(new Vector3i(1, 100, 33), 5));
        assertFalse(CityService.withinSector(new Vector3i(109, 100, 255), 1));
    }

    @Test
    public void singleChunkComparisonUsesCoordinates() {
        assertTrue(CityService.sameChunk(new Vector3i(4, 7, -2), new Vector3i(4, 7, -2)));
        assertFalse(CityService.sameChunk(new Vector3i(4, 7, -2), new Vector3i(4, 8, -2)));
    }
}
