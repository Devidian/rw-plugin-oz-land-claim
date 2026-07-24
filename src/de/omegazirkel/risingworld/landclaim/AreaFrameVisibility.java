package de.omegazirkel.risingworld.landclaim;

import net.risingworld.api.utils.Vector3i;

final class AreaFrameVisibility {

    private AreaFrameVisibility() {
    }

    static boolean intersectsChunkRadius(
            Vector3i playerChunk,
            Vector3i areaStartChunk,
            Vector3i areaEndChunk,
            int radius) {
        if (playerChunk == null || areaStartChunk == null || areaEndChunk == null) {
            return true;
        }

        long boundedRadius = Math.max(0, radius);
        long viewMinX = (long) playerChunk.x - boundedRadius;
        long viewMaxX = (long) playerChunk.x + boundedRadius;
        long viewMinY = (long) playerChunk.y - boundedRadius;
        long viewMaxY = (long) playerChunk.y + boundedRadius;
        long viewMinZ = (long) playerChunk.z - boundedRadius;
        long viewMaxZ = (long) playerChunk.z + boundedRadius;

        int areaMinX = Math.min(areaStartChunk.x, areaEndChunk.x);
        int areaMaxX = Math.max(areaStartChunk.x, areaEndChunk.x);
        int areaMinY = Math.min(areaStartChunk.y, areaEndChunk.y);
        int areaMaxY = Math.max(areaStartChunk.y, areaEndChunk.y);
        int areaMinZ = Math.min(areaStartChunk.z, areaEndChunk.z);
        int areaMaxZ = Math.max(areaStartChunk.z, areaEndChunk.z);

        return areaMaxX >= viewMinX
                && areaMinX <= viewMaxX
                && areaMaxY >= viewMinY
                && areaMinY <= viewMaxY
                && areaMaxZ >= viewMinZ
                && areaMinZ <= viewMaxZ;
    }
}
