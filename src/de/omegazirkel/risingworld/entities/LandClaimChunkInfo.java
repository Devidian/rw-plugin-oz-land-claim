package de.omegazirkel.risingworld.entities;

import net.risingworld.api.utils.Vector3i;

public class LandClaimChunkInfo {
    public final String playerUID;
    public final Integer playerDBID;
    public final String world;
    public final Vector3i chunkPos;

    public final long totalTimeMs;
    public final long lastSeenMs;
    public final long claimedAtMs;
    // for area attributes if claimed
    public final long areaID;
    public final int price;

    public LandClaimChunkInfo(
            Vector3i chunkPos, long timeSpent,
            long lastSeen, long claimedAt,
            String playerUID, String world,
            int price, long areaID, Integer playerDBID) {
        this.chunkPos = chunkPos;
        this.totalTimeMs = timeSpent;
        this.lastSeenMs = lastSeen;
        this.claimedAtMs = claimedAt;
        this.playerUID = playerUID;
        this.world = world;
        this.price = price;
        this.areaID = areaID;
        this.playerDBID = playerDBID;
    }

    // Utils

    public boolean isClaimed() {
        return claimedAtMs > 0;
    }

    public boolean isOwnedBy(Integer playerDBID){
        return this.playerDBID.equals(playerDBID);
    }

    public boolean isOwnedBy(String playerUID){
        return this.playerUID.equals(playerUID);
    }

    public Vector3i[] getNeighbourChunks() {
        Vector3i[] neighbours = new Vector3i[6];
        neighbours[0] = new Vector3i(chunkPos.x - 1, chunkPos.y, chunkPos.z);
        neighbours[1] = new Vector3i(chunkPos.x + 1, chunkPos.y, chunkPos.z);
        neighbours[2] = new Vector3i(chunkPos.x, chunkPos.y - 1, chunkPos.z);
        neighbours[3] = new Vector3i(chunkPos.x, chunkPos.y + 1, chunkPos.z);
        neighbours[4] = new Vector3i(chunkPos.x, chunkPos.y, chunkPos.z - 1);
        neighbours[5] = new Vector3i(chunkPos.x, chunkPos.y, chunkPos.z + 1);
        return neighbours;
    }

}
