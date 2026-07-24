package de.omegazirkel.risingworld.landclaim;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.risingworld.api.utils.Vector3i;

public class AreaFrameVisibilityTest {

    private static final Vector3i PLAYER_CHUNK = new Vector3i(0, 0, 0);

    @Test
    public void includesAreasTouchingEachRadiusBoundary() {
        assertVisible(chunk(-15, 0, 0), chunk(-15, 0, 0), 15);
        assertVisible(chunk(15, 0, 0), chunk(15, 0, 0), 15);
        assertVisible(chunk(0, -15, 0), chunk(0, -15, 0), 15);
        assertVisible(chunk(0, 15, 0), chunk(0, 15, 0), 15);
        assertVisible(chunk(0, 0, -15), chunk(0, 0, -15), 15);
        assertVisible(chunk(0, 0, 15), chunk(0, 0, 15), 15);
    }

    @Test
    public void excludesAreasOneChunkBeyondEachRadiusBoundary() {
        assertHidden(chunk(-16, 0, 0), chunk(-16, 0, 0), 15);
        assertHidden(chunk(16, 0, 0), chunk(16, 0, 0), 15);
        assertHidden(chunk(0, -16, 0), chunk(0, -16, 0), 15);
        assertHidden(chunk(0, 16, 0), chunk(0, 16, 0), 15);
        assertHidden(chunk(0, 0, -16), chunk(0, 0, -16), 15);
        assertHidden(chunk(0, 0, 16), chunk(0, 0, 16), 15);
    }

    @Test
    public void includesTheWholeAreaWhenOnlyOneEdgeIntersects() {
        assertVisible(chunk(15, 50, 50), chunk(60, -50, -50), 15);
    }

    @Test
    public void normalizesReversedAreaBoundsAndNegativeCoordinates() {
        Vector3i player = chunk(-20, -3, -40);

        assertTrue(AreaFrameVisibility.intersectsChunkRadius(
                player,
                chunk(-2, 4, -24),
                chunk(-35, -10, -55),
                15));
    }

    @Test
    public void failsOpenWhenChunkCoordinatesAreUnavailable() {
        assertTrue(AreaFrameVisibility.intersectsChunkRadius(null, chunk(0, 0, 0), chunk(0, 0, 0), 15));
        assertTrue(AreaFrameVisibility.intersectsChunkRadius(PLAYER_CHUNK, null, chunk(0, 0, 0), 15));
        assertTrue(AreaFrameVisibility.intersectsChunkRadius(PLAYER_CHUNK, chunk(0, 0, 0), null, 15));
    }

    private static void assertVisible(Vector3i start, Vector3i end, int radius) {
        assertTrue(AreaFrameVisibility.intersectsChunkRadius(PLAYER_CHUNK, start, end, radius));
    }

    private static void assertHidden(Vector3i start, Vector3i end, int radius) {
        assertFalse(AreaFrameVisibility.intersectsChunkRadius(PLAYER_CHUNK, start, end, radius));
    }

    private static Vector3i chunk(int x, int y, int z) {
        return new Vector3i(x, y, z);
    }
}
