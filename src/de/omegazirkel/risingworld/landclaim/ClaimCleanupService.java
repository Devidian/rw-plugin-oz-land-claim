package de.omegazirkel.risingworld.landclaim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.db.LandClaimChunkService;
import de.omegazirkel.risingworld.landclaim.db.entities.LandClaimChunkInfo;
import de.omegazirkel.risingworld.tools.PlayerDatabaseHelper;
import de.omegazirkel.risingworld.tools.PlayerDatabaseHelper.PlayerRecord;
import net.risingworld.api.Server;
import net.risingworld.api.World;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;
import net.risingworld.api.utils.Vector3i;

public class ClaimCleanupService {

    private static final float TELEPORT_Y_SAFETY_OFFSET = 2f;

    private final LandClaimChunkService claimService;
    private final PluginSettings settings;

    public ClaimCleanupService(LandClaimChunkService claimService, PluginSettings settings) {
        this.claimService = claimService;
        this.settings = settings;
    }

    public List<OwnerSummary> getOwnerSummaries() {
        List<LandClaimChunkInfo> claimed = claimService.getChunkInfoListClaimed();
        Map<String, List<LandClaimChunkInfo>> byOwner = new HashMap<>();
        Set<Integer> ownerDbIds = new HashSet<>();
        for (LandClaimChunkInfo info : claimed) {
            byOwner.computeIfAbsent(info.playerUID, key -> new ArrayList<>()).add(info);
            if (info.playerDBID != null && info.playerDBID > 0) {
                ownerDbIds.add(info.playerDBID);
            }
        }

        Map<Integer, PlayerRecord> playerRecords = PlayerDatabaseHelper.findPlayersByDbIds(
                LandClaim.getInstance(),
                ownerDbIds);
        List<OwnerSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<LandClaimChunkInfo>> entry : byOwner.entrySet()) {
            summaries.add(buildOwnerSummary(entry.getKey(), entry.getValue(), playerRecords));
        }
        summaries.sort(Comparator.comparingInt(OwnerSummary::claimCount).reversed()
                .thenComparing(OwnerSummary::ownerName, String.CASE_INSENSITIVE_ORDER));
        return summaries;
    }

    public List<AreaSummary> getAreaSummaries() {
        List<LandClaimChunkInfo> claimed = claimService.getChunkInfoListClaimed();
        Map<Long, List<LandClaimChunkInfo>> byArea = new LinkedHashMap<>();
        Set<Integer> ownerDbIds = new HashSet<>();
        for (LandClaimChunkInfo info : claimed) {
            if (info.areaID > 0) {
                byArea.computeIfAbsent(info.areaID, key -> new ArrayList<>()).add(info);
            }
            if (info.playerDBID != null && info.playerDBID > 0) {
                ownerDbIds.add(info.playerDBID);
            }
        }

        Map<Integer, PlayerRecord> playerRecords = PlayerDatabaseHelper.findPlayersByDbIds(
                LandClaim.getInstance(),
                ownerDbIds);
        List<AreaSummary> summaries = new ArrayList<>();
        for (Map.Entry<Long, List<LandClaimChunkInfo>> entry : byArea.entrySet()) {
            summaries.add(buildAreaSummary(entry.getKey(), entry.getValue(), playerRecords));
        }
        summaries.sort(Comparator.comparingLong(AreaSummary::inactiveDays).reversed()
                .thenComparing(AreaSummary::areaName, String.CASE_INSENSITIVE_ORDER));
        return summaries;
    }

    public List<SpecialAreaSummary> getSpecialAreaSummaries() {
        List<SpecialAreaSummary> summaries = new ArrayList<>();
        Area[] areas = Server.getAllAreas();
        if (areas == null) {
            return summaries;
        }
        for (Area area : areas) {
            if (isSpecialArea(area)) {
                summaries.add(new SpecialAreaSummary(
                        area.getID(),
                        areaName(area),
                        ChunkClaimUtil.areaToChunks(area).size(),
                        "System"));
            }
        }
        summaries.sort(Comparator.comparing(SpecialAreaSummary::areaName, String.CASE_INSENSITIVE_ORDER));
        return summaries;
    }

    public CleanupResult deleteOwner(String ownerUid) {
        return removeClaims(claimService.getClaimedChunkInfoListByPlayer(ownerUid), false);
    }

    public CleanupResult cleanupOwner(String ownerUid) {
        return removeClaims(claimService.getClaimedChunkInfoListByPlayer(ownerUid), true);
    }

    public CleanupResult deleteArea(long areaId) {
        return removeClaims(claimService.getChunkInfoListByArea(areaId), false);
    }

    public CleanupResult cleanupArea(long areaId) {
        return removeClaims(claimService.getChunkInfoListByArea(areaId), true);
    }

    public CleanupResult deleteSpecialArea(long areaId) {
        Area area = Server.getArea(areaId);
        if (area == null || !isSpecialArea(area)) {
            return CleanupResult.ok(0, 0);
        }
        Server.removeArea(area);
        return CleanupResult.ok(1, 0);
    }

    public CleanupResult cleanupSpecialArea(long areaId) {
        Area area = Server.getArea(areaId);
        if (area == null || !isSpecialArea(area)) {
            return CleanupResult.ok(0, 0);
        }
        Set<String> resetColumns = new HashSet<>();
        for (Vector3i chunk : ChunkClaimUtil.areaToChunks(area)) {
            resetColumns.add(chunk.x + ":" + chunk.z);
        }
        Server.removeArea(area);
        int resetCount = 0;
        for (String key : resetColumns) {
            String[] parts = key.split(":");
            if (parts.length == 2 && World.resetChunk(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]))) {
                resetCount++;
            }
        }
        return CleanupResult.ok(1, resetCount);
    }

    public AutoRemovalResult removeInactiveOwners(int inactiveDays) {
        long cutoffEpochSeconds = nowEpochSeconds() - (inactiveDays * 86400L);
        int ownersRemoved = 0;
        int claimsRemoved = 0;
        for (OwnerSummary owner : getOwnerSummaries()) {
            if (owner.lastSeenEpochSeconds() > 0 && owner.lastSeenEpochSeconds() <= cutoffEpochSeconds) {
                CleanupResult result = deleteOwner(owner.ownerUid());
                if (result.success()) {
                    ownersRemoved++;
                    claimsRemoved += result.claimsAffected();
                }
            }
        }
        return new AutoRemovalResult(ownersRemoved, claimsRemoved, inactiveDays);
    }

    public boolean teleportToArea(Player player, long areaId) {
        Area area = Server.getArea(areaId);
        List<LandClaimChunkInfo> infos = claimService.getChunkInfoListByArea(areaId);
        Vector3f target = area != null ? areaCenter(area) : centerOfFirstChunk(infos);
        if (target == null) {
            return false;
        }
        player.setPosition(target.x, target.y, target.z);
        return true;
    }

    private CleanupResult removeClaims(List<LandClaimChunkInfo> infos, boolean resetChunks) {
        List<LandClaimChunkInfo> claimed = infos.stream().filter(LandClaimChunkInfo::isClaimed).toList();
        if (claimed.isEmpty()) {
            return CleanupResult.ok(0, 0);
        }
        if (resetChunks) {
            Vector3i conflict = findSameColumnOtherOwnerConflict(claimed);
            if (conflict != null) {
                return CleanupResult.blocked(conflict);
            }
        }

        Set<Long> areaIds = new HashSet<>();
        Set<String> resetColumns = new HashSet<>();
        for (LandClaimChunkInfo info : claimed) {
            if (info.areaID > 0) {
                areaIds.add(info.areaID);
            }
            if (resetChunks) {
                resetColumns.add(info.chunkPos.x + ":" + info.chunkPos.z);
            }
            claimService.removeChunkClaim(info.playerUID, info.playerDBID, info.chunkPos);
        }
        for (Long areaId : areaIds) {
            Area area = Server.getArea(areaId);
            if (area != null) {
                Server.removeArea(area);
            }
        }
        int resetCount = 0;
        if (resetChunks) {
            for (String key : resetColumns) {
                String[] parts = key.split(":");
                if (parts.length == 2 && World.resetChunk(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]))) {
                    resetCount++;
                }
            }
        }
        return CleanupResult.ok(claimed.size(), resetCount);
    }

    private Vector3i findSameColumnOtherOwnerConflict(List<LandClaimChunkInfo> targets) {
        List<LandClaimChunkInfo> allClaimed = claimService.getChunkInfoListClaimed();
        for (LandClaimChunkInfo target : targets) {
            for (LandClaimChunkInfo other : allClaimed) {
                if (!other.isClaimed() || target == other) {
                    continue;
                }
                if (target.chunkPos.x == other.chunkPos.x
                        && target.chunkPos.z == other.chunkPos.z
                        && target.chunkPos.y != other.chunkPos.y
                        && !target.playerUID.equals(other.playerUID)) {
                    return other.chunkPos;
                }
            }
        }
        return null;
    }

    private OwnerSummary buildOwnerSummary(
            String ownerUid,
            List<LandClaimChunkInfo> infos,
            Map<Integer, PlayerRecord> playerRecords) {
        LandClaimChunkInfo first = infos.get(0);
        Player onlinePlayer = Server.getPlayerByUID(ownerUid);
        PlayerRecord record = playerRecords.get(first.playerDBID);
        String ownerName = onlinePlayer != null ? onlinePlayer.getName() : lastKnownName(first, record);
        long lastSeen = onlinePlayer != null ? nowEpochSeconds() : bestLastSeenEpochSeconds(infos, record);
        long maxClaims = onlinePlayer != null ? exactMaxClaims(onlinePlayer) : bestEffortMaxClaims(record, ownerUid);
        return new OwnerSummary(ownerUid, first.playerDBID == null ? 0 : first.playerDBID, ownerName, infos.size(), maxClaims, lastSeen,
                inactiveDays(lastSeen));
    }

    private AreaSummary buildAreaSummary(
            long areaId,
            List<LandClaimChunkInfo> infos,
            Map<Integer, PlayerRecord> playerRecords) {
        LandClaimChunkInfo first = infos.get(0);
        Area area = Server.getArea(areaId);
        PlayerRecord record = playerRecords.get(first.playerDBID);
        String ownerName = lastKnownName(first, record);
        long lastSeen = bestLastSeenEpochSeconds(infos, record);
        String areaName = area == null || area.getName() == null ? "Area #" + areaId : area.getName();
        return new AreaSummary(areaId, areaName, infos.size(), first.playerUID, first.playerDBID == null ? 0 : first.playerDBID, ownerName, lastSeen,
                inactiveDays(lastSeen));
    }

    private boolean isSpecialArea(Area area) {
        return area != null && !settings.defaultAreaPermission.equals(area.getDefaultPermission());
    }

    private String areaName(Area area) {
        return area.getName() == null || area.getName().isBlank() ? "Area #" + area.getID() : area.getName();
    }

    private String lastKnownName(LandClaimChunkInfo info, PlayerRecord record) {
        if (record != null && record.name != null && !record.name.isBlank()) {
            return record.name;
        }
        String name = Server.getLastKnownPlayerName(info.playerDBID);
        return name == null || name.isBlank() ? info.playerUID : name;
    }

    private long bestLastSeenEpochSeconds(List<LandClaimChunkInfo> infos, PlayerRecord record) {
        if (record != null && record.lastSeenEpochSeconds > 0) {
            return record.lastSeenEpochSeconds;
        }
        long maxMs = 0L;
        for (LandClaimChunkInfo info : infos) {
            maxMs = Math.max(maxMs, info.lastSeenMs);
        }
        return maxMs / 1000L;
    }

    private long exactMaxClaims(Player player) {
        long hours = player.getTotalPlayTime() / 3600L;
        return settings.basicClaimLimit + (long) (hours * settings.playTimeHoursExtraClaimFactor)
                + purchasedCapacity(player.getUID());
    }

    private long bestEffortMaxClaims(PlayerRecord record, String ownerUid) {
        if (record == null || record.totalPlayTimeSeconds <= 0) {
            return settings.basicClaimLimit + purchasedCapacity(ownerUid);
        }
        long hours = record.totalPlayTimeSeconds / 3600L;
        return settings.basicClaimLimit + (long) (hours * settings.playTimeHoursExtraClaimFactor)
                + purchasedCapacity(ownerUid);
    }

    private int purchasedCapacity(String ownerUid) {
        return LandClaim.extraClaimCapacityService() == null ? 0
                : LandClaim.extraClaimCapacityService().getPurchasedCapacity(ownerUid);
    }

    private long inactiveDays(long lastSeenEpochSeconds) {
        if (lastSeenEpochSeconds <= 0) {
            return 0L;
        }
        return Math.max(0L, (nowEpochSeconds() - lastSeenEpochSeconds) / 86400L);
    }

    private long nowEpochSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    private Vector3f areaCenter(Area area) {
        Vector3f start = area.getStartPosition();
        Vector3f end = area.getEndPosition();
        return new Vector3f(
                (start.x + end.x) / 2f,
                Math.min(start.y, end.y) + TELEPORT_Y_SAFETY_OFFSET,
                (start.z + end.z) / 2f);
    }

    private Vector3f centerOfFirstChunk(List<LandClaimChunkInfo> infos) {
        if (infos == null || infos.isEmpty()) {
            return null;
        }
        Vector3i chunk = infos.get(0).chunkPos;
        return new Vector3f(
                chunk.x * 32f + 16f,
                chunk.y * 32f + TELEPORT_Y_SAFETY_OFFSET,
                chunk.z * 32f + 16f);
    }

    public record OwnerSummary(String ownerUid, int ownerDbId, String ownerName, int claimCount, long maxClaims,
            long lastSeenEpochSeconds, long inactiveDays) {
    }

    public record AreaSummary(long areaId, String areaName, int chunkCount, String ownerUid, int ownerDbId,
            String ownerName, long lastSeenEpochSeconds, long inactiveDays) {
    }

    public record SpecialAreaSummary(long areaId, String areaName, int chunkCount, String ownerName) {
    }

    public record CleanupResult(boolean success, int claimsAffected, int chunksReset, Vector3i conflictChunk) {
        public static CleanupResult ok(int claimsAffected, int chunksReset) {
            return new CleanupResult(true, claimsAffected, chunksReset, null);
        }

        public static CleanupResult blocked(Vector3i conflictChunk) {
            return new CleanupResult(false, 0, 0, conflictChunk);
        }
    }

    public record AutoRemovalResult(int ownersRemoved, int claimsRemoved, int inactiveDays) {
    }
}
