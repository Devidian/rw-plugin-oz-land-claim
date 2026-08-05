package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Map;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil;
import de.omegazirkel.risingworld.landclaim.ClaimModePolicy;
import de.omegazirkel.risingworld.landclaim.ClaimMode;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.landclaim.db.ClaimSaleListing;
import de.omegazirkel.risingworld.landclaim.db.LeaseholdRecord;
import de.omegazirkel.risingworld.tools.I18n;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3i;

public class ChunkInfoController {
    public static final String INVENTORY_VISIBLE_KEY = "oz.landclaim.inventoryVisible";

    private final Player player;
    private final ChunkInfoOverlay overlay;
    private final PluginSettings s = PluginSettings.getInstance();
    private final ChunkClaimUtil ccu;

    private final I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    public ChunkInfoController(Player player, ChunkClaimUtil chunkClaimUtil) {
        this.player = player;
        this.ccu = chunkClaimUtil;
        this.overlay = new ChunkInfoOverlay();
    }

    public Vector3i getChunkPos() {
        return player.getChunkPosition();
    }

    public boolean update() {
        // overlay is opt-out by default
        Boolean enabled = player.hasAttribute(LandClaimPlayerPluginSettings.ENABLE_CLAIM_INFO_OVERLAY_KEY)
                ? (Boolean) player.getAttribute(LandClaimPlayerPluginSettings.ENABLE_CLAIM_INFO_OVERLAY_KEY)
                : true;
        if (!enabled) {
            overlay.hide(player);
            return enabled;
        }
        if (isInventoryVisible()) {
            overlay.hide(player);
            return enabled;
        }

        String text = computeDisplay();
        overlay.updateText(text);
        overlay.show(player);

        return enabled;
    }

    public void hide() {
        overlay.hide(player);
    }

    private String computeDisplay() {
        String specialArea = handleSpecialArea();
        if (specialArea != null) {
            return specialArea;
        }
        // check if owner
        if (isOwner()) {
            return t().get("TC_CHUNKINFO_OWNED", player)
                    .replace("PH_AREA_NAME", getAreaName());
        }
        // check if someone else is owner
        if (isClaimed()) {
            ClaimSaleListing listing = activeSaleListing();
            if (listing != null) {
                return t().get("TC_CHUNKINFO_FOR_SALE", player)
                        .replace("PH_PLAYER_NAME", getOwnerName())
                        .replace("PH_AREA_NAME", getAreaName())
                        .replace("PH_PRICE", String.valueOf(listing.price()))
                        .replace("PH_CURRENCY", defaultCurrency());
            }
            return t().get("TC_CHUNKINFO_OWNED_BY", player)
                    .replace("PH_PLAYER_NAME", getOwnerName())
                    .replace("PH_AREA_NAME", getAreaName());
        }
        boolean cityPrivateClaim = ClaimModePolicy.current() == ClaimMode.CITY && !player.isAdmin()
                && ccu.isCityPrivateClaimLocationAllowed(
                        ChunkClaimUtil.getVirtualAreaFromChunkVector(getChunkPos()));
        if (ClaimModePolicy.current() == ClaimMode.CITY && !player.isAdmin() && !cityPrivateClaim) {
            return t().get("TC_CHUNKINFO_CITY_ONLY", player);
        }
        // check claim limit
        if (!ClaimModePolicy.mayCreatePlayerClaim(player.isAdmin(), walletAvailable()) && !cityPrivateClaim) {
            return t().get(ClaimModePolicy.requiresWallet()
                    ? "TC_CHUNKINFO_WALLET_REQUIRED"
                    : "TC_CHUNKINFO_CLAIM_BLOCKED", player);
        }
        if (ClaimModePolicy.current() == ClaimMode.CITY && !walletAvailable()) {
            return t().get("TC_CHUNKINFO_WALLET_REQUIRED", player);
        }
        long maxClaims = ccu.getPlayerMaxClaims(player);
        Integer claimCount = ccu.getPlayerClaimCount(player);
        if (claimCount >= maxClaims) {
            return t().get("TC_CHUNKINFO_CLAIM_LIMIT", player)
                    .replace("PH_MAX_CLAIMS", maxClaims + "")
                    .replace("PH_CLAIM_COUNT", claimCount + "");
        }
        if (!ClaimModePolicy.usesClaimTime()) {
            if (ClaimModePolicy.current() == de.omegazirkel.risingworld.landclaim.ClaimMode.LAND_PRICING
                    && LandClaim.landPriceService() != null) {
                long price = LandClaim.landPriceService().price(getChunkPos(), s.landPriceBase);
                return t().get("TC_CHUNKINFO_LAND_PRICE", player).replace("PH_PRICE", String.valueOf(price))
                        .replace("PH_CURRENCY", defaultCurrency());
            }
            if (ClaimModePolicy.current() == ClaimMode.CITY && cityPrivateClaim) {
                de.omegazirkel.risingworld.landclaim.db.CityRecord city = LandClaim.cityService()
                        .containingCity(getChunkPos()).orElse(null);
                long price = city == null || city.privateClaimPriceOverride() == null ? s.cityPrivateClaimPrice
                        : city.privateClaimPriceOverride();
                return t().get("TC_CHUNKINFO_CITY_PRICE", player).replace("PH_PRICE", String.valueOf(price))
                        .replace("PH_CURRENCY", defaultCurrency());
            }
            return t().get("TC_CHUNKINFO_CLAIM_POSSIBLE", player);
        }
        // calculate time left to claim and show time or claimable message

        long timeSpentBeforeEntry = ccu.playerTimeInChunkInMs(player, getChunkPos());
        long timeSpentThisVisit = ccu.currentTimeinChunkMs(player, getChunkPos());

        long minTimeToClaimSeconds = ccu.getPlayerNextClaimTime(player);

        long timeLeftMs = (minTimeToClaimSeconds * 1000) - (timeSpentBeforeEntry + timeSpentThisVisit);

        if (timeSpentThisVisit > 10000) {
            ccu.idleChunk(player);
        }

        // player.showStatusMessage("timeleft: " + timeLeft + "ms" + "| totalSpentMs: "
        // + totalSpentMs + "ms", 1);

        // Spezialchecks

        if (timeLeftMs <= 0) {
            return t().get("TC_CHUNKINFO_CLAIM_POSSIBLE", player);
        }

        // Formatierung m:s
        long sec = timeLeftMs / 1000;
        long hours = sec / 60000;
        long minutes = (sec % 60000) / 60;
        long seconds = sec % 60;

        return t().get("TC_CHUNKINFO_CLAIM_WAIT", player)
                .replace("PH_TIME", String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private String getOwnerName() {
        String ownerName = null;
        Area currentArea = player.getCurrentArea();
        if (currentArea == null)
            return "No area found";
        Map<Integer, String> areaPermissions = currentArea.getAllPlayerPermissions();
        if (areaPermissions != null)
            for (Map.Entry<Integer, String> entry : areaPermissions.entrySet()) {
                if (s.ownerAreaPermission.equals(entry.getValue())) {
                    ownerName = Server.getLastKnownPlayerName(entry.getKey());
                    break;
                }
            }
        if (ownerName != null)
            return ownerName;
        else
            return "No owner found";
    }

    private String getAreaName() {
        Area currentArea = player.getCurrentArea();
        if (currentArea == null || currentArea.getName() == null || currentArea.getName().isBlank()) {
            return "N/A";
        }
        return currentArea.getName();
    }

    private boolean isClaimed() {
        Area currentArea = player.getCurrentArea();
        return currentArea != null;
    }

    private boolean isOwner() {
        Area currentArea = player.getCurrentArea();
        String areaPermission = currentArea == null ? null : currentArea.getPlayerPermission(player);
        return areaPermission != null && areaPermission.equals(s.ownerAreaPermission);
    }

    private ClaimSaleListing activeSaleListing() {
        Area currentArea = player.getCurrentArea();
        if (!ClaimModePolicy.salesAvailable(s.allowClaimSale, walletAvailable()) || currentArea == null
                || LandClaim.claimSaleListingService() == null) {
            return null;
        }
        return LandClaim.claimSaleListingService().activeListing(currentArea.getID()).orElse(null);
    }

    private boolean walletAvailable() {
        return LandClaim.economyIntegration() != null && LandClaim.economyIntegration().isWalletAvailable()
                && LandClaim.economyIntegration().hasSystemAccountApi();
    }

    private String defaultCurrency() {
        return LandClaim.economyIntegration() == null ? ""
                : LandClaim.economyIntegration().defaultCurrencyIdentifier();
    }

    private String handleSpecialArea() {
        Area currentArea = player.getCurrentArea();
        if (currentArea == null)
            return null;
        String areaName = currentArea.getName();
        if (areaName == null)
            areaName = "N/A";
        String defaultPermission = currentArea.getDefaultPermission();

        if (defaultPermission == null || defaultPermission.equals(s.defaultAreaPermission)) {
            // no special area
            return null;
        }
        // we have a special area
        if (defaultPermission.equals(s.specialAreaPermission)) {
            return t().get("TC_CHUNKINFO_SPECIAL_AREA", player)
                    .replace("PH_AREA_NAME", areaName);
        }
        if (defaultPermission.equals(s.specialPvPAreaPermission)) {
            return t().get("TC_CHUNKINFO_SPECIAL_PVP_AREA", player)
                    .replace("PH_AREA_NAME", areaName);
        }
        if (defaultPermission.equals(s.specialStaticAreaPermission)) {
            return t().get("TC_CHUNKINFO_SPECIAL_STATIC_AREA", player)
                    .replace("PH_AREA_NAME", areaName);
        }
        if (defaultPermission.equals(s.specialRestAreaPermission)) {
            return t().get("TC_CHUNKINFO_SPECIAL_REST_AREA", player)
                    .replace("PH_AREA_NAME", areaName);
        }
        if (defaultPermission.equals(s.specialTrapAreaPermission)) {
            return t().get("TC_CHUNKINFO_SPECIAL_TRAP_AREA", player)
                    .replace("PH_AREA_NAME", areaName);
        }
        if (defaultPermission.equals(s.specialRenewAreaPermission)) {
            return t().get("TC_CHUNKINFO_SPECIAL_RENEW_AREA", player)
                    .replace("PH_AREA_NAME", areaName);
        }
        if (defaultPermission.equals(s.specialCityCorePermission)) {
            return t().get("TC_CHUNKINFO_CITY_CORE", player).replace("PH_AREA_NAME", areaName);
        }
        if (defaultPermission.equals(s.specialCityLeaseholdPermission)) {
            LeaseholdRecord leasehold = LandClaim.cityService() == null ? null
                    : LandClaim.cityService().findLeasehold(currentArea.getID()).orElse(null);
            if (leasehold == null || !leasehold.occupied()) {
                return t().get("TC_CHUNKINFO_CITY_LEASEHOLD_FREE", player).replace("PH_AREA_NAME", areaName);
            }
            if (leasehold.ownerDbId() == player.getDbID()) {
                return t().get("TC_CHUNKINFO_CITY_LEASEHOLD_OWNED", player).replace("PH_AREA_NAME", areaName);
            }
            String ownerName = Server.getLastKnownPlayerName(leasehold.ownerDbId());
            return t().get("TC_CHUNKINFO_CITY_LEASEHOLD_OCCUPIED", player)
                    .replace("PH_AREA_NAME", areaName)
                    .replace("PH_PLAYER_NAME", ownerName == null ? String.valueOf(leasehold.ownerDbId()) : ownerName);
        }
        return null;
    }

    public void setInventoryVisible(boolean visible) {
        player.setAttribute(INVENTORY_VISIBLE_KEY, visible);
        if (visible) {
            overlay.hide(player);
        } else {
            update();
        }
    }

    private boolean isInventoryVisible() {
        if (!player.hasAttribute(INVENTORY_VISIBLE_KEY)) {
            return false;
        }
        Object value = player.getAttribute(INVENTORY_VISIBLE_KEY);
        return value instanceof Boolean && (Boolean) value;
    }

}
