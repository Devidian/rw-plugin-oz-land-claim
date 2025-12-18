package de.omegazirkel.risingworld.interfaces;

import java.util.List;

import de.omegazirkel.risingworld.entities.LandClaimChunkInfo;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3i;

public interface ChunkDatabase {

    // Returns a list of all chunks of a player
    List<LandClaimChunkInfo> getChunkInfoListByPlayer(Player player);

    // Returns a list of all chunks that are claimed by anyone
    List<LandClaimChunkInfo> getChunkInfoListClaimed();

    // Returns a list of the same chunk of all players
    List<LandClaimChunkInfo> getChunkInfoListByChunk(Vector3i chunk);

    // Returns a list of all chunks of a specific area
    List<LandClaimChunkInfo> getChunkInfoListByArea(long areaId);

    // Returns specific chunk info for a player
    LandClaimChunkInfo getChunkInfoForPlayer(Player player, Vector3i chunk);
    LandClaimChunkInfo getChunkInfoForPlayer(String playerId, Vector3i chunk);

    // Returns total weighted claim count of a player
    int getTotalClaimWeight(Player player);

    // Gets total time player has spent in a specific chunk
    long getTotalChunkTime(Player player, Vector3i chunk);

    // Saves chunk visitation time for player
    void saveChunkTime(Player player, Vector3i chunk, long milliseconds);

    // Saves a claim with timestamp
    void saveChunkClaim(Player player, Vector3i chunk, long claimedAt, long areaID);

    void saveChunkClaim(String playerId, Integer playerDBID, Vector3i chunk, long claimedAt, long areaID);

    void removeChunkClaim(Player player, Vector3i chunk);

    void removeChunkClaim(String playerId, Integer playerDBID, Vector3i chunk);
}
