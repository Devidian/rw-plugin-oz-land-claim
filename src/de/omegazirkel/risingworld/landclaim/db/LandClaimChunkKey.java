package de.omegazirkel.risingworld.landclaim.db;

import java.util.Objects;

import net.risingworld.api.utils.Vector3i;

public final class LandClaimChunkKey {

    private final String playerUuid;
    private final String world;
    private final int x, y, z;

    private final int hash;

    public LandClaimChunkKey(String playerUuid, String world, Vector3i c) {
        this.playerUuid = playerUuid;
        this.world = world;
        this.x = c.x;
        this.y = c.y;
        this.z = c.z;

        // Hash einmal berechnen
        this.hash = Objects.hash(playerUuid, world, x, y, z);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof LandClaimChunkKey other))
            return false;
        return x == other.x && y == other.y && z == other.z
                && playerUuid.equals(other.playerUuid)
                && world.equals(other.world);
    }
}
