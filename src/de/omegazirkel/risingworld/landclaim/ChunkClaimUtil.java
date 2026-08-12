package de.omegazirkel.risingworld.landclaim;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.db.ExtraClaimCapacityService;
import de.omegazirkel.risingworld.landclaim.db.LandClaimChunkService;
import de.omegazirkel.risingworld.landclaim.db.entities.LandClaimChunkInfo;
import de.omegazirkel.risingworld.landclaim.db.CityRecord;
import de.omegazirkel.risingworld.landclaim.db.LeaseholdRecord;
import de.omegazirkel.risingworld.tools.AreaUtils;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.PlayerDatabaseHelper;
import de.omegazirkel.risingworld.tools.PlayerDatabaseHelper.PlayerRecord;
import net.risingworld.api.Server;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3i;

/**
 * Utility class for managing chunk entry/exit, claim permissions,
 * playtime-based limits and claim/expand logic.
 */
public class ChunkClaimUtil {

    private final LandClaimChunkService service;
    private static final PluginSettings s = PluginSettings.getInstance();

    private static final I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    public static OZLogger logger() {
        return LandClaim.logger();
    }

    public static void sendPaymentFailure(Player player, String reason) {
        String message = reason == null ? "" : reason.toLowerCase(java.util.Locale.ROOT);
        if (message.contains("insufficient") || message.contains("not enough")
                || message.contains("nicht ausreichend") || message.contains("unzureichend")) {
            player.sendTextMessage(t().get("TC_CLAIM_ERROR_INSUFFICIENT_FUNDS", player));
            return;
        }
        player.sendTextMessage(t().get("TC_CLAIM_ERROR_PAYMENT", player).replace("PH_REASON", reason == null ? "" : reason));
    }

    public ChunkClaimUtil(LandClaimChunkService lccService) {
        this.service = lccService;
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
            service.saveChunkTime(p, chunk, duration);
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
                service.saveChunkTime(p, chunk, duration);
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
        ExtraClaimCapacityService extraCapacity = LandClaim.extraClaimCapacityService();
        int purchasedCapacity = extraCapacity == null ? 0 : extraCapacity.getPurchasedCapacity(p);

        return s.basicClaimLimit + (long) (hours * s.playTimeHoursExtraClaimFactor) + purchasedCapacity;
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
        int normalClaims = service.getTotalClaimWeight(p);
        int leaseholdClaims = LandClaim.cityService() == null ? 0
                : LandClaim.cityService().leaseholdClaimWeight(p.getUID());
        return normalClaims > Integer.MAX_VALUE - leaseholdClaims ? Integer.MAX_VALUE : normalClaims + leaseholdClaims;
    }

    public long playerTimeInChunkInMs(Player p, Vector3i chunk) {
        return service.getTotalChunkTime(p, chunk);
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

        boolean cityPrivateClaim = ClaimModePolicy.current() == ClaimMode.CITY && cityPrivateClaimAllowed(area);
        if (!ClaimModePolicy.mayCreatePlayerClaim(p.isAdmin(), walletAvailable()) && !cityPrivateClaim) {
            if (callback != null)
                callback.onCall(t().get(ClaimModePolicy.requiresWallet()
                        ? "TC_CLAIM_ERROR_WALLET_REQUIRED"
                        : "TC_CLAIM_ERROR_MODE", p));
            return false;
        }

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
        if (p.isAdmin() && (ClaimModePolicy.adminBypassesClaimLimit(p.isAdmin())
                || (s.adminIgnoreLimit && (s.adminIgnoreTime || !ClaimModePolicy.usesClaimTime())))) {
            return true;
        }

        // 3. Check max claims reached
        if (current >= maxAllowed && !(p.isAdmin()
                && (s.adminIgnoreLimit || ClaimModePolicy.adminBypassesClaimLimit(true)))) {
            if (callback != null)
                callback.onCall(t().get("TC_CLAIM_ERROR_MAX_REACHED", p));
            return false;
        }

        if (area != null && ClaimModePolicy.usesClaimTime()) {
            // 4. Check minimum time spent in chunk
            long time = service.getTotalChunkTime(p, area.getStartChunkPosition()) / 1000;

            if (time < getPlayerNextClaimTime(p) && !(p.isAdmin() && s.adminIgnoreTime)) {
                if (callback != null)
                    callback.onCall(t().get("TC_CLAIM_ERROR_TIME", p)
                            .replace("PH_TIME_LEFT", (getPlayerNextClaimTime(p) - time) + "s"));
                return false;
            }
        }

        return true;
    }

    private boolean walletAvailable() {
        return LandClaim.economyIntegration() != null && LandClaim.economyIntegration().isWalletAvailable()
                && LandClaim.economyIntegration().hasSystemAccountApi();
    }

    private boolean cityPrivateClaimAllowed(Area area) {
        return walletAvailable() && isCityPrivateClaimLocationAllowed(area);
    }

    public boolean isCityPrivateClaimLocationAllowed(Area area) {
        if (LandClaim.cityService() == null || area == null) return false;
        CityRecord city = LandClaim.cityService().containingCity(area.getStartChunkPosition()).orElse(null);
        if (city == null || !de.omegazirkel.risingworld.landclaim.db.CityService.contains(city,
                area.getEndChunkPosition())) return false;
        return city.allowPrivateClaimsOverride() == null
                ? Boolean.TRUE.equals(s.cityAllowPrivateClaims)
                : city.allowPrivateClaimsOverride();
    }

    // ---------------------------------------------------------
    // Area / Chunk utility
    // ---------------------------------------------------------

    /**
     * Converts a chunk coordinate into an Area
     */
    public static Area getVirtualAreaFromChunkVector(Vector3i chunkPosition) {
        return AreaUtils.getVirtualAreaFromChunkVector(chunkPosition);
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
        return AreaUtils.chunksToArea(chunks);
    }

    public Area isAreaIntersecting(Area area) {
        return AreaUtils.isAreaIntersecting(area);
    }

    /**
     * Quick check if area is in only one chunk
     * 
     * @param area
     * @return
     */
    private boolean isSingleChunkArea(Area area) {
        // no area = no chunks (also not > 1 chunk)
        if (area == null)
            return true;
        Vector3i start = area.getStartChunkPosition();
        Vector3i end = area.getEndChunkPosition();
        return start.x == end.x && start.z == end.z && start.y == end.y;
    }

    // ---------------------------------------------------------
    // Repair / sync / import areas
    // ---------------------------------------------------------

    public void syncAndRepairAreas() {
        // 1. check all claimed chunks if areas exist
        List<LandClaimChunkInfo> infoList = service.getChunkInfoListClaimed();

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

                    service.saveChunkClaim(playerUID, playerDBID, info.chunkPos, info.claimedAtMs, va.getID());
                } else if (playerPermission != null && !playerPermission.equals(s.ownerAreaPermission)) {
                    // case 2: area exists but ownership is wrong
                    logger().warn("Player " + va.getName() + "is not owner of " + areaId);
                    // remove claim
                    service.saveChunkClaim(playerUID, playerDBID, info.chunkPos, 0, 0);
                } else if (info.areaID == 0) {
                    // case 3: areaID is missing
                    logger().warn("LCCI " + areaId + " is missing areaID");

                    // use existing to fix
                    service.saveChunkClaim(playerUID, playerDBID, info.chunkPos, info.claimedAtMs, existing.getID());
                }
            }

        // 2. check server-areas for ownership
        for (Area a : Server.getAllAreas()) {
            // get LandClaimChunkInfo for area
            List<Vector3i> chunks = areaToChunks(a);
            List<LandClaimChunkInfo> infoAreaList = service.getChunkInfoListByArea(a.getID());
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
                        service.saveChunkClaim(ownerUID, ownerDbId, chunk, claimedAt, a.getID());
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
        String paymentCorrelation = null;
        long landPrice = 0L;
        if (ClaimModePolicy.current() == ClaimMode.LAND_PRICING
                && defaultPermission.equals(s.defaultAreaPermission)) {
            if (LandClaim.landPriceService() == null) {
                p.sendTextMessage(t().get("TC_CLAIM_ERROR_PRICE_UNAVAILABLE", p));
                return null;
            }
            landPrice = landPriceForClaim(area.getStartChunkPosition(), service.getTotalClaimWeight(p));
            paymentCorrelation = landPrice == 0 ? null : "land-claim:" + UUID.randomUUID();
            if (paymentCorrelation != null) LandClaim.cityService().beginEconomyOperation(paymentCorrelation,
                    "LAND_CLAIM", 0, p.getDbID(), landPrice);
            EconomyIntegration.WalletOperationResult payment = landPrice == 0
                    ? new EconomyIntegration.WalletOperationResult(true, "")
                    : LandClaim.economyIntegration().transferPlayerToWorld(p.getDbID(), landPrice,
                            t().get("TC_WALLET_LAND_PURCHASE", LandClaim.economyIntegration().walletAuditLanguage())
                                    .replace("PH_CHUNK_POS", area.getStartChunkPosition().toString())
                                    .replace("PH_PLAYER_NAME", p.getName()),
                            paymentCorrelation);
            if (!payment.success()) {
                if (paymentCorrelation != null) LandClaim.cityService().updateEconomyOperation(paymentCorrelation,
                        "FAILED", 0, payment.message());
                sendPaymentFailure(p, payment.message());
                return null;
            }
            if (paymentCorrelation != null) LandClaim.cityService().updateEconomyOperation(paymentCorrelation,
                    "PAID", 0, "");
        }
        if (ClaimModePolicy.current() == ClaimMode.CITY
                && defaultPermission.equals(s.defaultAreaPermission)) {
            CityRecord city = LandClaim.cityService() == null ? null
                    : LandClaim.cityService().containingCity(area.getStartChunkPosition()).orElse(null);
            if (city == null || !cityPrivateClaimAllowed(area)) {
                p.sendTextMessage(t().get("TC_CLAIM_ERROR_CITY_PRIVATE", p));
                return null;
            }
            landPrice = city.privateClaimPriceOverride() == null ? Math.max(0L, s.cityPrivateClaimPrice)
                    : Math.max(0L, city.privateClaimPriceOverride());
            paymentCorrelation = landPrice == 0 ? null : "city-private-claim:" + UUID.randomUUID();
            if (paymentCorrelation != null) LandClaim.cityService().beginEconomyOperation(paymentCorrelation,
                    "CITY_PRIVATE_CLAIM", city.areaId(), p.getDbID(), landPrice);
            EconomyIntegration.WalletOperationResult payment = landPrice == 0
                    ? new EconomyIntegration.WalletOperationResult(true, "")
                    : LandClaim.economyIntegration().transferPlayerToCity(p.getDbID(), city.areaId(), landPrice,
                            t().get("TC_WALLET_CITY_PRIVATE_PURCHASE", LandClaim.economyIntegration().walletAuditLanguage())
                                    .replace("PH_CHUNK_POS", area.getStartChunkPosition().toString())
                                    .replace("PH_CITY_NAME", city.name()), paymentCorrelation);
            if (!payment.success()) {
                if (paymentCorrelation != null) LandClaim.cityService().updateEconomyOperation(paymentCorrelation,
                        "FAILED", city.areaId(), payment.message());
                sendPaymentFailure(p, payment.message());
                return null;
            }
            if (paymentCorrelation != null) LandClaim.cityService().updateEconomyOperation(paymentCorrelation,
                    "PAID", city.areaId(), "");
        }
        try {
        long now = System.currentTimeMillis();
        area.setNameVisible(true);
        area.setDefaultPermission(defaultPermission);

        // only save owner if claim is a player-claim and not a special area
        if (ownerDBId != null && defaultPermission.equals(s.defaultAreaPermission)) {
            String ownerName = Server.getLastKnownPlayerName(ownerDBId);
            area.setName("Claimed by " + ownerName);
            service.saveChunkClaim(p, area.getStartChunkPosition(), now, area.getID());
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
        if (LandClaim.landPriceService() != null) LandClaim.landPriceService().refresh();
        if (paymentCorrelation != null) LandClaim.cityService().updateEconomyOperation(paymentCorrelation,
                "COMPLETED", area.getID(), "");
        return area;
        } catch (RuntimeException ex) {
            if (paymentCorrelation != null) {
                EconomyIntegration.WalletOperationResult reversal = LandClaim.economyIntegration().reverseTransfer(
                        paymentCorrelation, paymentCorrelation + ":reversal",
                        t().get("TC_WALLET_LAND_PURCHASE_ROLLBACK", LandClaim.economyIntegration().walletAuditLanguage()));
                LandClaim.cityService().updateEconomyOperation(paymentCorrelation,
                        reversal.success() ? "REVERSED" : "RECONCILIATION_REQUIRED", area == null ? 0 : area.getID(),
                        reversal.message());
                if (!reversal.success()) logger().error("Land purchase reversal failed: " + reversal.message());
            }
            logger().error("Could not create paid land claim: " + ex.getMessage());
            return null;
        }
    }

    public boolean transferAreaOwnership(Area area, Player newOwner) {
        if (newOwner == null) {
            return false;
        }
        return transferAreaOwnership(area, newOwner.getUID(), newOwner.getDbID());
    }

    public boolean transferAreaOwnership(Area area, String newOwnerUid, int newOwnerDbId) {
        if (area == null || newOwnerUid == null || newOwnerUid.isBlank() || newOwnerDbId <= 0) {
            return false;
        }
        if (!service.transferAreaClaims(area.getID(), newOwnerUid, newOwnerDbId)) {
            return false;
        }
        Map<Integer, String> permissionSet = area.getAllPlayerPermissions();
        if (permissionSet != null) {
            for (Map.Entry<Integer, String> entry : List.copyOf(permissionSet.entrySet())) {
                area.removePlayerPermission(entry.getKey());
            }
        }
        area.setAttribute("ownerUID", newOwnerUid);
        area.setAttribute("ownerDBID", newOwnerDbId);
        area.setPlayerPermission(newOwnerDbId, s.ownerAreaPermission);
        return true;
    }

    // ---------------------------------------------------------
    // Expansion
    // ---------------------------------------------------------

    private boolean helperCheckForClaim(Vector3i chunk, String claimOwnerUid, Player feedbackPlayer) {
        List<LandClaimChunkInfo> infoList = service.getChunkInfoListByChunk(chunk);
        Area intersectingArea = isAreaIntersecting(getVirtualAreaFromChunkVector(chunk));
        if (intersectingArea != null && !intersectingArea.getDefaultPermission().equals(s.defaultAreaPermission)) {
            // Area is special area or other non-default area, prevent claiming
            feedbackPlayer.sendTextMessage(t().get("TC_CLAIM_ERROR_SPECIAL_AREA", feedbackPlayer)
                    .replace("PH_CHUNK_POS", chunk.toString() + ""));
            return false;
        }
        // check if chunk is claimed by another user
        for (LandClaimChunkInfo info : infoList) {
            if (info.isOwnedBy(claimOwnerUid)) {
                continue;
            } else if (info.isClaimed()) {
                // TODO later: check area offline protection
                // checkPlayerOfflineProtection(info.playerDBID)
                // area is claimed by someone else
                feedbackPlayer.sendTextMessage(t().get("TC_CLAIM_ERROR_CLAIMED", feedbackPlayer)
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
        if (!ClaimModePolicy.mayPlayerResizeOrRelease(p.isAdmin())) {
            p.sendTextMessage(t().get("TC_CLAIM_ERROR_MODE_RESIZE", p));
            return null;
        }
        Vector3i startChunk = area.getStartChunkPosition();
        Vector3i endChunk = area.getEndChunkPosition();
        List<Vector3i> chunks = areaToChunks(area);
        List<Area> areasToRemove = new java.util.ArrayList<>();
        List<Vector3i> chunkInExtendedArea = new java.util.ArrayList<>();
        String defaultPermission = area.getDefaultPermission();
        String claimOwnerUid = p.getUID();
        int claimOwnerDbId = p.getDbID();
        long ownerMaxClaims = getPlayerMaxClaims(p);
        int ownerClaimCount = getPlayerClaimCount(p);
        boolean administrativeClaimExpansion = p.isAdmin() && ClaimModePolicy.current() == ClaimMode.ADMINISTRATIVE
                && s.defaultAreaPermission.equals(defaultPermission);
        if (administrativeClaimExpansion) {
            ClaimOwner owner = claimOwner(area);
            if (owner == null) {
                p.sendTextMessage(t().get("TC_CLAIM_ERROR_OWNER_OFFLINE", p));
                return null;
            }
            claimOwnerUid = owner.uid();
            claimOwnerDbId = owner.dbId();
            ownerClaimCount = claimCount(owner.uid());
            ownerMaxClaims = maxClaims(owner);
        }

        // ISSUE: Sector 0 -1 cant expand North/south correctly
        // example chunk: 94 1 -24, worldPart: 5 -2
        // pos: 3016 113 -747
        // ISSUE: Sector -1 0 cant expand East/West correctly

        switch (dir) {
            case NORTH: // check all chunks with max(z) if their neighbour +1z is claimable
                for (Vector3i chunk : chunks) {
                    // first add all current chunks to the extended area
                    chunkInExtendedArea.add(chunk);
                    if (chunk.z != endChunk.z) {
                        continue;
                    }
                    Vector3i c = new Vector3i(chunk.x, chunk.y, chunk.z + 1);
                    if (!helperCheckForClaim(c, claimOwnerUid, p)) {
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
                    if (!helperCheckForClaim(c, claimOwnerUid, p)) {
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
                    if (!helperCheckForClaim(c, claimOwnerUid, p)) {
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
                    if (!helperCheckForClaim(c, claimOwnerUid, p)) {
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
                    if (!helperCheckForClaim(c, claimOwnerUid, p)) {
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
                    if (!helperCheckForClaim(c, claimOwnerUid, p)) {
                        return null;
                    }
                    chunkInExtendedArea.add(c);
                }
                break;
        }

        // Only chunks that are not already owned consume capacity or require claim time.
        // The complete perimeter remains necessary below to merge adjacent owned areas.
        String expansionOwnerUid = claimOwnerUid;
        List<Vector3i> newlyClaimedChunks = chunkInExtendedArea.stream()
                .filter(chunk -> !isAlreadyOwnedBy(chunk, expansionOwnerUid))
                .toList();

        // determine if newly acquired chunks exceed claim limit
        Integer currentClaimCount = ownerClaimCount;
        Integer extendedChunksCount = newlyClaimedChunks.size();

        CityRecord cityForExpansion = null;
        if (ClaimModePolicy.current() == ClaimMode.CITY && defaultPermission.equals(s.defaultAreaPermission)) {
            cityForExpansion = LandClaim.cityService() == null ? null
                    : LandClaim.cityService().containingCity(area.getStartChunkPosition()).orElse(null);
            boolean privateAllowed = cityForExpansion != null
                    && (cityForExpansion.allowPrivateClaimsOverride() == null
                            ? Boolean.TRUE.equals(s.cityAllowPrivateClaims)
                            : cityForExpansion.allowPrivateClaimsOverride());
            CityRecord validatedCity = cityForExpansion;
            if (!privateAllowed || chunkInExtendedArea.stream().anyMatch(
                    chunk -> !de.omegazirkel.risingworld.landclaim.db.CityService.contains(validatedCity, chunk))) {
                p.sendTextMessage(t().get("TC_CLAIM_ERROR_CITY_EXPANSION", p));
                return null;
            }
        }
        if (ClaimModePolicy.current() == ClaimMode.CITY
                && defaultPermission.equals(s.specialCityLeaseholdPermission)) {
            LeaseholdRecord lease = LandClaim.cityService() == null ? null
                    : LandClaim.cityService().findLeasehold(area.getID()).orElse(null);
            CityRecord leaseCity = lease == null ? null
                    : LandClaim.cityService().findCity(lease.cityAreaId()).orElse(null);
            if (lease == null || leaseCity == null || chunkInExtendedArea.stream()
                    .anyMatch(chunk -> !de.omegazirkel.risingworld.landclaim.db.CityService.contains(leaseCity, chunk))) {
                p.sendTextMessage(t().get("TC_CITY_LEASE_EXPAND_OUTSIDE", p));
                return null;
            }
            if (lease.occupied()) {
                Player owner = Server.getPlayerByDbID(lease.ownerDbId());
                if (owner == null || getPlayerClaimCount(owner) + extendedChunksCount > getPlayerMaxClaims(owner)) {
                    p.sendTextMessage(t().get("TC_CITY_LEASE_EXPAND_LIMIT", p));
                    return null;
                }
            }
        }

        if (currentClaimCount + extendedChunksCount > ownerMaxClaims
                && !(p.isAdmin() && !administrativeClaimExpansion
                        && (s.adminIgnoreLimit || ClaimModePolicy.adminBypassesClaimLimit(true)))) {
            p.sendTextMessage(t().get("TC_CLAIM_ERROR_LIMIT", p)
                    .replace("PH_MAX_CLAIMS", ownerMaxClaims + ""));
            return null;
        }

        // check chunks for combined areas and split if needed
        for (Vector3i chunk : chunkInExtendedArea) {
            List<LandClaimChunkInfo> infoList = service.getChunkInfoListByChunk(chunk);
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
        for (Vector3i chunk : newlyClaimedChunks) {
            sumTimeInChunks += playerTimeInChunkInSeconds(p, chunk);
        }

        if (ClaimModePolicy.usesClaimTime() && sumTimeInChunks < timeToClaimNeeded
                && !(p.isAdmin() && s.adminIgnoreTime)) {
            // time needed not reached
            p.sendTextMessage(t().get("TC_CLAIM_ERROR_TIME", p)
                    .replace("PH_TIME_NEEDED", timeToClaimNeeded + "s")
                    .replace("PH_TIME_LEFT", (timeToClaimNeeded - sumTimeInChunks) + "s"));
            return null;
        }

        String topologyCorrelation = null;
        if (ClaimModePolicy.current() == ClaimMode.LAND_PRICING
                && defaultPermission.equals(s.defaultAreaPermission)) {
            if (!walletAvailable() || LandClaim.landPriceService() == null) {
                p.sendTextMessage(t().get("TC_CLAIM_ERROR_WALLET_REQUIRED", p));
                return null;
            }
            long expansionPrice = landExpansionPrice(area, dir, claimOwnerUid);
            String correlation = "land-expand:" + UUID.randomUUID();
            topologyCorrelation = correlation;
            LandClaim.cityService().beginEconomyOperation(correlation, "LAND_EXPANSION", area.getID(), p.getDbID(),
                    expansionPrice);
            EconomyIntegration.WalletOperationResult payment = expansionPrice == 0
                    ? new EconomyIntegration.WalletOperationResult(true, "")
                    : LandClaim.economyIntegration().transferPlayerToWorld(claimOwnerDbId, expansionPrice,
                            t().get("TC_WALLET_LAND_EXPANSION", LandClaim.economyIntegration().walletAuditLanguage())
                                    .replace("PH_AREA_NAME", area.getName() == null ? "" : area.getName()),
                            correlation);
            if (!payment.success()) {
                LandClaim.cityService().updateEconomyOperation(correlation, "FAILED", area.getID(), payment.message());
                sendPaymentFailure(p, payment.message());
                return null;
            }
            LandClaim.cityService().updateEconomyOperation(correlation, "PAID", area.getID(), "");
        }
        if (ClaimModePolicy.current() == ClaimMode.CITY && defaultPermission.equals(s.defaultAreaPermission)) {
            long expansionPrice = cityPrivateExpansionPrice(area, dir, claimOwnerUid);
            String correlation = "city-private-expand:" + UUID.randomUUID();
            topologyCorrelation = correlation;
            LandClaim.cityService().beginEconomyOperation(correlation, "CITY_PRIVATE_EXPANSION", area.getID(),
                    p.getDbID(), expansionPrice);
            EconomyIntegration.WalletOperationResult payment = expansionPrice == 0
                    ? new EconomyIntegration.WalletOperationResult(true, "")
                    : LandClaim.economyIntegration().transferPlayerToCity(p.getDbID(), cityForExpansion.areaId(),
                            expansionPrice, t().get("TC_WALLET_CITY_PRIVATE_EXPANSION", LandClaim.economyIntegration().walletAuditLanguage())
                                    .replace("PH_CITY_NAME", cityForExpansion.name()), correlation);
            if (!payment.success()) {
                LandClaim.cityService().updateEconomyOperation(correlation, "FAILED", area.getID(), payment.message());
                sendPaymentFailure(p, payment.message());
                return null;
            }
            LandClaim.cityService().updateEconomyOperation(correlation, "PAID", area.getID(), "");
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
        // Server.removeArea(area);
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
        area.set(extendedArea.getStartPosition(), extendedArea.getEndPosition());
        // WORKAROUND: we need to create a new area to avoid null pointer exception
        // extendedArea.setName(area.getName());
        // extendedArea.setNameVisible(true);
        // Server.addArea(extendedArea, true);
        // if (defaultPermission.equals(s.defaultAreaPermission)) {
        // extendedArea.setPlayerPermission(p.getDbID(), s.ownerAreaPermission);
        // extendedArea.setAttribute("ownerUID", p.getUID());
        // extendedArea.setAttribute("ownerDBID", p.getDbID());
        // }
        // extendedArea.setDefaultPermission(defaultPermission);

        // we need to transfer all permissions to the new area
        // Map<Integer, String> originAreaPermissions = area.getAllPlayerPermissions();
        // if (originAreaPermissions != null)
        // for (Map.Entry<Integer, String> entry : originAreaPermissions.entrySet()) {
        // extendedArea.setPlayerPermission(entry.getKey(), entry.getValue());
        // }

        // last step: set claim status in database for all chunks
        if (defaultPermission.equals(s.defaultAreaPermission)) {
            for (Vector3i chunk : chunkInExtendedArea) {
                service.saveChunkClaim(claimOwnerUid, claimOwnerDbId, chunk, System.currentTimeMillis(), area.getID());
            }
        }
        // remove claim status if it is a special area
        else {
            for (Vector3i chunk : chunkInExtendedArea) {
                service.saveChunkClaim(p, chunk, 0, 0);
            }
        }

        if (LandClaim.landPriceService() != null) LandClaim.landPriceService().refresh();
        if (topologyCorrelation != null) LandClaim.cityService().updateEconomyOperation(topologyCorrelation,
                "COMPLETED", area.getID(), "");

        // area.destroy();
        return area;
    }

    public long landExpansionPrice(Area area, Direction direction) {
        ClaimOwner owner = claimOwner(area);
        return landExpansionPrice(area, direction, owner == null ? "" : owner.uid());
    }

    private long landExpansionPrice(Area area, Direction direction, String ownerUid) {
        if (area == null || direction == null || LandClaim.landPriceService() == null) return 0L;
        Vector3i start = area.getStartChunkPosition();
        Vector3i end = area.getEndChunkPosition();
        long total = 0L;
        int additionalClaimIndex = 0;
        for (Vector3i chunk : areaToChunks(area)) {
            Vector3i added = switch (direction) {
                case NORTH -> chunk.z == end.z ? new Vector3i(chunk.x, chunk.y, chunk.z + 1) : null;
                case SOUTH -> chunk.z == start.z ? new Vector3i(chunk.x, chunk.y, chunk.z - 1) : null;
                case EAST -> chunk.x == end.x ? new Vector3i(chunk.x + 1, chunk.y, chunk.z) : null;
                case WEST -> chunk.x == start.x ? new Vector3i(chunk.x - 1, chunk.y, chunk.z) : null;
                case UP -> chunk.y == end.y ? new Vector3i(chunk.x, chunk.y + 1, chunk.z) : null;
                case DOWN -> chunk.y == start.y ? new Vector3i(chunk.x, chunk.y - 1, chunk.z) : null;
            };
            if (added == null) continue;
            if (isAlreadyOwnedBy(added, ownerUid)) continue;
            long price = landPriceForClaim(added, service.getTotalClaimWeight(ownerUid) + additionalClaimIndex++);
            if (total >= de.omegazirkel.risingworld.landclaim.db.LandPriceService.MAX_SAFE_INTEGER - price)
                return de.omegazirkel.risingworld.landclaim.db.LandPriceService.MAX_SAFE_INTEGER;
            total += price;
        }
        return total;
    }

    /** Included normal claims waive only the base component; cluster surcharge remains payable. */
    private long landPriceForClaim(Vector3i chunk, int ownedNormalClaims) {
        if (LandClaim.landPriceService() == null) return 0L;
        long fullPrice = LandClaim.landPriceService().price(chunk, s.landPriceBase);
        if (!Boolean.TRUE.equals(s.landPriceIncludeBaseClaimsFree)
                || ownedNormalClaims >= Math.max(0, s.basicClaimLimit)) return fullPrice;
        return Math.max(0L, fullPrice - Math.max(0L, s.landPriceBase));
    }

    /** Price for the one-cell perimeter added to a city private claim. */
    public long cityPrivateExpansionPrice(Area area, Direction direction) {
        ClaimOwner owner = claimOwner(area);
        return cityPrivateExpansionPrice(area, direction, owner == null ? "" : owner.uid());
    }

    private long cityPrivateExpansionPrice(Area area, Direction direction, String ownerUid) {
        if (area == null || direction == null || LandClaim.cityService() == null) return 0L;
        CityRecord city = LandClaim.cityService().containingCity(area.getStartChunkPosition()).orElse(null);
        if (city == null) return 0L;
        long pricePerChunk = city.privateClaimPriceOverride() == null
                ? Math.max(0L, s.cityPrivateClaimPrice) : Math.max(0L, city.privateClaimPriceOverride());
        Vector3i start = area.getStartChunkPosition();
        Vector3i end = area.getEndChunkPosition();
        long added = 0L;
        for (Vector3i chunk : areaToChunks(area)) {
            Vector3i expanded = switch (direction) {
                case NORTH -> chunk.z == end.z ? new Vector3i(chunk.x, chunk.y, chunk.z + 1) : null;
                case SOUTH -> chunk.z == start.z ? new Vector3i(chunk.x, chunk.y, chunk.z - 1) : null;
                case EAST -> chunk.x == end.x ? new Vector3i(chunk.x + 1, chunk.y, chunk.z) : null;
                case WEST -> chunk.x == start.x ? new Vector3i(chunk.x - 1, chunk.y, chunk.z) : null;
                case UP -> chunk.y == end.y ? new Vector3i(chunk.x, chunk.y + 1, chunk.z) : null;
                case DOWN -> chunk.y == start.y ? new Vector3i(chunk.x, chunk.y - 1, chunk.z) : null;
            };
            if (expanded != null && !isAlreadyOwnedBy(expanded, ownerUid)) added++;
        }
        try {
            return Math.min(de.omegazirkel.risingworld.landclaim.db.LandPriceService.MAX_SAFE_INTEGER,
                    Math.multiplyExact(pricePerChunk, Math.max(0L, added)));
        } catch (ArithmeticException ex) {
            return de.omegazirkel.risingworld.landclaim.db.LandPriceService.MAX_SAFE_INTEGER;
        }
    }

    private ClaimOwner claimOwner(Area area) {
        if (area == null) return null;
        List<LandClaimChunkInfo> claims = service.getChunkInfoListByArea(area.getID());
        for (LandClaimChunkInfo claim : claims) {
            if (claim.playerUID != null && !claim.playerUID.isBlank() && claim.playerDBID > 0) {
                return new ClaimOwner(claim.playerUID, claim.playerDBID, Server.getPlayerByDbID(claim.playerDBID));
            }
        }
        Object uidAttribute = area.getAttribute("ownerUID");
        Object dbIdAttribute = area.getAttribute("ownerDBID");
        String uid = uidAttribute == null ? "" : String.valueOf(uidAttribute);
        int dbId = dbIdAttribute instanceof Number number ? number.intValue() : 0;
        if (dbId <= 0) {
            Map<Integer, String> permissions = area.getAllPlayerPermissions();
            if (permissions != null) {
                dbId = permissions.entrySet().stream()
                        .filter(entry -> s.ownerAreaPermission.equals(entry.getValue()))
                        .map(Map.Entry::getKey).findFirst().orElse(0);
            }
        }
        Player online = dbId <= 0 ? null : Server.getPlayerByDbID(dbId);
        if (uid.isBlank() && online != null) uid = online.getUID();
        return uid.isBlank() || dbId <= 0 ? null : new ClaimOwner(uid, dbId, online);
    }

    private boolean isAlreadyOwnedBy(Vector3i chunk, String ownerUid) {
        return ownerUid != null && !ownerUid.isBlank() && service.getChunkInfoListByChunk(chunk).stream()
                .anyMatch(info -> info.isClaimed() && info.isOwnedBy(ownerUid));
    }

    private int claimCount(String ownerUid) {
        int normalClaims = service.getTotalClaimWeight(ownerUid);
        int leaseholdClaims = LandClaim.cityService() == null ? 0
                : LandClaim.cityService().leaseholdClaimWeight(ownerUid);
        return normalClaims > Integer.MAX_VALUE - leaseholdClaims ? Integer.MAX_VALUE
                : normalClaims + leaseholdClaims;
    }

    private long maxClaims(ClaimOwner owner) {
        if (owner.online() != null) return getPlayerMaxClaims(owner.online());
        PlayerRecord record = PlayerDatabaseHelper.findPlayersByDbIds(LandClaim.getInstance(), Set.of(owner.dbId()))
                .get(owner.dbId());
        long playtimeSeconds = record == null ? 0L : Math.max(0L, record.totalPlayTimeSeconds);
        int purchasedCapacity = LandClaim.extraClaimCapacityService() == null ? 0
                : LandClaim.extraClaimCapacityService().getPurchasedCapacity(owner.uid());
        return s.basicClaimLimit + (long) ((playtimeSeconds / 3_600L) * s.playTimeHoursExtraClaimFactor)
                + purchasedCapacity;
    }

    private record ClaimOwner(String uid, int dbId, Player online) { }

    public enum Direction {
        NORTH, SOUTH, EAST, WEST, UP, DOWN
    }

    public void releaseArea(Player p, Area area) {
        if (!ClaimModePolicy.mayPlayerResizeOrRelease(p.isAdmin())) {
            p.sendTextMessage(t().get("TC_CLAIM_ERROR_MODE_RESIZE", p));
            return;
        }
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
            if (LandClaim.landPriceService() != null) LandClaim.landPriceService().refresh();

            p.sendTextMessage(t().get("TC_AREA_RELEASE_AREA", p).replace("PH_AREA_NAME", areaName));
            // remove area and claim information from database
            List<LandClaimChunkInfo> infoList = service.getChunkInfoListByArea(area.getID());
            for (LandClaimChunkInfo info : infoList) {
                Player owner = Server.getPlayerByUID(info.playerUID);
                if (p.isAdmin())
                    p.sendTextMessage(t().get("TC_AREA_RELEASE_CHUNK", p)
                            .replace("PH_AREA_NAME", areaName)
                            .replace("PH_CHUNK_POS", info.chunkPos.toString())
                            .replace("PH_PLAYER_NAME", owner.getName()));
                service.saveChunkClaim(owner, info.chunkPos, 0, 0);
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
        if (!ClaimModePolicy.mayPlayerResizeOrRelease(p.isAdmin())) {
            p.sendTextMessage(t().get("TC_CLAIM_ERROR_MODE_RESIZE", p));
            return false;
        }
        LeaseholdRecord lease = LandClaim.cityService() == null || existingArea == null ? null
                : LandClaim.cityService().findLeasehold(existingArea.getID()).orElse(null);
        if (lease != null && lease.occupied()) {
            p.sendTextMessage(t().get("TC_CITY_LEASE_SPLIT_OCCUPIED", p));
            return false;
        }
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
            service.saveChunkClaim(ownerUIDS[0], ownerId, chunk, System.currentTimeMillis(), newArea.getID());
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
