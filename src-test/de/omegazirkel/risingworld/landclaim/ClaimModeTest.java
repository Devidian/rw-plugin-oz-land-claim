package de.omegazirkel.risingworld.landclaim;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ClaimModeTest {
    @Test
    public void defaultsUnknownValuesToTimeBased() {
        assertEquals(ClaimMode.TIME_BASED, ClaimMode.parse(null));
        assertEquals(ClaimMode.TIME_BASED, ClaimMode.parse("future-mode"));
    }

    @Test
    public void acceptsConfiguredAndGermanAliases() {
        assertEquals(ClaimMode.ADMINISTRATIVE, ClaimMode.parse("ADMINISTRATIVE"));
        assertEquals(ClaimMode.LAND_PRICING, ClaimMode.parse("Grundstückspreise"));
        assertEquals(ClaimMode.CITY, ClaimMode.parse("Stadtmodus"));
    }

    @Test
    public void expansionClaimTimeIncludesTheExistingAreaAtItsOriginalClaimIndex() {
        assertEquals(1_260L, ChunkClaimUtil.claimTimeForClaimRange(600, 1.1, 0.1, 2.5, 0, 2));
    }

    @Test
    public void increasingClaimTimeStopsAtTheMaximum() {
        assertEquals(1_500L, ChunkClaimUtil.claimTimeForClaimRange(600, 1.01, 0.1, 2.5, 300, 1));
        assertEquals(3_000L, ChunkClaimUtil.claimTimeForClaimRange(600, 1.01, 0.1, 2.5, 300, 2));
    }

    @Test
    public void decreasingClaimTimeStopsAtTheMinimum() {
        assertEquals(60L, ChunkClaimUtil.claimTimeForClaimRange(600, 0.99, 0.1, 2.5, 300, 1));
        assertEquals(120L, ChunkClaimUtil.claimTimeForClaimRange(600, 0.99, 0.1, 2.5, 300, 2));
    }

    @Test
    public void customBoundsApplyToBothScalingDirections() {
        assertEquals(900L, ChunkClaimUtil.claimTimeForClaimRange(600, 2.0, 0.5, 1.5, 1, 1));
        assertEquals(300L, ChunkClaimUtil.claimTimeForClaimRange(600, 0.5, 0.5, 1.5, 2, 1));
    }

    @Test
    public void expansionRangeClampsEachChunkAtItsOwnClaimIndex() {
        assertEquals(3_300L, ChunkClaimUtil.claimTimeForClaimRange(600, 2.0, 0.1, 2.5, 0, 3));
    }
}
