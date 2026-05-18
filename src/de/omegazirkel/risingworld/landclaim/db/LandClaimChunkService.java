package de.omegazirkel.risingworld.landclaim.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.omegazirkel.risingworld.landclaim.db.entities.LandClaimChunkInfo;
import net.risingworld.api.World;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3i;

public class LandClaimChunkService {

    private final ConcurrentHashMap<String, Set<LandClaimChunkInfo>> byPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Vector3i, Set<LandClaimChunkInfo>> byChunk = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<LandClaimChunkInfo>> byArea = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> claimCountByPlayer = new ConcurrentHashMap<>();
    private final Set<LandClaimChunkInfo> claimedChunks = ConcurrentHashMap.newKeySet();

    private final String world = World.getName();
    private final LandClaimChunkStore store;

    public LandClaimChunkService(LandClaimChunkStore store) {
        this.store = store;
        rebuildIndexesFromStore();
    }

    public LandClaimChunkInfo get(String playerUuid, Vector3i chunk) {
        return store.get(new LandClaimChunkKey(playerUuid, world, chunk));
    }

    public LandClaimChunkInfo createIfAbsent(
            String playerUuid,
            int playerDbId,
            Vector3i chunk) {
        LandClaimChunkKey key = new LandClaimChunkKey(playerUuid, world, chunk);

        LandClaimChunkInfo existing = store.get(key);
        if (existing != null) {
            return existing;
        }

        LandClaimChunkInfo info = new LandClaimChunkInfo(
                chunk,
                0,
                System.currentTimeMillis(),
                0,
                playerUuid,
                world,
                0,
                0,
                playerDbId);

        store.put(key, info);
        index(info); // wichtig!
        return info;
    }

    public void markDirty(LandClaimChunkInfo info) {
        store.markDirty(info);
    }

    public List<LandClaimChunkInfo> getChunkInfoListByPlayer(Player player) {
        return getChunkInfoListByPlayer(player.getUID());
    }

    public LandClaimChunkInfo getChunkInfoForPlayer(Player player, Vector3i chunk) {
        return get(player.getUID(), chunk);
    }

    public int getTotalClaimWeight(Player player) {
        return getTotalClaimWeight(player.getUID());
    }

    public long getTotalChunkTime(Player player, Vector3i chunk) {
        return getTotalChunkTime(player.getUID(), chunk);
    }

    public List<LandClaimChunkInfo> getChunkInfoListByPlayer(String playerUuid) {
        Set<LandClaimChunkInfo> set = byPlayer.get(playerUuid);
        return set == null ? List.of() : new ArrayList<>(set);
    }

    public List<LandClaimChunkInfo> getChunkInfoListByChunk(Vector3i chunk) {
        Set<LandClaimChunkInfo> set = byChunk.get(chunk);
        return set == null ? List.of() : new ArrayList<>(set);
    }

    public int getTotalClaimWeight(String playerUuid) {
        AtomicInteger c = claimCountByPlayer.get(playerUuid);
        return c == null ? 0 : c.get();
    }

    public long getTotalChunkTime(String playerUuid, Vector3i chunk) {
        LandClaimChunkKey key = new LandClaimChunkKey(playerUuid, world, chunk);
        LandClaimChunkInfo info = store.get(key);
        return info == null ? 0L : info.totalTimeMs;
    }

    public List<LandClaimChunkInfo> getChunkInfoListClaimed() {
        return new ArrayList<>(claimedChunks);
    }

    public List<LandClaimChunkInfo> getClaimedChunkInfoListByPlayer(String playerUuid) {
        Set<LandClaimChunkInfo> set = byPlayer.get(playerUuid);
        if (set == null) {
            return List.of();
        }
        List<LandClaimChunkInfo> result = new ArrayList<>();
        for (LandClaimChunkInfo info : set) {
            if (info.isClaimed()) {
                result.add(info);
            }
        }
        return result;
    }

    public List<LandClaimChunkInfo> getChunkInfoListByArea(long areaId) {
        Set<LandClaimChunkInfo> set = byArea.get(areaId);
        return set == null ? List.of() : new ArrayList<>(set);
    }

    public void saveChunkTime(Player player, Vector3i chunk, long milliseconds) {
        LandClaimChunkKey key = new LandClaimChunkKey(player.getUID(), world, chunk);
        LandClaimChunkInfo info = store.get(key);

        if (info != null) {
            info.totalTimeMs += milliseconds;
            info.lastSeenMs = System.currentTimeMillis();
            markDirty(info);
        } else {
            // new entry
            info = new LandClaimChunkInfo(chunk, milliseconds, System.currentTimeMillis(), 0, player.getUID(), world, 0, 0, player.getDbID());
            store.put(key, info);
            index(info);
            markDirty(info);
        }
    }

    private void updateClaimIndexes(
            LandClaimChunkInfo info,
            long oldClaimedAt,
            long oldAreaId) {
        boolean wasClaimed = oldClaimedAt > 0;
        boolean isClaimed = info.claimedAtMs > 0;

        if (!wasClaimed && isClaimed) {
            claimedChunks.add(info);
            adjustClaimCount(info.playerUID, true);
        } else if (wasClaimed && !isClaimed) {
            claimedChunks.remove(info);
            adjustClaimCount(info.playerUID, false);
        } else if (isClaimed) {
            claimedChunks.add(info);
        }

        if (oldAreaId != info.areaID) {
            if (oldAreaId > 0) {
                Set<LandClaimChunkInfo> oldSet = byArea.get(oldAreaId);
                if (oldSet != null)
                    oldSet.remove(info);
            }

            if (info.areaID > 0) {
                byArea
                        .computeIfAbsent(info.areaID, k -> ConcurrentHashMap.newKeySet())
                        .add(info);
            }
        }
    }

    public void saveChunkClaim(String playerId, Integer playerDBID, Vector3i chunk, long claimedAt, long areaID) {
        LandClaimChunkKey key = new LandClaimChunkKey(playerId, world, chunk);
        LandClaimChunkInfo info = store.get(key);

        if (info == null && claimedAt <= 0) {
            return;
        }

        if (info == null) {
            info = new LandClaimChunkInfo(
                    chunk,
                    0,
                    System.currentTimeMillis(),
                    claimedAt,
                    playerId,
                    world,
                    0,
                    areaID,
                    playerDBID == null ? 0 : playerDBID);
            store.put(key, info);
            index(info);
            markDirty(info);
            return;
        }

        synchronized (info) {
            long oldClaimedAt = info.claimedAtMs;
            long oldAreaId = info.areaID;

            info.claimedAtMs = claimedAt;
            info.areaID = areaID;
            info.lastSeenMs = System.currentTimeMillis();

            updateClaimIndexes(info, oldClaimedAt, oldAreaId);
            markDirty(info);
        }
    }

    public void saveChunkClaim(Player player, Vector3i chunk, long claimedAt, long areaID) {
        String playerId = player.getUID();
        saveChunkClaim(playerId, player.getDbID(), chunk, claimedAt, areaID);
    }

    public void removeChunkClaim(String playerId, Integer playerDBID, Vector3i chunk) {
        saveChunkClaim(playerId, playerDBID, chunk, 0, 0);
    }

    public void removeChunkClaim(Player player, Vector3i chunk) {
        saveChunkClaim(player.getUID(), player.getDbID(), chunk, 0, 0);
    }

    public LandClaimChunkInfo upsert(LandClaimChunkInfo info) {
        LandClaimChunkKey key = new LandClaimChunkKey(info.playerUID, world, info.chunkPos);

        LandClaimChunkInfo existing = store.get(key);

        if (existing == null) {
            // new entry
            store.put(key, info);
            index(info);
            return info;
        }

        // update existing
        synchronized (existing) {
            long oldClaimedAt = existing.claimedAtMs;
            long oldAreaId = existing.areaID;

            existing.totalTimeMs += info.totalTimeMs;
            existing.lastSeenMs = info.lastSeenMs;
            existing.claimedAtMs = info.claimedAtMs;
            existing.areaID = info.areaID;
            existing.price = info.price;

            updateClaimIndexes(existing, oldClaimedAt, oldAreaId);
            markDirty(existing);
        }

        return existing;
    }

    public void rebuildIndexesFromStore() {
        store.clear();
        byPlayer.clear();
        byChunk.clear();
        byArea.clear();
        claimedChunks.clear();
        claimCountByPlayer.clear();
        store.loadAll();

        for (LandClaimChunkInfo info : store.values()) {
            index(info);
        }
    }

    private void index(LandClaimChunkInfo info) {

        byPlayer.computeIfAbsent(info.playerUID, k -> ConcurrentHashMap.newKeySet()).add(info);

        byChunk.computeIfAbsent(info.chunkPos, k -> ConcurrentHashMap.newKeySet()).add(info);

        if (info.claimedAtMs > 0) {
            claimedChunks.add(info);
            adjustClaimCount(info.playerUID, true);
        }
        if (info.areaID > 0) {
            byArea.computeIfAbsent(info.areaID, k -> ConcurrentHashMap.newKeySet()).add(info);
        }
    }

    private void adjustClaimCount(String playerUuid, boolean increment) {
        claimCountByPlayer
                .computeIfAbsent(playerUuid, k -> new AtomicInteger(0))
                .addAndGet(increment ? 1 : -1);
    }

}
