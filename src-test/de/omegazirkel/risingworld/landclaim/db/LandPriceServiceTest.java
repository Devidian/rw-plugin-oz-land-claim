package de.omegazirkel.risingworld.landclaim.db;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

import net.risingworld.api.utils.Vector3i;

public class LandPriceServiceTest {
    @Test
    public void countsAllTwentySixNeighborDirectionsInOneCluster() {
        Set<Vector3i> occupied = Set.of(
                new Vector3i(-1, -1, -1),
                new Vector3i(-1, 0, 0),
                new Vector3i(-1, 1, 1));

        assertEquals(3L, LandPriceService.adjacentClusterSizeSum(new Vector3i(0, 0, 0), occupied));
        assertEquals(1150L, LandPriceService.calculatePrice(1000, 0.05d, 3));
    }

    @Test
    public void addsDistinctAdjacentClusterSizes() {
        Set<Vector3i> occupied = Set.of(
                new Vector3i(-1, 0, 0), new Vector3i(-2, 0, 0),
                new Vector3i(1, 0, 0), new Vector3i(2, 0, 0), new Vector3i(3, 0, 0));

        assertEquals(5L, LandPriceService.adjacentClusterSizeSum(new Vector3i(0, 0, 0), occupied));
        assertEquals(1250L, LandPriceService.calculatePrice(1000, 0.05d, 5));
    }

    @Test
    public void capsUnsafePrices() {
        assertEquals(LandPriceService.MAX_SAFE_INTEGER,
                LandPriceService.calculatePrice(Long.MAX_VALUE, 1d, Long.MAX_VALUE));
    }
}
