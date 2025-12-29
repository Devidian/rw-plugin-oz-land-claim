package de.omegazirkel.risingworld.landclaim;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.entities.LandClaimChunkInfo;
import de.omegazirkel.risingworld.interfaces.ChunkDatabase;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.Server;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;
import net.risingworld.api.utils.Vector3i;

/**
 * Utility class for managing chunk entry/exit, claim permissions,
 * playtime-based limits and claim/expand logic.
 */
public class ChunkClaimUtil {

    private final ChunkDatabase db;
    private static final PluginSettings s = PluginSettings.getInstance();

    private static final I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    public static OZLogger logger() {
        return LandClaim.logger();
    }

    public ChunkClaimUtil(ChunkDatabase db) {
        this.db = db;
    }

    // ---------------------------------------------------------
    // Player chunk tracking
    // ---------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<Vector3i, Long> playerChunkTimes(Player p) {
        if (!p.hasAttribute("chunkTimes")) {
            p.setAttribute("chunkTimes", new ConcurrentHashMap<Vector3i, Long>());
        }
        return (Map<Vector3i, Long>) p.getAttribute("chunkTimes");
    }

    /**
     * Called when a player enters a chunk.
     */
    public void enterChunk(Player p, Vector3i chunk) {
        Map<Vector3i, Long> currentChunks = playerChunkTimes(p);

        currentChunks.put(chunk, System.currentTimeMillis());
    }

    /**
     * Called when a player leaves a chunk.
     */
    public void leaveChunk(Player p, Vector3i chunk) {
        Map<Vector3i, Long> currentChunks = playerChunkTimes(p);

        Long start = currentChunks.remove(chunk);

        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            // Save time spent in this chunk to DB
            db.saveChunkTime(p, chunk, duration);
        }
    }

    public void idleChunk(Player p) {
        Map<Vector3i, Long> currentChunks = playerChunkTimes(p);
        Vector3i chunk = p.getChunkPosition();

        Long start = currentChunks.remove(chunk);

        if (start != null) {
            // we use now for calculations and new start stop
            Long now = System.currentTimeMillis();
            long duration = now - start;
            // only save time if duration is at least 5 seconds
            if (duration > 5000) {
                currentChunks.put(chunk, now);
                db.saveChunkTime(p, chunk, duration);
            } else
                currentChunks.put(chunk, start);
        }
    }

    public long currentTimeinChunkMs(Player p, Vector3i chunk) {
        Map<Vector3i, Long> currentChunks = playerChunkTimes(p);
        Long start = currentChunks.get(chunk);
        if (start != null) {
            return System.currentTimeMillis() - start;
        }
        return 0;
    }

    // ---------------------------------------------------------
    // Player claim limits
    // ---------------------------------------------------------

    /**
     * Calculates how many claims a player is allowed to have,
     * based on total playtime.
     */
    public long getPlayerMaxClaims(Player p) {
        long playtimeSeconds = p.getTotalPlayTime(); // Rising World: in seconds
        long hours = playtimeSeconds / 3_600;

        return s.basicClaimLimit + (long) (hours * s.playTimeHoursExtraClaimFactor);
    }

    public long getPlayerNextClaimTime(Player p) {
        return getPlayerNextClaimTime(p, 1);
    }

    public long getPlayerNextClaimTime(Player p, Integer chunkCount) {
        Integer baseTimeSeconds = s.minutesToClaim * 60;
        long claimCount = getPlayerClaimCount(p);
        long timeToClaim = 0;
        for (int i = 0; i < chunkCount; i++) {
            timeToClaim += (long) (baseTimeSeconds * Math.pow(s.claimTimeScaleFactor, claimCount + i));
        }
        return timeToClaim;
    }

    /**
     * Returns weighted sum of player's claims (1 weight per chunk).
     */
    public int getPlayerClaimCount(Player p) {
        return db.getTotalClaimWeight(p);
    }

    public long playerTimeInChunkInMs(Player p, Vector3i chunk) {
        return db.getTotalChunkTime(p, chunk);
    }

    public long playerTimeInChunkInSeconds(Player p, Vector3i chunk) {
        return playerTimeInChunkInMs(p, chunk) / 1000;
    }

    public long playerTimeInChunkInMinutes(Player p, Vector3i chunk) {
        return playerTimeInChunkInSeconds(p, chunk) / 60;
    }

    // ---------------------------------------------------------
    // Claim checks
    // ---------------------------------------------------------

    /**
     * Determines if a player can claim this Area.
     * A claimable area must consist of only 1 chunk
     */
    public boolean canPlayerClaimArea(Player p, Area area, Callback<String> callback) {

        // 1. Check if player reached max claims
        long current = getPlayerClaimCount(p);
        long maxAllowed = getPlayerMaxClaims(p);

        if (!isSingleChunkArea(area)) {
            if (callback != null)
                callback.onCall(t().get("TC_CLAIM_ERROR_SIZE", p));
            return false;
        }

        // 2. Check area is unclaimed
        Area existing = isAreaIntersecting(area);
        if (existing != null) {
            if (callback != null)
                callback.onCall(t().get("TC_CLAIM_ERROR_OCCUPIED", p));
            return false;
        }

        // shortage for admins with all ignores
        if (p.isAdmin() && s.adminIgnoreLimit && s.adminIgnoreTime) {
            return true;
        }

        // 3. Check max claims reached
        if (current >= maxAllowed && !(p.isAdmin() && s.adminIgnoreLimit)) {
            if (callback != null)
                callback.onCall(t().get("TC_CLAIM_ERROR_MAX_REACHED", p));
            return false;
        }

        if (area != null) {
            // 4. Check minimum time spent in chunk
            long time = db.getTotalChunkTime(p, area.getStartChunkPosition()) / 1000;

            if (time < getPlayerNextClaimTime(p) && !(p.isAdmin() && s.adminIgnoreTime)) {
                if (callback != null)
                    callback.onCall(t().get("TC_CLAIM_ERROR_TIME", p));
                return false;
            }
        }

        return true;
    }

    // ---------------------------------------------------------
    // Area / Chunk utility
    // ---------------------------------------------------------

    /**
     * Converts a chunk coordinate into an Area
     */
    public static Area getVirtualAreaFromChunkVector(Vector3i chunkPosition) {
        return chunksToArea(List.of(chunkPosition));
    }

    /**
     * transform area into list of chunks
     * 
     * @param area
     * @return
     */
    public static List<Vector3i> areaToChunks(Area area) {
        if (area == null)
            return null;
        Vector3i start = area.getStartChunkPosition();
        Vector3i end = area.getEndChunkPosition();

        List<Vector3i> chunks = new java.util.ArrayList<>();
        for (int x = start.x; x <= end.x; x++) {
            for (int y = start.y; y <= end.y; y++) {
                for (int z = start.z; z <= end.z; z++) {
                    chunks.add(new Vector3i(x, y, z));
                }
            }
        }
        return chunks;
    }

    /**
     * transform list of chunks into area
     * 
     * @param chunks
     * @return
     */
    public static Area chunksToArea(List<Vector3i> chunks) {
        Vector3i start = chunks.get(0).copy();
        Vector3i end = chunks.get(0).copy();

        if (chunks.size() > 1)
            for (Vector3i chunk : chunks) {
                if (chunk.x < start.x)
                    start.x = chunk.x;
                if (chunk.y < start.y)
                    start.y = chunk.y;
                if (chunk.z < start.z)
                    start.z = chunk.z;
                if (chunk.x > end.x)
                    end.x = chunk.x;
                if (chunk.y > end.y)
                    end.y = chunk.y;
                if (chunk.z > end.z)
                    end.z = chunk.z;
            }
        Vector3f areaStart = new Vector3f(start.x * 32 + (start.x < 0 ? 31.99f : 0f), start.y * 64,
                start.z * 32 + (start.z < 0 ? 31.99f : 0f));
        Vector3f areaEnd = new Vector3f(end.x * 32 + (end.x < 0 ? 0.001f : 31.999f), end.y * 64 + 63.999f,
                end.z * 32 + (end.z < 0 ? 0.001f : 31.999f));
        Area area = new Area(areaStart, areaEnd);

        // if (chunks.size() > 1)
        // area.setName("New Multichunk Area");
        // else
        // area.setName("New Chunk Area @ " + start.toString());
        // area.setNameVisible(true);
        // area.setDefaultPermission(s.defaultAreaPermission);
        return area;
    }

    public Area isAreaIntersecting(Area area) {
        for (Area a : Server.getAllAreas()) {
            if (a != null && a.intersects(area)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Quick check if area is in only one chunk
     * 
     * @param area
     * @return
     */
    private boolean isSingleChunkArea(Area area) {
        if (area == null)
            return false;
        Vector3i start = area.getStartChunkPosition();
        Vector3i end = area.getEndChunkPosition();
        return start.x == end.x && start.z == end.z && start.y == end.y;
    }

    // ---------------------------------------------------------
    // Repair / sync / import areas
    // ---------------------------------------------------------

    public void syncAndRepairAreas() {
        // 1. check all claimed chunks if areas exist
        List<LandClaimChunkInfo> infoList = db.getChunkInfoListClaimed();

        if (!infoList.isEmpty())
            for (LandClaimChunkInfo info : infoList) {
                String playerUID = info.playerUID;
                Integer playerDBID = info.playerDBID;
                String playerName = Server.getLastKnownPlayerName(playerDBID);
                String areaId = info.chunkPos + " / " + info.playerUID;
                Area va = getVirtualAreaFromChunkVector(info.chunkPos);
                Area existing = isAreaIntersecting(va);
                String playerPermission = existing != null ? existing.getPlayerPermission(playerDBID) : null;

                va.setName("Recovered area of " + playerName);
                if (existing == null) {
                    // case 1: if area not exist create it
                    logger().warn("Serverarea for LCCI " + areaId + " does not exist, creating new");
                    Server.addArea(va, true);
                    va.setPlayerPermission(playerDBID, s.ownerAreaPermission);
                    va.setAttribute("ownerUID", playerUID);
                    va.setAttribute("ownerDBID", playerDBID);

                    db.saveChunkClaim(playerUID, playerDBID, info.chunkPos, info.claimedAtMs, va.getID());
                } else if (playerPermission != null && !playerPermission.equals(s.ownerAreaPermission)) {
                    // case 2: area exists but ownership is wrong
                    logger().warn("Player " + va.getName() + "is not owner of " + areaId);
                    // remove claim
                    db.saveChunkClaim(playerUID, playerDBID, info.chunkPos, 0, 0);
                } else if (info.areaID == 0) {
                    // case 3: areaID is missing
                    logger().warn("LCCI " + areaId + " is missing areaID");

                    // use existing to fix
                    db.saveChunkClaim(playerUID, playerDBID, info.chunkPos, info.claimedAtMs, existing.getID());
                }
            }

        // 2. check server-areas for ownership
        for (Area a : Server.getAllAreas()) {
            // get LandClaimChunkInfo for area
            List<Vector3i> chunks = areaToChunks(a);
            List<LandClaimChunkInfo> infoAreaList = db.getChunkInfoListByArea(a.getID());
            // get all permissions from area to check who is owner
            Map<Integer, String> permissions = a.getAllPlayerPermissions();
            if (permissions == null) {
                logger().warn("Area " + a.getName() + " has no permissions [skipping]");
                continue;
            }

            String defaultPermission = a.getDefaultPermission();
            if (defaultPermission == null || !defaultPermission.equals(s.defaultAreaPermission)) {
                // area might not be an LCCI area
                logger().warn(
                        "Area " + a.getName() + " is not an LCCI area, default permission is "
                                + defaultPermission.toString()
                                + " [skipping]");

                continue;
            }
            if (!permissions.containsValue(s.ownerAreaPermission)) {
                logger().warn(
                        "Area " + a.getName() + " has no " + s.ownerAreaPermission + " (owner) permissions [skipping]");
                continue;
            }
            // get userid of player with owner permission
            Integer ownerDbId = null;
            for (Map.Entry<Integer, String> entry : permissions.entrySet()) {
                if (entry.getValue().equals(s.ownerAreaPermission)) {
                    ownerDbId = entry.getKey();
                    break;
                }
            }
            String ownerName = Server.getLastKnownPlayerName(ownerDbId);
            String ownerUID = Server.getLastKnownPlayerUIDs(ownerName)[0];

            // check if all items have same playerUID
            Boolean mismatch = false;
            if (!infoAreaList.isEmpty())
                for (LandClaimChunkInfo info : infoAreaList) {
                    if (!info.playerDBID.equals(ownerDbId)) {
                        logger().warn(
                                "Area " + a.getName() + " owned by " + ownerName + ": found different owners");
                        mismatch = true;
                        break;
                    }
                }

            if (mismatch) {
                logger().warn("Area " + a.getName() + " not integer, cant sync with database");
                break;
            }
            if (chunks.size() != infoAreaList.size()) {
                logger().warn("Area " + a.getName() + " not in sync");
                for (Vector3i chunk : chunks) {
                    boolean found = false;
                    if (!infoAreaList.isEmpty())
                        for (LandClaimChunkInfo info : infoAreaList) {
                            if (info.chunkPos.equals(chunk)) {
                                found = true;
                                break;
                            }
                        }
                    if (!found) {
                        long claimedAt = System.currentTimeMillis();
                        if (!infoAreaList.isEmpty())
                            claimedAt = infoAreaList.get(0).claimedAtMs;
                        logger().warn("Area chunk " + chunk + " synced with database");
                        db.saveChunkClaim(ownerUID, ownerDbId, chunk, claimedAt, a.getID());
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------
    // Claim single chunk
    // ---------------------------------------------------------

    /**
     * Creates a new claim after all checks passed.
     */
    public Area claimArea(Player p, Area area) {
        return claimArea(p, area, s.defaultAreaPermission, p.getDbID());
    }

    public Area claimArea(Player p, Area area, String defaultPermission, Integer ownerDBId) {
        if (!canPlayerClaimArea(p, area, (t) -> p.sendTextMessage(t))) {
            return null;
        }
        long now = System.currentTimeMillis();
        area.setNameVisible(true);
        area.setDefaultPermission(defaultPermission);

        // only save owner if claim is a player-claim and not a special area
        if (ownerDBId != null && defaultPermission.equals(s.defaultAreaPermission)) {
            String ownerName = Server.getLastKnownPlayerName(ownerDBId);
            area.setName("Claimed by " + ownerName);
            db.saveChunkClaim(p, area.getStartChunkPosition(), now, area.getID());
        } else {
            area.setName(defaultPermission + " area");
        }
        // we must set name BEFORE addArea
        Server.addArea(area, true);
        // we must set player permission AFTER addArea (because before it has no id in
        // db??)
        if (ownerDBId != null && defaultPermission.equals(s.defaultAreaPermission)) {
            String ownerName = Server.getLastKnownPlayerName(ownerDBId);
            String ownerUID = Server.getLastKnownPlayerUIDs(ownerName)[0];
            area.setAttribute("ownerUID", ownerUID);
            area.setAttribute("ownerDBID", ownerDBId);
            area.setPlayerPermission(ownerDBId, s.ownerAreaPermission);
        }
        return area;
    }

    // ---------------------------------------------------------
    // Expansion
    // ---------------------------------------------------------

    private boolean helperCheckForClaim(Vector3i chunk, Player p) {
        List<LandClaimChunkInfo> infoList = db.getChunkInfoListByChunk(chunk);
        // check if chunk is claimed by another user
        for (LandClaimChunkInfo info : infoList) {
            if (info.isOwnedBy(p.getUID())) {
                continue;
            } else if (info.isClaimed()) {
                // TODO later: check area offline protection
                // checkPlayerOfflineProtection(info.playerDBID)
                // area is claimed by someone else
                p.sendTextMessage(t().get("TC_CLAIM_ERROR_CLAIMED", p)
                        .replace("PH_CHUNK_POS", info.chunkPos + "")
                        .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(info.playerDBID)));
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to expand an area in a direction. Checks ALL affected chunks.
     */
    public Area expandClaim(Area area, Direction dir, Player p) {
        if (area == null)
            return null;
        Vector3i startChunk = area.getStartChunkPosition();
        Vector3i endChunk = area.getEndChunkPosition();
        List<Vector3i> chunks = areaToChunks(area);
        List<Area> areasToRemove = new java.util.ArrayList<>();
        List<Vector3i> chunkInExtendedArea = new java.util.ArrayList<>();
        String defaultPermission = area.getDefaultPermission();

        switch (dir) {
            case NORTH: // check all chunks with max(z) if their neighbour +1z is claimable
                for (Vector3i chunk : chunks) {
                    // first add all current chunks to the extended area
                    chunkInExtendedArea.add(chunk);
                    if (chunk.z != endChunk.z) {
                        continue;
                    }
                    Vector3i c = new Vector3i(chunk.x, chunk.y, chunk.z + 1);
                    if (!helperCheckForClaim(c, p)) {
                        return null;
                    }
                    chunkInExtendedArea.add(c);
                }
                break;
            case SOUTH: // check all chunks with min(z) if their neighbour -1z is claimable
                for (Vector3i chunk : chunks) {
                    chunkInExtendedArea.add(chunk);
                    if (chunk.z != startChunk.z) {
                        continue;
                    }
                    Vector3i c = new Vector3i(chunk.x, chunk.y, chunk.z - 1);
                    if (!helperCheckForClaim(c, p)) {
                        return null;
                    }
                    chunkInExtendedArea.add(c);
                }
                break;
            case WEST: // check all chunks with max(x) if their neighbour -1x is claimable
                for (Vector3i chunk : chunks) {
                    chunkInExtendedArea.add(chunk);
                    if (chunk.x != startChunk.x) {
                        continue;
                    }
                    Vector3i c = new Vector3i(chunk.x - 1, chunk.y, chunk.z);
                    if (!helperCheckForClaim(c, p)) {
                        return null;
                    }
                    chunkInExtendedArea.add(c);
                }
                break;
            case EAST: // check all chunks with min(x) if their neighbour +1x is claimable
                for (Vector3i chunk : chunks) {
                    chunkInExtendedArea.add(chunk);
                    if (chunk.x != endChunk.x) {
                        continue;
                    }
                    Vector3i c = new Vector3i(chunk.x + 1, chunk.y, chunk.z);
                    if (!helperCheckForClaim(c, p)) {
                        return null;
                    }
                    chunkInExtendedArea.add(c);
                }
                break;
            case UP: // check all chunks with max(y) if their neighbour +1y is claimable
                for (Vector3i chunk : chunks) {
                    chunkInExtendedArea.add(chunk);
                    if (chunk.y != endChunk.y) {
                        continue;
                    }
                    Vector3i c = new Vector3i(chunk.x, chunk.y + 1, chunk.z);
                    if (!helperCheckForClaim(c, p)) {
                        return null;
                    }
                    chunkInExtendedArea.add(c);
                }
                break;
            case DOWN: // check all chunks with min(y) if their neighbour -1y is claimable
                for (Vector3i chunk : chunks) {
                    chunkInExtendedArea.add(chunk);
                    if (chunk.y != startChunk.y) {
                        continue;
                    }
                    Vector3i c = new Vector3i(chunk.x, chunk.y - 1, chunk.z);
                    if (!helperCheckForClaim(c, p)) {
                        return null;
                    }
                    chunkInExtendedArea.add(c);
                }
                break;
        }

        // determine if added chunks exceed claim limit
        Integer currentClaimCount = getPlayerClaimCount(p);
        Integer extendedAreaSize = chunkInExtendedArea.size();
        Integer areaSize = chunks.size();
        Integer extendedChunksCount = extendedAreaSize - areaSize;

        if (currentClaimCount + extendedChunksCount > getPlayerMaxClaims(p)
                && !(p.isAdmin() && s.adminIgnoreLimit)) {
            p.sendTextMessage(t().get("TC_CLAIM_ERROR_LIMIT", p).replace("PH_MAX_CLAIMS", getPlayerMaxClaims(p) + ""));
            return null;
        }

        // check chunks for combined areas and split if needed
        for (Vector3i chunk : chunkInExtendedArea) {
            List<LandClaimChunkInfo> infoList = db.getChunkInfoListByChunk(chunk);
            for (LandClaimChunkInfo info : infoList) {
                // if this chunk belongs to the area that should be expanded ignore loop
                if (info.areaID == area.getID())
                    break;
                // if we have an areaId check if it is single chunk only
                if (info.areaID != 0) {
                    Area a = Server.getArea(info.areaID);
                    if (isSingleChunkArea(a)) {
                        areasToRemove.add(a);
                        break;
                    } else {
                        // we need to split this area before proceeding
                        splitClaim(a, p);
                        a = isAreaIntersecting(getVirtualAreaFromChunkVector(chunk));
                        areasToRemove.add(a);
                        break;
                    }
                }
            }
        }

        // determine if in-chunk-time fits
        long timeToClaimNeeded = getPlayerNextClaimTime(p, extendedChunksCount);
        long sumTimeInChunks = 0;
        for (Vector3i chunk : chunkInExtendedArea) {
            sumTimeInChunks += playerTimeInChunkInSeconds(p, chunk);
        }

        if (sumTimeInChunks < timeToClaimNeeded && !(p.isAdmin() && s.adminIgnoreTime)) {
            // time needed not reached
            p.sendTextMessage(t().get("TC_CLAIM_ERROR_TIME", p)
                    .replace("PH_TIME_NEEDED", timeToClaimNeeded + "s")
                    .replace("PH_TIME_LEFT", (timeToClaimNeeded - sumTimeInChunks) + "s"));
            return null;
        }

        Area extendedArea = chunksToArea(chunkInExtendedArea);
        // if (p.isAdmin()) {
        // p.sendTextMessage("Debug extendedArea:\nchunks: "
        // + chunkInExtendedArea.size() + "\nstart: "
        // + extendedArea.getStartPosition() + "\nend: "
        // + extendedArea.getEndPosition() + "\nstartChunk: "
        // + extendedArea.getStartChunkPosition() + "\nendChunk: "
        // + extendedArea.getEndChunkPosition() + "\n");
        // return false;
        // }

        // we remove all areas before createing the new one
        Server.removeArea(area);
        for (Area a : areasToRemove) {
            Server.removeArea(a);
            // a.destroy();
        }

        // instead adding extended area we expand the original area, so permissions will
        // be the same
        // ERROR: java.lang.NullPointerException =>
        // net.risingworld.api.objects.Area.INFINITE is null
        // area.setStartPosition(extendedArea.getStartPosition());
        // area.setEndPosition(extendedArea.getEndPosition());
        // ERROR: java.lang.NullPointerException =>
        // net.risingworld.api.objects.Area.INFINITE is null
        // area.set(extendedArea.getStartPosition(), extendedArea.getEndPosition());
        // WORKAROUND: we need to create a new area to avoid null pointer exception
        extendedArea.setName(area.getName());
        extendedArea.setNameVisible(true);
        Server.addArea(extendedArea, true);
        if (defaultPermission.equals(s.defaultAreaPermission)) {
            extendedArea.setPlayerPermission(p.getDbID(), s.ownerAreaPermission);
            extendedArea.setAttribute("ownerUID", p.getUID());
            extendedArea.setAttribute("ownerDBID", p.getDbID());
        }
        extendedArea.setDefaultPermission(defaultPermission);

        // we need to transfer all permissions to the new area
        Map<Integer, String> originAreaPermissions = area.getAllPlayerPermissions();
        if (originAreaPermissions != null)
            for (Map.Entry<Integer, String> entry : originAreaPermissions.entrySet()) {
                extendedArea.setPlayerPermission(entry.getKey(), entry.getValue());
            }

        // last step: set claim status in database for all chunks
        for (Vector3i chunk : chunkInExtendedArea) {
            db.saveChunkClaim(p, chunk, System.currentTimeMillis(), extendedArea.getID());
        }

        // area.destroy();
        return extendedArea;
    }

    public enum Direction {
        NORTH, SOUTH, EAST, WEST, UP, DOWN
    }

    public void releaseArea(Player p, Area area) {
        String playerPermission = area.getPlayerPermission(p);
        Boolean isOwner = playerPermission != null && playerPermission.equals(s.ownerAreaPermission);
        if (isOwner || p.isAdmin()) {
            Map<Integer, String> permissionSet = area.getAllPlayerPermissions();
            // 1. remove all player permissions
            if (permissionSet != null)
                for (Map.Entry<Integer, String> entry : permissionSet.entrySet()) {
                    area.removePlayerPermission(entry.getKey());
                }
            // 2. remove area from server
            String areaName = area.getName() == null ? "Unnamed Area" : area.getName();
            Server.removeArea(area);

            p.sendTextMessage(t().get("TC_AREA_RELEASE_AREA", p).replace("PH_AREA_NAME", areaName));
            // remove area and claim information from database
            List<LandClaimChunkInfo> infoList = db.getChunkInfoListByArea(area.getID());
            for (LandClaimChunkInfo info : infoList) {
                Player owner = Server.getPlayerByUID(info.playerUID);
                p.sendTextMessage(t().get("TC_AREA_RELEASE_CHUNK", p)
                        .replace("PH_AREA_NAME", areaName)
                        .replace("PH_CHUNK_POS", info.chunkPos.toString())
                        .replace("PH_PLAYER_NAME", owner.getName()));
                db.saveChunkClaim(owner, info.chunkPos, 0, 0);
            }
            // Discord announcement
            String message = t().get("TC_DISCORD_AREA_RELEASED", DiscordConnect.botLang())
                    .replace("PH_AREA_NAME", areaName)
                    .replace("PH_CHUNK_POS", area.getStartChunkPosition().toString())
                    .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(p.getDbID()));
            DiscordConnect.sendDiscordReleaseAccouncement(message);
            // Server announcement
            for (Player player : Server.getAllPlayers()) {
                if (!player.equals(p))
                    player.sendYellMessage(
                            t().get("TC_ANNOUNCEMENT_AREA_RELEASED", player)
                                    .replace("PH_AREA_NAME", areaName)
                                    .replace("PH_CHUNK_POS", area.getStartChunkPosition().toString())
                                    .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(p.getDbID())),
                            5, true);
            }
            // destroy area as last step
            // area.destroy();
        }
    }

    /**
     * Split an area with chunk size > 1 into 1-chunk-sized areas.
     * 
     * @param existingArea
     * @param p
     * @return
     */
    public boolean splitClaim(Area existingArea, Player p) {
        String playerPermission = existingArea.getPlayerPermission(p);
        Boolean isOwner = playerPermission != null && playerPermission.equals(s.ownerAreaPermission);
        if (!isOwner && !p.isAdmin())
            return false;
        List<Vector3i> chunks = areaToChunks(existingArea);

        String defaultPermission = existingArea.getDefaultPermission();

        // Remove origin area
        Server.removeArea(existingArea);
        for (Vector3i chunk : chunks) {
            Area newArea = getVirtualAreaFromChunkVector(chunk);
            newArea.setName(existingArea.getName());
            newArea.setNameVisible(true);
            Server.addArea(newArea, true);
            // set permissions from existing area
            Integer ownerId = 0;
            Map<Integer, String> permissions = existingArea.getAllPlayerPermissions();
            if (permissions != null)
                for (Map.Entry<Integer, String> entry : permissions.entrySet()) {
                    newArea.setPlayerPermission(entry.getKey(), entry.getValue());
                    if (entry.getValue().equals(s.ownerAreaPermission)) {
                        ownerId = entry.getKey();
                    }
                }
            String ownerName = Server.getLastKnownPlayerName(ownerId);
            String[] ownerUIDS = Server.getLastKnownPlayerUIDs(ownerName);
            newArea.setAttribute("ownerUID", ownerUIDS[0]);
            newArea.setAttribute("ownerDBID", ownerId);
            // newArea.setNameVisible(true);
            // newArea.setName(existingArea.getName());
            newArea.setDefaultPermission(defaultPermission);
            db.saveChunkClaim(ownerUIDS[0], ownerId, chunk, System.currentTimeMillis(), newArea.getID());
        }

        p.sendTextMessage(t().get("TC_AREA_SPLIT", p)
                .replace("PH_AREA_NAME", existingArea.getName())
                .replace("PH_CHUNK_COUNT", chunks.size() + ""));
        // we do not need this area anymore
        // existingArea.destroy();
        // note: we do not have to touch any db entry as the chunks remain claimed
        return true;
    }
}
