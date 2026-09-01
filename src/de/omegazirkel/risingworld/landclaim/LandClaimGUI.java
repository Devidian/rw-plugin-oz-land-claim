package de.omegazirkel.risingworld.landclaim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.EconomyIntegration.WalletOperationResult;
import de.omegazirkel.risingworld.landclaim.db.ClaimSaleListing;
import de.omegazirkel.risingworld.landclaim.db.RenewZoneConfig;
import de.omegazirkel.risingworld.landclaim.db.CityRecord;
import de.omegazirkel.risingworld.landclaim.db.LeaseholdRecord;
import de.omegazirkel.risingworld.landclaim.db.LandPriceService;
import de.omegazirkel.risingworld.landclaim.ui.AdminCleanupOverlay;
import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil.Direction;
import de.omegazirkel.risingworld.landclaim.ui.LandClaimPlayerPluginSettings;
import de.omegazirkel.risingworld.landclaim.ui.PermissionOverlay;
import de.omegazirkel.risingworld.landclaim.ui.UIDialogFactory;
import de.omegazirkel.risingworld.landclaim.ui.CityManagementOverlay;
import de.omegazirkel.risingworld.landclaim.ui.LeaseholdManagementOverlay;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.utils.Vector3i;

/**
 * LandClaimGUI
 * LandClaim main menu is using integrated radial menu with submenues
 * 
 * Menu tree:
 * - Main
 * | - ✅: open Visibility settings
 * | | - ✅: switch current chunk frame (yellow eye)
 * | | - ✅: switch owned area frames (green eye)
 * | | - ✅: switch other area frames (blue eye)
 * | | - ✅: close menu (Visibility settings)
 * | - ✅: open Admin Menu (Admin only)
 * | | - ✅: show debug stuff [DEV ONLY]
 * | | - ✅: rename area
 * | | - ✅: manage permissions
 * | | - ✅: remove claim/area
 * | | - ✅: open special area menu
 * | | | - ✅: set current area to special area (white border)
 * | | | - ✅: set current area to arena (pvp) area (red border)
 * | | | - ✅: set current area to arena (rest) area (light green? border)
 * | | | - ✅: set current area to arena (trap) area (orange border)
 * | | | - ✅: open expand area menu (adjust expanding method for special
 * areas)
 * | | - ✅: start repairmode
 * | | - TODO: mark for sale
 * | | - ✅: close menu (Admin Menu)
 * | - ✅: claim current chunk (if possible)
 * | - TODO: buy current area (if set for sale)
 * | - ✅: open claim options
 * | | - ✅: rename area
 * | | - ✅: manage permissions
 * | | - ✅: unclaim area
 * | | - ✅: split area (if multi-chunk)
 * | | - ✅: open expand area
 * | | | - ✅: expand north
 * | | | - ✅: expand east
 * | | | - ✅: expand south
 * | | | - ✅: expand west
 * | | | - ✅: expand up
 * | | | - ✅: expand down
 * | | | - ✅: close menu (expand area)
 * | | - TODO: set area for sale
 * | | - ✅: close menu (claim options)
 * | - ✅: close menu (Main)
 * 
 * Other:
 * - ✅ time to claim hud
 * 
 * 
 */
public class LandClaimGUI {
    private static LandClaimGUI instance = null;
    private static ChunkClaimUtil chunkClaimUtil;
    private static ClaimCleanupService cleanupService;
    private static final I18n t = I18n.getInstance(LandClaim.name);
    private static final PluginSettings s = PluginSettings.getInstance();

    public static LandClaimGUI getInstance(ChunkClaimUtil ccu, ClaimCleanupService ccs, Plugin p) {
        chunkClaimUtil = ccu;
        cleanupService = ccs;

        // Icon for /ozt menu
        AssetManager.loadIconFromPlugin(p, "oz-land-claim");
        // Icons for main menu
        AssetManager.loadIconFromPlugin(p, "zone-claim-create");
        AssetManager.loadIconFromPlugin(p, "zone-sale-indicator");
        AssetManager.loadIconFromPlugin(p, "zone-city-core");
        AssetManager.loadIconFromPlugin(p, "zone-city-leasehold");

        // Visibility menu
        AssetManager.loadIconFromPlugin(p, "menu-zone-visibility"); // menu icon
        AssetManager.loadIconFromPlugin(p, "zone-visibility-current-on");
        AssetManager.loadIconFromPlugin(p, "zone-visibility-current-off");
        AssetManager.loadIconFromPlugin(p, "zone-visibility-owned-on");
        AssetManager.loadIconFromPlugin(p, "zone-visibility-owned-off");
        AssetManager.loadIconFromPlugin(p, "zone-visibility-others-on");
        AssetManager.loadIconFromPlugin(p, "zone-visibility-others-off");
        // Area administration menu for players
        AssetManager.loadIconFromPlugin(p, "zone-claim-rename");
        AssetManager.loadIconFromPlugin(p, "zone-claim-split");
        AssetManager.loadIconFromPlugin(p, "zone-claim-delete");
        AssetManager.loadIconFromPlugin(p, "zone-sale");
        AssetManager.loadIconFromPlugin(p, "menu-zone-permissions");
        // Area expansion menu
        AssetManager.loadIconFromPlugin(p, "menu-expand-zone");
        AssetManager.loadIconFromPlugin(p, "zone-expand-north");
        AssetManager.loadIconFromPlugin(p, "zone-expand-east");
        AssetManager.loadIconFromPlugin(p, "zone-expand-south");
        AssetManager.loadIconFromPlugin(p, "zone-expand-west");
        AssetManager.loadIconFromPlugin(p, "zone-expand-up");
        AssetManager.loadIconFromPlugin(p, "zone-expand-down");
        // Admin Menu
        AssetManager.loadIconFromPlugin(p, "menu-zone-admin"); // Admin menu
        AssetManager.loadIconFromPlugin(p, "menu-zone-management"); // Zone manager
        // Special area menu (admin)
        AssetManager.loadIconFromPlugin(p, "menu-zone-special"); // open special area menu
        AssetManager.loadIconFromPlugin(p, "zone-neutral-create"); // create default special area
        AssetManager.loadIconFromPlugin(p, "zone-trap-create"); // create trap area
        AssetManager.loadIconFromPlugin(p, "zone-rest-create"); // create rest area
        AssetManager.loadIconFromPlugin(p, "zone-combat-create"); // create pvp area
        AssetManager.loadIconFromPlugin(p, "zone-static-create"); // static area
        AssetManager.loadIconFromPlugin(p, "zone-renew-create"); // renew area

        return getInstance();
    }

    public static LandClaimGUI getInstance() {
        if (instance == null) {
            instance = new LandClaimGUI();
        }
        return instance;
    }

    private MenuItem menuItemSplitArea(Player uiPlayer, Area area, Callback<Player> onCancel) {
        return new MenuItem("zone-claim-split",
                t.get("tc.menu.area.split", uiPlayer),
                (p) -> {
                    UIElement confirmDialog = UIDialogFactory.getConfirmDangerDialog(p,
                            t.get("tc.dialog.area.split.title", p),
                            t.get("tc.dialog.area.split.confirm", p)
                                    .replace("PH_AREA_NAME",
                                            area.getName() == null ? "Unnamed Area"
                                                    : area.getName()),
                            (Boolean v) -> {
                                if (v) {
                                    if (!chunkClaimUtil.splitClaim(area, p))
                                        onCancel.onCall(p);
                                    // if claim succeeds close menu
                                    else {
                                        p.hideRadialMenu(false);
                                        Area3DUtils.updateAreaFramesForAllPlayers();
                                    }
                                }
                            }, onCancel);

                    p.addUIElement(confirmDialog, UITarget.Modal);
                    p.hideRadialMenu(false);
                });
    }

    private MenuItem menuItemRenameArea(Player player, Area area, Callback<Player> onCancel) {
        final String currentName = area.getName();
        return new MenuItem("zone-claim-rename",
                t.get("tc.menu.area.rename", player),
                (p) -> {
                    UIElement renameWindow = UIDialogFactory.getTextInput(p,
                            t.get("tc.dialog.area.rename.title", p),
                            area.getName(), (String v) -> {
                                if (v.isEmpty())
                                    onCancel.onCall(p);
                                else {
                                    area.setName(v);
                                    if (LandClaim.cityService() != null
                                            && LandClaim.cityService().findCity(area.getID()).isPresent()) {
                                        LandClaim.cityService().renameCity(area.getID(), v);
                                        EconomyIntegration.WalletOperationResult renamed = LandClaim.economyIntegration()
                                                .renameCityAccount(area.getID(), v);
                                        if (!renamed.success()) LandClaim.logger().warn(
                                                "Could not update city account display name for area " + area.getID());
                                    }
                                    // WORKAROUND direct SQL
                                    // if (LandClaim.wdbAreas != null) {
                                    // LandClaim.wdbAreas.executeUpdate(
                                    // "UPDATE areas SET name ='" + v.replace("'", "''") + "' WHERE id="
                                    // + area.getID());
                                    // }
                                    p.sendTextMessage(t.get("tc.area.rennamed", p)
                                            .replace("PH_AREA_NAME", v)
                                            .replace("PH_OLD_NAME",
                                                    currentName != null ? currentName : "Unnamed Area"));
                                }
                            }, onCancel);

                    p.addUIElement(renameWindow, UITarget.Modal);
                    p.hideRadialMenu(false);

                });
    }

    private MenuItem menuItemRemoveArea(Player player, Area area, Callback<Player> onCancel) {
        return new MenuItem("zone-claim-delete",
                t.get("tc.menu.area.release", player),
                (p) -> {
                    UIElement confirmDialog = UIDialogFactory.getConfirmDangerDialog(p,
                            t.get("tc.dialog.area.release.title", p),
                            t.get("tc.dialog.area.release.confirm", p).replace("PH_AREA_NAME",
                                    area.getName() == null ? "Unnamed Area" : area.getName()),
                            (Boolean v) -> {
                                if (v) {
                                    chunkClaimUtil.releaseArea(p, area);
                                    Area3DUtils.updateAreaFramesForAllPlayers();
                                    p.sendTextMessage(t.get("tc.dialog.area.release.success", p));
                                }
                            }, onCancel);

                    p.addUIElement(confirmDialog, UITarget.Modal);
                    p.hideRadialMenu(false);

                });
    }

    private MenuItem menuItemListAreaForSale(Player player, Area area, Callback<Player> onCancel) {
        return new MenuItem("zone-sale",
                t.get("tc.menu.area.sale.list", player),
                (p) -> {
                    UIElement priceWindow = UIDialogFactory.getTextInput(p,
                            t.get("tc.dialog.area.sale.list.title", p),
                            ClaimModePolicy.allowsFreeAdministrativeTakeover(walletAvailable()) ? "0"
                                    : ClaimModePolicy.current() == ClaimMode.LAND_PRICING
                                            && LandClaim.landPriceService() != null
                                                    ? String.valueOf(LandClaim.landPriceService().areaValue(area,
                                                            s.landPriceBase, s.landPriceClusterIncrement))
                                                    : "",
                            (String value) -> {
                                if (value.isBlank()) {
                                    onCancel.onCall(p);
                                    return;
                                }
                                long price = ClaimModePolicy.allowsFreeAdministrativeTakeover(walletAvailable())
                                        ? 0L
                                        : parseNonNegativeLong(value);
                                if (price < 0 || (price == 0
                                        && !ClaimModePolicy.allowsFreeAdministrativeTakeover(walletAvailable()))
                                        || LandClaim.claimSaleListingService() == null) {
                                    p.sendTextMessage(t.get("tc.area.sale.invalid.price", p));
                                    onCancel.onCall(p);
                                    return;
                                }
                                ClaimSaleListing listing = LandClaim.claimSaleListingService()
                                        .listForSale(p, area.getID(), price);
                                if (listing == null) {
                                    p.sendTextMessage(t.get("tc.area.sale.list.failed", p));
                                } else {
                                    p.sendTextMessage(t.get("tc.area.sale.listed", p)
                                            .replace("PH_AREA_NAME", areaName(area))
                                            .replace("PH_PRICE", String.valueOf(listing.price()))
                                            .replace("PH_CURRENCY", defaultCurrency()));
                                    Area3DUtils.updateAreaFramesForAllPlayers();
                                }
                                onCancel.onCall(p);
                            },
                            onCancel);

                    p.addUIElement(priceWindow, UITarget.Modal);
                    p.hideRadialMenu(false);
                });
    }

    private MenuItem menuItemWithdrawAreaSale(Player player, Area area, ClaimSaleListing listing,
            Callback<Player> onCancel) {
        return new MenuItem("zone-claim-delete",
                t.get("tc.menu.area.sale.withdraw", player),
                (p) -> {
                    UIElement confirmDialog = UIDialogFactory.getConfirmDialog(p,
                            t.get("tc.dialog.area.sale.withdraw.title", p),
                            t.get("tc.dialog.area.sale.withdraw.confirm", p)
                                    .replace("PH_AREA_NAME", areaName(area))
                                    .replace("PH_PRICE", String.valueOf(listing.price()))
                                    .replace("PH_CURRENCY", defaultCurrency()),
                            (Boolean v) -> {
                                if (v && LandClaim.claimSaleListingService() != null
                                        && LandClaim.claimSaleListingService().withdrawActiveListing(area.getID())) {
                                    p.sendTextMessage(t.get("tc.area.sale.withdrawn", p)
                                            .replace("PH_AREA_NAME", areaName(area)));
                                    Area3DUtils.updateAreaFramesForAllPlayers();
                                }
                                onCancel.onCall(p);
                            },
                            onCancel);

                    p.addUIElement(confirmDialog, UITarget.Modal);
                    p.hideRadialMenu(false);
                });
    }

    private MenuItem menuItemBuyArea(Player player, Area area, ClaimSaleListing listing, Callback<Player> onCancel) {
        return new MenuItem("zone-sale",
                t.get("tc.menu.area.sale.buy", player),
                (p) -> {
                    UIElement confirmDialog = UIDialogFactory.getConfirmDialog(p,
                            t.get("tc.dialog.area.sale.buy.title", p),
                            t.get("tc.dialog.area.sale.buy.confirm", p)
                                    .replace("PH_AREA_NAME", areaName(area))
                                    .replace("PH_PRICE", String.valueOf(listing.price()))
                                    .replace("PH_CURRENCY", defaultCurrency()),
                            (Boolean v) -> {
                                if (v) {
                                    purchaseArea(p, area, listing);
                                }
                                onCancel.onCall(p);
                            },
                            onCancel);

                    p.addUIElement(confirmDialog, UITarget.Modal);
                    p.hideRadialMenu(false);
                });
    }

    private void purchaseArea(Player buyer, Area area, ClaimSaleListing listing) {
        if (LandClaim.claimSaleListingService() == null) {
            buyer.sendTextMessage(t.get("tc.area.sale.purchase.stale", buyer));
            return;
        }
        if (listing.price() > 0 && !walletAvailable()) {
            buyer.sendTextMessage(t.get("tc.area.sale.purchase.wallet.missing", buyer));
            return;
        }
        ClaimSaleListing currentListing = activeSaleListing(area);
        if (currentListing == null || currentListing.id() != listing.id()) {
            buyer.sendTextMessage(t.get("tc.area.sale.purchase.stale", buyer));
            return;
        }
        if (currentListing.ownerDbId() == buyer.getDbID()) {
            buyer.sendTextMessage(t.get("tc.area.sale.purchase.self", buyer));
            return;
        }
        if (!canBuyAreaWithinLimit(buyer, area)) {
            buyer.sendTextMessage(t.get("tc.area.sale.purchase.limit", buyer)
                    .replace("PH_MAX_CLAIMS", String.valueOf(chunkClaimUtil.getPlayerMaxClaims(buyer))));
            return;
        }

        String displayAreaName = areaName(area);
        Map<Integer, String> originalPermissions = capturePermissions(area);
        String reason = "LandClaim area purchase: " + displayAreaName + " (#" + area.getID() + ")";
        WalletOperationResult withdrawal = currentListing.price() == 0
                ? new WalletOperationResult(true, "")
                : LandClaim.economyIntegration().withdrawDefault(buyer.getDbID(), currentListing.price(), reason);
        if (!withdrawal.success()) {
            ChunkClaimUtil.sendPaymentFailure(buyer, walletMessage(withdrawal));
            return;
        }

        if (!chunkClaimUtil.transferAreaOwnership(area, buyer)) {
            refundBuyer(buyer, currentListing.price(), reason);
            buyer.sendTextMessage(t.get("tc.area.sale.purchase.transfer.failed", buyer));
            return;
        }

        WalletOperationResult sellerCredit = currentListing.price() == 0
                ? new WalletOperationResult(true, "")
                : LandClaim.economyIntegration().depositDefault(
                        currentListing.ownerDbId(), currentListing.price(), reason);
        if (!sellerCredit.success()) {
            rollbackAreaTransfer(area, currentListing, originalPermissions);
            refundBuyer(buyer, currentListing.price(), reason);
            buyer.sendTextMessage(t.get("tc.area.sale.purchase.seller.credit.failed", buyer)
                    .replace("PH_REASON", walletMessage(sellerCredit)));
            Area3DUtils.updateAreaFramesForAllPlayers();
            return;
        }

        if (!LandClaim.claimSaleListingService().markPurchased(area.getID(), buyer)) {
            rollbackAreaTransfer(area, currentListing, originalPermissions);
            refundBuyer(buyer, currentListing.price(), reason);
            WalletOperationResult sellerDebit = currentListing.price() == 0
                    ? new WalletOperationResult(true, "")
                    : LandClaim.economyIntegration().withdrawDefault(
                            currentListing.ownerDbId(), currentListing.price(),
                            "Rollback failed LandClaim area purchase: " + displayAreaName + " (#" + area.getID() + ")");
            if (!sellerDebit.success()) {
                LandClaim.logger().error("Could not reverse seller credit for failed claim purchase on area "
                        + area.getID() + ": " + walletMessage(sellerDebit));
            }
            buyer.sendTextMessage(t.get("tc.area.sale.purchase.listing.update.failed", buyer));
            Area3DUtils.updateAreaFramesForAllPlayers();
            return;
        }

        buyer.sendTextMessage(t.get("tc.area.sale.purchased", buyer)
                .replace("PH_AREA_NAME", displayAreaName)
                .replace("PH_PRICE", String.valueOf(currentListing.price()))
                .replace("PH_CURRENCY", defaultCurrency()));
        Player seller = Server.getPlayerByDbID(currentListing.ownerDbId());
        if (seller != null) {
            seller.sendTextMessage(t.get("tc.area.sale.sold", seller)
                    .replace("PH_AREA_NAME", displayAreaName)
                    .replace("PH_PRICE", String.valueOf(currentListing.price()))
                    .replace("PH_CURRENCY", defaultCurrency())
                    .replace("PH_PLAYER_NAME", buyer.getName()));
        }
        announceAreaPurchase(area, buyer, currentListing);
        Area3DUtils.updateAreaFramesForAllPlayers();
    }

    private boolean canBuyAreaWithinLimit(Player buyer, Area area) {
        if (s.allowClaimBuyExceedLimit) {
            return true;
        }
        int areaClaimWeight = LandClaim.llcs.getChunkInfoListByArea(area.getID()).size();
        return chunkClaimUtil.getPlayerClaimCount(buyer) + areaClaimWeight <= chunkClaimUtil.getPlayerMaxClaims(buyer);
    }

    private Map<Integer, String> capturePermissions(Area area) {
        Map<Integer, String> permissions = area.getAllPlayerPermissions();
        return permissions == null ? Map.of() : new HashMap<>(permissions);
    }

    private void rollbackAreaTransfer(Area area, ClaimSaleListing listing, Map<Integer, String> originalPermissions) {
        if (!chunkClaimUtil.transferAreaOwnership(area, listing.ownerUuid(), listing.ownerDbId())) {
            LandClaim.logger().error("Could not roll back claim ownership transfer for area " + area.getID());
            return;
        }
        Map<Integer, String> currentPermissions = area.getAllPlayerPermissions();
        if (currentPermissions != null) {
            for (Map.Entry<Integer, String> entry : List.copyOf(currentPermissions.entrySet())) {
                area.removePlayerPermission(entry.getKey());
            }
        }
        for (Map.Entry<Integer, String> entry : originalPermissions.entrySet()) {
            area.setPlayerPermission(entry.getKey(), entry.getValue());
        }
    }

    private void refundBuyer(Player buyer, long price, String reason) {
        if (price == 0) {
            return;
        }
        WalletOperationResult refund = LandClaim.economyIntegration().depositDefault(
                buyer.getDbID(), price, "Rollback " + reason);
        if (!refund.success()) {
            LandClaim.logger().error("Could not refund failed claim purchase for player " + buyer.getDbID()
                    + ": " + walletMessage(refund));
        }
    }

    private String walletMessage(WalletOperationResult result) {
        return result == null || result.message() == null || result.message().isBlank()
                ? "Wallet transaction failed."
                : result.message();
    }

    private void announceAreaPurchase(Area area, Player buyer, ClaimSaleListing listing) {
        String sellerName = Server.getLastKnownPlayerName(listing.ownerDbId());
        String resolvedSellerName = sellerName == null || sellerName.isBlank() ? "Unknown" : sellerName;
        String message = t.get("tc.discord.area.sold", DiscordConnect.botLang())
                .replace("PH_AREA_NAME", areaName(area))
                .replace("PH_PRICE", String.valueOf(listing.price()))
                .replace("PH_BUYER_NAME", buyer.getName())
                .replace("PH_SELLER_NAME", resolvedSellerName);
        DiscordConnect.sendDiscordBuyAccouncement(message);
        if (!s.enableIngameBuyAccouncement) {
            return;
        }
        for (Player onlinePlayer : Server.getAllPlayers()) {
            if (!onlinePlayer.equals(buyer)) {
                onlinePlayer.sendTextMessage(t.get("tc.announcement.area.sold", onlinePlayer)
                        .replace("PH_AREA_NAME", areaName(area))
                        .replace("PH_PRICE", String.valueOf(listing.price()))
                        .replace("PH_BUYER_NAME", buyer.getName())
                        .replace("PH_SELLER_NAME", resolvedSellerName));
            }
        }
    }

    private long parseNonNegativeLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private boolean walletAvailable() {
        return LandClaim.economyIntegration() != null && LandClaim.economyIntegration().isWalletAvailable()
                && LandClaim.economyIntegration().hasSystemAccountApi();
    }

    private int parsePositiveInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String areaName(Area area) {
        return area == null || area.getName() == null || area.getName().isBlank() ? "Unnamed Area" : area.getName();
    }

    private MenuItem menuItemCreateSpecialArea(
            Player uiPlayer,
            String iconKey,
            String labelKey,
            Area area,
            String permission,
            Callback<Player> onBack) {
        return new MenuItem(iconKey,
                t.get(labelKey, uiPlayer),
                (p) -> {
                    Area createdArea = chunkClaimUtil.claimArea(uiPlayer, area, permission, null);

                    if (createdArea != null) {
                        if (permission.equals(s.specialRenewAreaPermission)
                                && LandClaim.renewZoneConfigService() != null) {
                            LandClaim.renewZoneConfigService().save(
                                    createdArea.getID(),
                                    s.renewZoneDefaultIntervalHours,
                                    System.currentTimeMillis());
                        }
                        if (permission.equals(s.specialCityCorePermission)) {
                            if (LandClaim.cityService() == null) {
                                Server.removeArea(createdArea);
                                showCityCreationError(p, "SERVICE_UNAVAILABLE", "");
                                openSpecialAreaMenu(uiPlayer, onBack);
                                return;
                            }
                            de.omegazirkel.risingworld.landclaim.db.CityService.CityCreationEligibility eligibility =
                                    LandClaim.cityService().creationEligibility(createdArea, s.cityBaseRadius);
                            if (!eligibility.eligible()) {
                                Server.removeArea(createdArea);
                                showCityCreationError(p, eligibility.blocker().name(), "");
                                openSpecialAreaMenu(uiPlayer, onBack);
                                return;
                            }
                            CityRecord city = LandClaim.cityService().createCity(createdArea, createdArea.getName(),
                                    s.cityBaseRadius);
                            EconomyIntegration.WalletOperationResult account = city == null
                                    ? new EconomyIntegration.WalletOperationResult(false, "")
                                    : LandClaim.economyIntegration().createCityAccount(createdArea.getID(), city.name());
                            if (city == null || !account.success()) {
                                if (city != null) LandClaim.cityService().deleteCityRecord(createdArea.getID());
                                Server.removeArea(createdArea);
                                showCityCreationError(p, "WALLET", account.message());
                                openSpecialAreaMenu(uiPlayer, onBack);
                                return;
                            }
                        } else if (permission.equals(s.specialCityLeaseholdPermission)) {
                            CityRecord city = LandClaim.cityService() == null ? null
                                    : LandClaim.cityService().containingCity(createdArea.getStartChunkPosition())
                                            .orElse(null);
                            if (city == null || LandClaim.cityService().createLeasehold(createdArea, city) == null) {
                                Server.removeArea(createdArea);
                                p.sendTextMessage(t.get("tc.city.leasehold.create.failed", p));
                                openSpecialAreaMenu(uiPlayer, onBack);
                                return;
                            }
                        }
                        createSpecialAreaAnnouncement(area, uiPlayer);
                        Area3DUtils.updateAreaFramesForAllPlayers();
                    }

                    openSpecialAreaMenu(uiPlayer, onBack);
                });
    }

    private MenuItem menuItemExpandArea(Player player, Area area, Direction direction, String iconKey, String labelKey,
            Callback<Player> onCancel, Callback<Player> onBack) {
        return new MenuItem(iconKey,
                t.get(labelKey, player),
                (p) -> {
                    boolean playerClaim = s.defaultAreaPermission.equals(area.getDefaultPermission());
                    boolean paidExpansion = playerClaim && (ClaimModePolicy.current() == ClaimMode.LAND_PRICING
                            || isCityPrivateClaim(area));
                    long expansionPrice = !paidExpansion ? 0L
                            : ClaimModePolicy.current() == ClaimMode.LAND_PRICING
                                    ? chunkClaimUtil.landExpansionPrice(area, direction)
                                    : chunkClaimUtil.cityPrivateExpansionPrice(area, direction);
                    String confirmText = t.get(paidExpansion ? "tc.dialog.area.expand.paid.confirm"
                            : "tc.dialog.area.expand.confirm", p)
                                    .replace("PH_AREA_NAME", area.getName() == null ? "Unnamed Area" : area.getName())
                                    .replace("PH_DIRECTION", t.get("TC_DIRECTION_" + direction.name().toUpperCase(), p))
                                    .replace("PH_PRICE", String.valueOf(expansionPrice))
                                    .replace("PH_CURRENCY", defaultCurrency());
                    UIElement confirmDialog = UIDialogFactory.getConfirmDangerDialog(p,
                            t.get("tc.dialog.area.expand.title", p),
                            confirmText,
                            t.get(paidExpansion ? "tc.ui.btn.expand.paid" : "tc.ui.btn.yes", p),
                            (Boolean v) -> {
                                if (v) {
                                    Area expandedArea = chunkClaimUtil.expandClaim(area, direction, p);
                                    if (expandedArea != null) {
                                        expandClaimAnnouncement(expandedArea, p);
                                        Area3DUtils.updateAreaFramesForAllPlayers();
                                        openExpandAreaMenu(p, onBack, expandedArea);
                                    } else {
                                        // p.sendTextMessage(t.get("tc.dialog.area.expand.failed", p));
                                    }
                                }
                            }, onCancel);

                    p.addUIElement(confirmDialog, UITarget.Modal);
                    p.hideRadialMenu(false);

                });
    }

    private MenuItem menuItemPermissionManager(Player player, Area area, Callback<Player> onResponse) {
        return new MenuItem("menu-zone-permissions",
                t.get("tc.menu.area.permissions", player),
                (p) -> {
                    UIElement overlay = (UIElement) p.getAttribute(PermissionOverlay.ATTRIBUTE_KEY);
                    if (overlay != null) {
                        p.removeUIElement(overlay);
                    }
                    PermissionOverlay permissionOverlay = new PermissionOverlay(area, p, onResponse);
                    p.addUIElement(permissionOverlay, UITarget.Modal);
                    p.setAttribute(PermissionOverlay.ATTRIBUTE_KEY, permissionOverlay);

                    p.hideRadialMenu(false);

                });
    }

    private MenuItem menuItemAdminCleanup(Player uiPlayer, Callback<Player> onResponse) {
        return new MenuItem("menu-zone-management",
                t.get("tc.menu.admin.cleanup", uiPlayer),
                (p) -> {
                    UIElement overlay = (UIElement) p.getAttribute(AdminCleanupOverlay.ATTRIBUTE_KEY);
                    if (overlay != null) {
                        p.removeUIElement(overlay);
                    }
                    AdminCleanupOverlay cleanupOverlay = new AdminCleanupOverlay(p, cleanupService, onResponse);
                    p.addUIElement(cleanupOverlay, UITarget.Modal);
                    p.setAttribute(AdminCleanupOverlay.ATTRIBUTE_KEY, cleanupOverlay);
                    p.hideRadialMenu(false);
                });
    }

    private MenuItem menuItemCityManagement(Player player, CityRecord city, Callback<Player> onClose) {
        return new MenuItem("zone-city-core", t.get("tc.menu.city.management", player), p -> {
            UIElement existing = (UIElement) p.getAttribute(CityManagementOverlay.ATTRIBUTE_KEY);
            if (existing != null) p.removeUIElement(existing);
            CityManagementOverlay overlay = new CityManagementOverlay(p, LandClaim.cityService(),
                    LandClaim.economyIntegration(), city.areaId(), onClose);
            p.addUIElement(overlay, UITarget.Modal);
            p.setAttribute(CityManagementOverlay.ATTRIBUTE_KEY, overlay);
            p.hideRadialMenu(false);
        });
    }

    private MenuItem menuItemLeaseholdManagement(Player player, Area area, Callback<Player> onClose) {
        return new MenuItem("zone-city-leasehold", t.get("tc.menu.city.lease.admin", player), p -> {
            UIElement existing = (UIElement) p.getAttribute(LeaseholdManagementOverlay.ATTRIBUTE_KEY);
            if (existing != null) p.removeUIElement(existing);
            LeaseholdManagementOverlay overlay = new LeaseholdManagementOverlay(p, area, LandClaim.cityService(),
                    LandClaim.economyIntegration(), onClose);
            p.addUIElement(overlay, UITarget.Modal);
            p.setAttribute(LeaseholdManagementOverlay.ATTRIBUTE_KEY, overlay);
            p.hideRadialMenu(false);
        });
    }

    private MenuItem menuItemAreaConfig(Player player, Callback<Player> onResponse) {
        return new MenuItem("zone-renew-create",
                t.get("tc.menu.area.config", player),
                (p) -> openCurrentAreaConfig(p, onResponse));
    }

    private MenuItem menuItemLeaseAction(Player player, Area area, LeaseholdRecord lease, boolean purchase,
            Callback<Player> onCancel) {
        String label = purchase ? t.get("tc.menu.city.lease.buy", player) : t.get("tc.menu.city.lease.rent", player);
        long amount = purchase ? Math.max(0L, lease.purchasePrice() - lease.paidRentCredit()) : lease.dailyRent();
        return new MenuItem(purchase ? "zone-sale" : "zone-city-leasehold", label + " (" + amount + ")", p -> {
            UIElement confirm = UIDialogFactory.getConfirmDialog(p, label,
                    t.get("tc.dialog.city.lease.confirm", p)
                            .replace("PH_AREA_NAME", areaName(area)).replace("PH_PRICE", String.valueOf(amount)),
                    accepted -> {
                        if (accepted) acquireLeasehold(p, area, lease, purchase);
                        onCancel.onCall(p);
                    }, onCancel);
            p.addUIElement(confirm, UITarget.Modal);
            p.hideRadialMenu(false);
        });
    }

    private void acquireLeasehold(Player player, Area area, LeaseholdRecord lease, boolean purchase) {
        if (ClaimModePolicy.current() != ClaimMode.CITY || !walletAvailable() || LandClaim.cityService() == null) {
            player.sendTextMessage(t.get("tc.claim.error.wallet.required", player));
            return;
        }
        boolean sameOwner = lease.ownerDbId() == player.getDbID();
        if (lease.occupied() && !(sameOwner && lease.rented() && purchase)) {
            player.sendTextMessage(t.get("tc.city.lease.occupied", player));
            return;
        }
        if ((purchase && !lease.purchaseAllowed()) || (!purchase && !lease.rentAllowed())) return;
        int chunks = ChunkClaimUtil.areaToChunks(area).size();
        if (!sameOwner && chunkClaimUtil.getPlayerClaimCount(player) + chunks > chunkClaimUtil.getPlayerMaxClaims(player)) {
            player.sendTextMessage(t.get("tc.claim.error.limit", player)
                    .replace("PH_MAX_CLAIMS", String.valueOf(chunkClaimUtil.getPlayerMaxClaims(player))));
            return;
        }
        long amount = purchase ? Math.max(0L, lease.purchasePrice() - lease.paidRentCredit()) : lease.dailyRent();
        String correlation = "city-lease-" + (purchase ? "buy:" : "rent:") + UUID.randomUUID();
        if (amount > 0) LandClaim.cityService().beginEconomyOperation(correlation,
                purchase ? "LEASE_PURCHASE" : "LEASE_RENT", area.getID(), player.getDbID(), amount);
        EconomyIntegration.WalletOperationResult payment = amount == 0
                ? new EconomyIntegration.WalletOperationResult(true, "")
                : LandClaim.economyIntegration().transferPlayerToCity(player.getDbID(), lease.cityAreaId(), amount,
                        t.get(purchase ? "tc.wallet.city.lease.purchase" : "tc.wallet.city.lease.rent",
                                LandClaim.economyIntegration().walletAuditLanguage())
                                .replace("PH_AREA_NAME", areaName(area))
                                .replace("PH_AREA_ID", String.valueOf(area.getID())), correlation);
        if (!payment.success()) {
            if (amount > 0) LandClaim.cityService().updateEconomyOperation(correlation, "FAILED", area.getID(),
                    payment.message());
            ChunkClaimUtil.sendPaymentFailure(player, payment.message());
            return;
        }
        if (amount > 0) LandClaim.cityService().updateEconomyOperation(correlation, "PAID", area.getID(), "");
        boolean persisted = LandClaim.cityService().assignLeasehold(area.getID(), player.getUID(), player.getDbID(),
                purchase ? "PURCHASED" : "RENTED", purchase ? 0 : amount, LocalDate.now().toString());
        if (!persisted) {
            if (amount > 0) {
                EconomyIntegration.WalletOperationResult reversal = LandClaim.economyIntegration().reverseTransfer(
                        correlation, correlation + ":reversal", t.get("tc.wallet.city.lease.rollback",
                                LandClaim.economyIntegration().walletAuditLanguage()));
                LandClaim.cityService().updateEconomyOperation(correlation,
                        reversal.success() ? "REVERSED" : "RECONCILIATION_REQUIRED", area.getID(), reversal.message());
            }
            player.sendTextMessage(t.get("tc.city.lease.update.failed", player));
            return;
        }
        clearAreaPermissions(area);
        area.setPlayerPermission(player.getDbID(), s.ownerAreaPermission);
        if (amount > 0) LandClaim.cityService().updateEconomyOperation(correlation, "COMPLETED", area.getID(), "");
        player.sendTextMessage(t.get(purchase ? "tc.city.lease.purchased" : "tc.city.lease.rented", player)
                .replace("PH_AREA_NAME", areaName(area)));
    }

    private MenuItem menuItemReturnLeasehold(Player player, Area area, Callback<Player> onCancel) {
        return new MenuItem("zone-claim-delete", t.get("tc.menu.city.lease.return", player), p -> {
            UIElement confirm = UIDialogFactory.getConfirmDangerDialog(p, t.get("tc.menu.city.lease.return", p),
                    t.get("tc.dialog.city.lease.return", p).replace("PH_AREA_NAME", areaName(area)), accepted -> {
                        if (accepted && LandClaim.cityService() != null && LandClaim.cityService().clearLeasehold(area.getID())) {
                            clearAreaPermissions(area);
                            p.sendTextMessage(t.get("tc.city.lease.returned", p).replace("PH_AREA_NAME", areaName(area)));
                        }
                        onCancel.onCall(p);
                    }, onCancel);
            p.addUIElement(confirm, UITarget.Modal);
            p.hideRadialMenu(false);
        });
    }

    private void clearAreaPermissions(Area area) {
        Map<Integer, String> permissions = area.getAllPlayerPermissions();
        if (permissions != null) for (Integer dbId : List.copyOf(permissions.keySet())) area.removePlayerPermission(dbId);
    }

    private MenuItem menuItemExpandCity(Player player, CityRecord city, Callback<Player> onCancel) {
        long chunks = LandClaim.cityService().expansionChunkCount(city);
        long price;
        try { price = Math.multiplyExact(chunks, Math.max(0L, s.cityExpansionBasePrice)); }
        catch (ArithmeticException ex) { price = Long.MAX_VALUE; }
        long safePrice = price > LandPriceService.MAX_SAFE_INTEGER ? -1L : price;
        return new MenuItem("menu-expand-zone", t.get("tc.menu.city.expand", player)
                .replace("PH_PRICE", safePrice < 0 ? "N/A" : String.valueOf(safePrice)), p -> {
            de.omegazirkel.risingworld.landclaim.db.CityService.ExpansionEligibility eligibility =
                    LandClaim.cityService().expansionEligibility(city.areaId());
            if (!eligibility.eligible()) {
                p.sendTextMessage(cityExpansionBlockerText(p, eligibility));
                onCancel.onCall(p);
                return;
            }
            if (safePrice < 0 || LandClaim.economyIntegration().cityBalance(city.areaId()) < safePrice) {
                p.sendTextMessage(t.get("tc.city.expand.funds", p));
                onCancel.onCall(p);
                return;
            }
            UIElement confirm = UIDialogFactory.getConfirmDangerDialog(p, t.get("tc.dialog.city.expand.title", p),
                    t.get("tc.dialog.city.expand.confirm", p).replace("PH_PRICE", String.valueOf(safePrice)), accepted -> {
                        if (accepted) expandCity(p, city, safePrice);
                        onCancel.onCall(p);
                    }, onCancel);
            p.addUIElement(confirm, UITarget.Modal);
            p.hideRadialMenu(false);
        });
    }

    private MenuItem menuItemDeleteCity(Player player, Area core, CityRecord city, Callback<Player> onCancel) {
        return new MenuItem("zone-claim-delete", t.get("tc.menu.city.delete", player), p -> {
            UIElement confirm = UIDialogFactory.getConfirmDangerDialog(p, t.get("tc.menu.city.delete", p),
                    t.get("tc.dialog.city.delete", p).replace("PH_CITY_NAME", city.name()), accepted -> {
                        if (accepted) deleteCity(p, core, city);
                        onCancel.onCall(p);
                    }, onCancel);
            p.addUIElement(confirm, UITarget.Modal); p.hideRadialMenu(false);
        });
    }

    private void deleteCity(Player player, Area core, CityRecord city) {
        List<LeaseholdRecord> leases = LandClaim.cityService().leaseholdsForCity(city.areaId());
        if (leases.stream().anyMatch(LeaseholdRecord::occupied)) {
            player.showErrorMessageBox(t.get("tc.menu.city.delete", player), t.get("tc.city.delete.occupied", player));
            return;
        }
        String correlation = "city-delete:" + city.areaId();
        EconomyIntegration.WalletOperationResult closed = LandClaim.economyIntegration().closeCityAccount(
                city.areaId(), t.get("tc.wallet.city.dissolution", LandClaim.economyIntegration().walletAuditLanguage())
                        .replace("PH_CITY_NAME", city.name()).replace("PH_AREA_ID", String.valueOf(city.areaId())),
                correlation);
        if (!closed.success()) {
            player.showErrorMessageBox(t.get("tc.menu.city.delete", player), t.get("tc.city.delete.account.failed", player)
                    .replace("PH_REASON", closed.message()));
            return;
        }
        for (LeaseholdRecord lease : leases) {
            Area area = Server.getArea(lease.areaId());
            if (area != null) Server.removeArea(area);
            LandClaim.cityService().deleteLeaseholdRecord(lease.areaId());
        }
        LandClaim.cityService().deleteCityRecord(city.areaId());
        Server.removeArea(core);
        player.sendTextMessage(t.get("tc.city.deleted", player).replace("PH_CITY_NAME", city.name()));
    }

    private MenuItem menuItemDeleteLeasehold(Player player, Area area, LeaseholdRecord lease,
            Callback<Player> onCancel) {
        return new MenuItem("zone-claim-delete", t.get("tc.menu.city.lease.delete", player), p -> {
            UIElement confirm = UIDialogFactory.getConfirmDangerDialog(p, t.get("tc.menu.city.lease.delete", p),
                    t.get("tc.dialog.city.lease.delete", p).replace("PH_AREA_NAME", areaName(area)), accepted -> {
                        if (accepted) {
                            if (lease.occupied()) {
                                Player owner = Server.getPlayerByDbID(lease.ownerDbId());
                                String ownerName = Server.getLastKnownPlayerName(lease.ownerDbId());
                                String language = owner == null
                                        ? LandClaim.cityService().playerLanguage(lease.ownerDbId()).orElse("en")
                                        : de.omegazirkel.risingworld.OZTools.getPlayerLanguage(owner);
                                String notice = t.get("tc.city.lease.expropriated", language)
                                        .replace("PH_AREA_NAME", areaName(area));
                                long pendingId = LandClaim.cityService().addPendingNotification(lease.ownerDbId(),
                                        "tc.city.lease.expropriated", areaName(area));
                                boolean sent = LandClaim.economyIntegration().sendMail(lease.ownerDbId(),
                                        ownerName == null ? "Player" : ownerName,
                                        t.get("tc.city.mail.subject", language),
                                        notice, "city-expropriation:" + area.getID() + ":" + UUID.randomUUID());
                                if (sent && pendingId > 0) LandClaim.cityService().deletePendingNotification(pendingId);
                            }
                            clearAreaPermissions(area);
                            LandClaim.cityService().deleteLeaseholdRecord(area.getID());
                            Server.removeArea(area);
                            p.sendTextMessage(t.get("tc.city.lease.deleted", p));
                        }
                        onCancel.onCall(p);
                    }, onCancel);
            p.addUIElement(confirm, UITarget.Modal); p.hideRadialMenu(false);
        });
    }

    private void expandCity(Player player, CityRecord city, long price) {
        String correlation = "city-expand:" + city.areaId() + ":" + (city.radius() + 1);
        EconomyIntegration.WalletOperationResult payment = price == 0
                ? new EconomyIntegration.WalletOperationResult(true, "")
                : LandClaim.economyIntegration().transferCityToWorld(city.areaId(), price,
                        t.get("tc.wallet.city.expansion", LandClaim.economyIntegration().walletAuditLanguage())
                                .replace("PH_CITY_NAME", city.name()), correlation);
        if (!payment.success() || !LandClaim.cityService().expandCity(city.areaId())) {
            if (payment.success() && price > 0) LandClaim.economyIntegration().reverseTransfer(
                    correlation, correlation + ":reversal", t.get("tc.wallet.city.expansion.rollback",
                            LandClaim.economyIntegration().walletAuditLanguage()));
            player.sendTextMessage(t.get("tc.city.expand.failed", player));
            return;
        }
        player.sendTextMessage(t.get("tc.city.expanded", player).replace("PH_RADIUS", String.valueOf(city.radius() + 1)));
    }

    private String cityExpansionBlockerText(Player player,
            de.omegazirkel.risingworld.landclaim.db.CityService.ExpansionEligibility eligibility) {
        return t.get("TC_UI_CITY_EXPAND_BLOCKED_" + eligibility.blocker().name(), player);
    }

    private void showCityCreationError(Player player, String blocker, String detail) {
        String key = "TC_CITY_CREATE_FAILED_" + blocker;
        String message = t.get(key, player);
        if (message.equals(key)) message = t.get("tc.city.create.failed", player);
        player.showErrorMessageBox(t.get("tc.dialog.city.create.title", player),
                message.replace("PH_REASON", detail == null || detail.isBlank() ? "-" : detail));
    }

    public void openCurrentAreaConfig(Player player, Callback<Player> onCancel) {
        Area area = player.getCurrentArea();
        if (!isRenewArea(area)) {
            player.sendTextMessage(t.get("tc.area.config.unavailable", player));
            onCancel.onCall(player);
            return;
        }
        openRenewZoneConfig(player, area, onCancel);
    }

    private void openRenewZoneConfig(Player player, Area area, Callback<Player> onCancel) {
        if (LandClaim.renewZoneConfigService() == null) {
            player.sendTextMessage(t.get("tc.area.config.unavailable", player));
            onCancel.onCall(player);
            return;
        }
        RenewZoneConfig config = LandClaim.renewZoneConfigService()
                .find(area.getID())
                .orElse(new RenewZoneConfig(area.getID(), s.renewZoneDefaultIntervalHours, 0L));
        UIElement intervalWindow = UIDialogFactory.getTextInput(player,
                t.get("tc.dialog.renew.zone.config.title", player),
                String.valueOf(config.intervalHours()),
                (String value) -> {
                    int intervalHours = parsePositiveInt(value);
                    if (intervalHours <= 0) {
                        player.sendTextMessage(t.get("tc.renew.zone.config.invalid.interval", player));
                        onCancel.onCall(player);
                        return;
                    }
                    RenewZoneConfig saved = LandClaim.renewZoneConfigService().save(
                            area.getID(),
                            intervalHours,
                            config.lastResetAt());
                    if (saved == null) {
                        player.sendTextMessage(t.get("tc.renew.zone.config.save.failed", player));
                    } else {
                        player.sendTextMessage(t.get("tc.renew.zone.config.saved", player)
                                .replace("PH_INTERVAL_HOURS", String.valueOf(saved.intervalHours())));
                    }
                    onCancel.onCall(player);
                },
                onCancel);

        player.addUIElement(intervalWindow, UITarget.Modal);
        player.hideRadialMenu(false);
    }

    public void openSpecialAreaMenu(Player uiPlayer, Callback<Player> onBack) {
        List<MenuItem> menuItems = new ArrayList<>();

        Area currentArea = uiPlayer.getCurrentArea();

        Callback<Player> onBackReopen = (Player player) -> openSpecialAreaMenu(player, onBack);

        if (currentArea == null) {
            Area virtualArea = ChunkClaimUtil.getVirtualAreaFromChunkVector(uiPlayer.getChunkPosition());
            // special areas
            menuItems.add(menuItemCreateSpecialArea(uiPlayer, "zone-neutral-create", "tc.menu.special.area.create",
                    virtualArea,
                    s.specialAreaPermission, onBack));
            menuItems.add(menuItemCreateSpecialArea(uiPlayer, "zone-static-create", "tc.menu.special.area.static",
                    virtualArea, s.specialStaticAreaPermission, onBack));
            menuItems.add(menuItemCreateSpecialArea(uiPlayer, "zone-combat-create", "tc.menu.special.area.pvp",
                    virtualArea, s.specialPvPAreaPermission, onBack));
            menuItems.add(
                    menuItemCreateSpecialArea(uiPlayer, "zone-rest-create", "tc.menu.special.area.rest", virtualArea,
                            s.specialRestAreaPermission, onBack));
            menuItems
                    .add(menuItemCreateSpecialArea(uiPlayer, "zone-trap-create", "tc.menu.special.area.trap",
                            virtualArea,
                            s.specialTrapAreaPermission, onBack));
            menuItems
                    .add(menuItemCreateSpecialArea(uiPlayer, "zone-renew-create", "tc.menu.special.area.renew",
                            virtualArea,
                            s.specialRenewAreaPermission, onBack));
            if (ClaimModePolicy.current() == ClaimMode.CITY && walletAvailable()) {
                menuItems.add(menuItemCreateSpecialArea(uiPlayer, "zone-city-core", "tc.menu.city.core.create",
                        virtualArea, s.specialCityCorePermission, onBack));
                menuItems.add(menuItemCreateSpecialArea(uiPlayer, "zone-city-leasehold",
                        "tc.menu.city.leasehold.create", virtualArea, s.specialCityLeaseholdPermission, onBack));
            }
        }
        // show extend menu if area exist
        if (currentArea != null && (LandClaim.cityService() == null
                || LandClaim.cityService().findCity(currentArea.getID()).isEmpty())) {
            menuItems.add(new MenuItem("menu-expand-zone",
                    t.get("tc.menu.area.expand.option", uiPlayer),
                    (p) -> {
                        openExpandAreaMenu(p, onBackReopen);
                    }));
        }

        menuItems.add(MenuItem.closeMenu(uiPlayer));
        menuItems.add(MenuItem.backMenu(uiPlayer, onBack));

        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

    public void openAdminMenu(Player uiPlayer, Callback<Player> onBack) {
        Boolean developerMode = (Boolean) uiPlayer.getAttribute(LandClaimPlayerPluginSettings.DEVELOPER_MODE_KEY);
        Area currentArea = uiPlayer.getCurrentArea();
        String defaultPermission = currentArea == null ? null : currentArea.getDefaultPermission();
        Boolean isDefaultArea = defaultPermission != null && defaultPermission.equals(s.defaultAreaPermission);
        Integer chunkCount = currentArea == null ? 0 : ChunkClaimUtil.areaToChunks(currentArea).size();

        List<MenuItem> menuItems = new ArrayList<>();

        Callback<Player> onBackReopen = (Player player) -> openAdminMenu(player, onBack);

        if (developerMode) {

            menuItems.add(new MenuItem("menu-debug",
                    t.get("tc.menu.admin.debug", uiPlayer),
                    (p) -> {
                        Area3DUtils.refreshAreaFramesForPlayer(p);
                        chunkClaimUtil.idleChunk(p);
                        // if we do not reopen the menu it seems to be frozen and unclickable
                        openAdminMenu(p, onBack);
                        Area[] allAreas = Server.getAllAreas();
                        if (allAreas != null)
                            for (Area sa : allAreas) {
                                p.sendTextMessage(
                                        sa.getName() + " :: " + sa.getID() + " :: " + sa.getDefaultPermission());
                            }
                        else
                            p.sendTextMessage("no areas found Server.getAllAreas() == null");
                    }));
            menuItems.add(new MenuItem("tools",
                    t.get("tc.menu.admin.sync.repair", uiPlayer),
                    (p) -> {
                        if (p.isAdmin()) {
                            chunkClaimUtil.syncAndRepairAreas();
                            p.sendTextMessage(t.get("tc.cmd.repair.success"));
                        } else
                            p.sendTextMessage(t.get("tc.cmd.repair.err.permission"));
                        // if we do not reopen the menu it seems to be frozen and unclickable
                        openAdminMenu(p, onBack);
                    }));
        }
        menuItems.add(menuItemAdminCleanup(uiPlayer, onBackReopen));
        if (currentArea != null) {
            CityRecord city = LandClaim.cityService() == null ? null
                    : LandClaim.cityService().findCity(currentArea.getID()).orElse(null);
            LeaseholdRecord lease = LandClaim.cityService() == null ? null
                    : LandClaim.cityService().findLeasehold(currentArea.getID()).orElse(null);
            if (ClaimModePolicy.current() == ClaimMode.CITY && city != null) {
                menuItems.add(menuItemCityManagement(uiPlayer, city, onBackReopen));
                menuItems.add(menuItemDeleteCity(uiPlayer, currentArea, city, onBackReopen));
            }
            if (ClaimModePolicy.current() == ClaimMode.ADMINISTRATIVE && Boolean.TRUE.equals(isDefaultArea)) {
                menuItems.add(new MenuItem("menu-expand-zone", t.get("tc.menu.area.expand.option", uiPlayer),
                        p -> openExpandAreaMenu(p, onBackReopen)));
            }
            if (ClaimModePolicy.current() == ClaimMode.CITY && lease != null) {
                menuItems.add(menuItemLeaseholdManagement(uiPlayer, currentArea, onBackReopen));
                menuItems.add(menuItemDeleteLeasehold(uiPlayer, currentArea, lease, onBackReopen));
            }
            menuItems.add(menuItemRenameArea(uiPlayer, currentArea, onBackReopen));
            if (isRenewArea(currentArea)) {
                menuItems.add(menuItemAreaConfig(uiPlayer, onBackReopen));
            }
            menuItems.add(menuItemPermissionManager(uiPlayer, currentArea, onBackReopen));
            if (city == null && lease == null) menuItems.add(menuItemRemoveArea(uiPlayer, currentArea, onBackReopen));
        }
        if (!isDefaultArea)
            menuItems.add(new MenuItem("menu-zone-special",
                    t.get("tc.menu.special.area", uiPlayer),
                    (p) -> openSpecialAreaMenu(p, onBackReopen)));
        LeaseholdRecord currentLease = LandClaim.cityService() == null || currentArea == null ? null
                : LandClaim.cityService().findLeasehold(currentArea.getID()).orElse(null);
        if (chunkCount > 1 && (currentLease == null || !currentLease.occupied()))
            menuItems.add(menuItemSplitArea(uiPlayer, currentArea, onBackReopen));

        menuItems.add(MenuItem.closeMenu(uiPlayer));
        menuItems.add(MenuItem.backMenu(uiPlayer, onBack));

        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

    public void openClaimOptionsMenu(Player uiPlayer, Callback<Player> onBack) {
        List<MenuItem> menuItems = new ArrayList<>();
        Area currentArea = uiPlayer.getCurrentArea();

        String areaPermission = currentArea == null ? null : currentArea.getPlayerPermission(uiPlayer);
        boolean isOwner = areaPermission != null && areaPermission.equals(s.ownerAreaPermission);
        Integer chunkCount = currentArea == null ? 0 : ChunkClaimUtil.areaToChunks(currentArea).size();

        Callback<Player> onBackReopen = (Player player) -> openClaimOptionsMenu(player, onBack);

        LeaseholdRecord lease = LandClaim.cityService() == null || currentArea == null ? null
                : LandClaim.cityService().findLeasehold(currentArea.getID()).orElse(null);
        if (ClaimModePolicy.current() == ClaimMode.CITY && lease != null) {
            if (!lease.occupied()) {
                if (lease.rentAllowed()) menuItems.add(menuItemLeaseAction(uiPlayer, currentArea, lease, false, onBackReopen));
                if (lease.purchaseAllowed()) menuItems.add(menuItemLeaseAction(uiPlayer, currentArea, lease, true, onBackReopen));
            } else if (lease.ownerDbId() == uiPlayer.getDbID()) {
                menuItems.add(menuItemPermissionManager(uiPlayer, currentArea, onBackReopen));
                menuItems.add(menuItemRenameArea(uiPlayer, currentArea, onBackReopen));
                if (lease.rented() && lease.purchaseAllowed())
                    menuItems.add(menuItemLeaseAction(uiPlayer, currentArea, lease, true, onBackReopen));
                menuItems.add(menuItemReturnLeasehold(uiPlayer, currentArea, onBackReopen));
            }
            menuItems.add(MenuItem.closeMenu(uiPlayer));
            menuItems.add(MenuItem.backMenu(uiPlayer, onBack));
            PluginMenuManager.showMenu(uiPlayer, menuItems);
            return;
        }

        if (isOwner) {
            if (ClaimModePolicy.mayPlayerResizeOrRelease(uiPlayer.isAdmin())) {
                menuItems.add(new MenuItem("menu-expand-zone",
                        t.get("tc.menu.area.expand.option", uiPlayer),
                        (p) -> openExpandAreaMenu(p, onBackReopen)));
                if (chunkCount > 1)
                    menuItems.add(menuItemSplitArea(uiPlayer, currentArea, onBackReopen));
            }

            menuItems.add(menuItemPermissionManager(uiPlayer, currentArea, onBackReopen));
            menuItems.add(menuItemRenameArea(uiPlayer, currentArea, onBackReopen));
            if (ClaimModePolicy.salesAvailable(s.allowClaimSale, walletAvailable())
                    && LandClaim.claimSaleListingService() != null) {
                ClaimSaleListing listing = LandClaim.claimSaleListingService().activeListing(currentArea.getID())
                        .orElse(null);
                menuItems.add(listing == null
                        ? menuItemListAreaForSale(uiPlayer, currentArea, onBackReopen)
                        : menuItemWithdrawAreaSale(uiPlayer, currentArea, listing, onBackReopen));
            }
            if (ClaimModePolicy.mayPlayerResizeOrRelease(uiPlayer.isAdmin()))
                menuItems.add(menuItemRemoveArea(uiPlayer, currentArea, onBackReopen));
        }

        menuItems.add(MenuItem.closeMenu(uiPlayer));
        menuItems.add(MenuItem.backMenu(uiPlayer, onBack));

        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

    private void openLeaseholdAdminMenu(Player player, Area area, Callback<Player> onBack) {
        LeaseholdRecord lease = LandClaim.cityService() == null ? null
                : LandClaim.cityService().findLeasehold(area.getID()).orElse(null);
        if (lease == null) { onBack.onCall(player); return; }
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("zone-sale", t.get("tc.menu.city.lease.purchase.price", player)
                .replace("PH_PRICE", String.valueOf(lease.purchasePrice())), p -> {
            UIElement input = UIDialogFactory.getTextInput(p, t.get("tc.menu.city.lease.purchase.price", p),
                    String.valueOf(lease.purchasePrice()), value -> {
                        long price = parseNonNegativeLong(value);
                        if (price >= 0) LandClaim.cityService().configureLeasehold(area.getID(), price,
                                lease.dailyRent(), lease.purchaseAllowed(), lease.rentAllowed());
                        openLeaseholdAdminMenu(p, area, onBack);
                    }, onBack);
            p.addUIElement(input, UITarget.Modal); p.hideRadialMenu(false);
        }));
        items.add(new MenuItem("zone-city-leasehold", t.get("tc.menu.city.lease.daily.rent", player)
                .replace("PH_PRICE", String.valueOf(lease.dailyRent())), p -> {
            UIElement input = UIDialogFactory.getTextInput(p, t.get("tc.menu.city.lease.daily.rent", p),
                    String.valueOf(lease.dailyRent()), value -> {
                        long rent = parseNonNegativeLong(value);
                        if (rent >= 0) LandClaim.cityService().configureLeasehold(area.getID(), lease.purchasePrice(),
                                rent, lease.purchaseAllowed(), lease.rentAllowed());
                        openLeaseholdAdminMenu(p, area, onBack);
                    }, onBack);
            p.addUIElement(input, UITarget.Modal); p.hideRadialMenu(false);
        }));
        items.add(new MenuItem("zone-sale", t.get(lease.purchaseAllowed()
                ? "tc.menu.city.lease.disable.buy" : "tc.menu.city.lease.enable.buy", player), p -> {
            LandClaim.cityService().configureLeasehold(area.getID(), lease.purchasePrice(), lease.dailyRent(),
                    !lease.purchaseAllowed(), lease.rentAllowed());
            openLeaseholdAdminMenu(p, area, onBack);
        }));
        items.add(new MenuItem("zone-city-leasehold", t.get(lease.rentAllowed()
                ? "tc.menu.city.lease.disable.rent" : "tc.menu.city.lease.enable.rent", player), p -> {
            LandClaim.cityService().configureLeasehold(area.getID(), lease.purchasePrice(), lease.dailyRent(),
                    lease.purchaseAllowed(), !lease.rentAllowed());
            openLeaseholdAdminMenu(p, area, onBack);
        }));
        items.add(MenuItem.closeMenu(player));
        items.add(MenuItem.backMenu(player, onBack));
        PluginMenuManager.showMenu(player, items);
    }

    public void openExpandAreaMenu(Player uiPlayer, Callback<Player> onBack) {
        openExpandAreaMenu(uiPlayer, onBack, null);
    }

    public void openExpandAreaMenu(Player uiPlayer, Callback<Player> onBack, Area targetArea) {
        List<MenuItem> menuItems = new ArrayList<>();
        Area currentArea = uiPlayer.getCurrentArea();
        if (targetArea != null)
            currentArea = targetArea;

        Callback<Player> onBackReopen = (Player player) -> openExpandAreaMenu(player, onBack);

        if (currentArea != null) {
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.NORTH, "zone-expand-north",
                    "tc.menu.area.expand.north", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.EAST, "zone-expand-east",
                    "tc.menu.area.expand.east", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.SOUTH, "zone-expand-south",
                    "tc.menu.area.expand.south", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.WEST, "zone-expand-west",
                    "tc.menu.area.expand.west", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.UP, "zone-expand-up",
                    "tc.menu.area.expand.up",
                    onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.DOWN, "zone-expand-down",
                    "tc.menu.area.expand.down", onBackReopen, onBackReopen));
        }
        menuItems.add(MenuItem.closeMenu(uiPlayer));
        menuItems.add(MenuItem.backMenu(uiPlayer, onBack));

        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

    private void expandClaimAnnouncement(Area area, Player player) {
        player.sendTextMessage(t.get("tc.claim.expanded", player).replace("PH_AREA_NAME", area.getName()));
        String message = t.get("tc.discord.area.expanded", DiscordConnect.botLang())
                .replace("PH_AREA_NAME", area.getName())
                .replace("PH_CHUNK_POS", area.getStartChunkPosition() + "")
                .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(player.getDbID()));
        DiscordConnect.sendDiscordExpandAnnouncement(message);
        // message to all players
        for (Player onlinePlayer : Server.getAllPlayers()) {
            if (!onlinePlayer.equals(player))
                onlinePlayer.sendTextMessage(
                        t.get("tc.announcement.area.expanded", onlinePlayer)
                                .replace("PH_AREA_NAME", area.getName())
                                .replace("PH_CHUNK_POS", area.getStartChunkPosition().toString())
                                .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(player.getDbID())));
        }
    }

    private void createSpecialAreaAnnouncement(Area area, Player player) {
        player.sendYellMessage(t.get("tc.area.special.created", player), 5, false);
        // Discord announcement
        String message = t.get("tc.discord.area.special.created", DiscordConnect.botLang())
                .replace("PH_AREA_NAME", area.getName())
                .replace("PH_CHUNK_POS", area.getStartChunkPosition().toString())
                .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(player.getDbID()));
        DiscordConnect.sendDiscordClaimAnnouncement(message);
        // Server announcement
        for (Player onlinePlayer : Server.getAllPlayers()) {
            if (!onlinePlayer.equals(player))
                onlinePlayer.sendTextMessage(
                        t.get("tc.announcement.area.special.created", onlinePlayer)
                                .replace("PH_AREA_NAME", area.getName())
                                .replace("PH_CHUNK_POS", area.getStartChunkPosition().toString())
                                .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(player.getDbID())));
        }
        Area3DUtils.updateAreaFramesForAllPlayers();
    }

    public void openVisibilitySettingsMenu(Player uiPlayer) {
        Boolean showCurrentChunkFrame = (Boolean) uiPlayer
                .getAttribute(LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY);
        Boolean showOwnedAreaFrames = (Boolean) uiPlayer
                .getAttribute(LandClaimPlayerPluginSettings.SHOW_OWNED_AREA_FRAMES_KEY);
        Boolean showOtherAreaFrames = (Boolean) uiPlayer
                .getAttribute(LandClaimPlayerPluginSettings.SHOW_OTHER_AREA_FRAMES_KEY);

        List<MenuItem> menuItems = new ArrayList<>();

        menuItems.add(new MenuItem(showCurrentChunkFrame ? "zone-visibility-current-on" : "zone-visibility-current-off",
                t.get(showCurrentChunkFrame ? "tc.menu.visibility.current.hide" : "tc.menu.visibility.current.show",
                        uiPlayer),
                (p) -> {
                    Vector3i chunkPos = p.getChunkPosition();
                    Area area = ChunkClaimUtil.getVirtualAreaFromChunkVector(chunkPos);
                    LandClaimPlayerPluginSettings.setBooleanValue(p,
                            LandClaimPlayerPluginSettings.SHOW_CURRENT_CHUNK_FRAME_KEY, !showCurrentChunkFrame);
                    Area3DUtils.updateCurrentChunkFrameForPlayer(p, showCurrentChunkFrame ? (Area) null : area);
                    // if we do not reopen the menu it seems to be frozen and unclickable
                    openVisibilitySettingsMenu(p);
                }));

        menuItems.add(new MenuItem(showOwnedAreaFrames ? "zone-visibility-owned-on" : "zone-visibility-owned-off",
                t.get(showOwnedAreaFrames ? "tc.menu.visibility.owned.hide" : "tc.menu.visibility.owned.show",
                        uiPlayer),
                (p) -> {
                    LandClaimPlayerPluginSettings.setBooleanValue(p,
                            LandClaimPlayerPluginSettings.SHOW_OWNED_AREA_FRAMES_KEY, !showOwnedAreaFrames);
                    // if we do not reopen the menu it seems to be frozen and unclickable
                    openVisibilitySettingsMenu(p);
                    Area3DUtils.updateAreaFramesForPlayer(p);
                }));

        menuItems.add(new MenuItem(showOtherAreaFrames ? "zone-visibility-others-on" : "zone-visibility-others-off",
                t.get(showOtherAreaFrames ? "tc.menu.visibility.other.hide" : "tc.menu.visibility.other.show",
                        uiPlayer),
                (p) -> {
                    LandClaimPlayerPluginSettings.setBooleanValue(p,
                            LandClaimPlayerPluginSettings.SHOW_OTHER_AREA_FRAMES_KEY, !showOtherAreaFrames);
                    // if we do not reopen the menu it seems to be frozen and unclickable
                    openVisibilitySettingsMenu(p);
                    Area3DUtils.updateAreaFramesForPlayer(p);
                }));

        menuItems.add(MenuItem.closeMenu(uiPlayer));
        menuItems.add(MenuItem.backMenu(uiPlayer, (p) -> openMainMenu(p)));

        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

    public void openMainMenu(Player uiPlayer) {
        Area currentArea = uiPlayer.getCurrentArea();
        Boolean canClaimArea = chunkClaimUtil.canPlayerClaimArea(uiPlayer, currentArea, null);
        ClaimSaleListing activeSaleListing = activeSaleListing(currentArea);
        boolean currentAreaOwner = isOwner(uiPlayer, currentArea);

        List<MenuItem> menuItems = new ArrayList<>();

        menuItems.add(new MenuItem("menu-zone-visibility",
                t.get("tc.menu.visibility", uiPlayer),
                (p) -> {
                    openVisibilitySettingsMenu(p);
                }));

        if (uiPlayer.isAdmin())
            menuItems.add(new MenuItem("menu-zone-admin",
                    t.get("tc.menu.admin", uiPlayer),
                    (p) -> {
                        openAdminMenu(p, (Player player) -> openMainMenu(player));
                    }));

        if (canClaimArea)
            menuItems.add(new MenuItem("zone-claim-create",
                    claimMenuLabel(uiPlayer),
                    (p) -> {
                        if (isCityPrivateClaim(ChunkClaimUtil.getVirtualAreaFromChunkVector(p.getChunkPosition()))) {
                            long price = cityPrivateClaimPrice(p);
                            UIElement confirm = UIDialogFactory.getConfirmDangerDialog(p,
                                    t.get("tc.menu.land.buy", p).replace("PH_PRICE", String.valueOf(price))
                                            .replace("PH_CURRENCY", defaultCurrency()),
                                    t.get("tc.dialog.area.claim.paid.confirm", p)
                                            .replace("PH_PRICE", String.valueOf(price))
                                            .replace("PH_CURRENCY", defaultCurrency()),
                                    t.get("tc.menu.land.buy", p).replace("PH_PRICE", String.valueOf(price))
                                            .replace("PH_CURRENCY", defaultCurrency()),
                                    accepted -> { if (accepted) claimCurrentChunk(p); }, player -> openMainMenu(player));
                            p.addUIElement(confirm, UITarget.Modal); p.hideRadialMenu(false);
                            return;
                        }
                        claimCurrentChunk(p);
                    }));
        if (activeSaleListing != null && !currentAreaOwner) {
            menuItems.add(menuItemBuyArea(uiPlayer, currentArea, activeSaleListing,
                    (Player player) -> openMainMenu(player)));
        }
        if (currentArea != null) {
            menuItems.add(new MenuItem("zone-claim-create",
                    t.get("tc.menu.area.option", uiPlayer),
                    (p) -> {
                        openClaimOptionsMenu(p, (Player player) -> openMainMenu(player));
                    }));
        }

        menuItems.add(PluginInfoStatusProviders.menuItem(t.get("tc.menu.info.status", uiPlayer), LandClaim.name));
        menuItems.add(MenuItem.closeMenu(uiPlayer));

        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

    private void claimCurrentChunk(Player p) {
        Area createdArea = chunkClaimUtil.claimArea(p,
                ChunkClaimUtil.getVirtualAreaFromChunkVector(p.getChunkPosition()));
        if (createdArea != null) {
                            p.sendYellMessage(t.get("tc.claim.congratulation", p), 5, true);
                            // Discord announcement
                            String message = t.get("tc.discord.area.claimed", DiscordConnect.botLang())
                                    .replace("PH_AREA_NAME", createdArea.getName())
                                    .replace("PH_CHUNK_POS", createdArea.getStartChunkPosition().toString())
                                    .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(p.getDbID()));
                            DiscordConnect.sendDiscordClaimAnnouncement(message);
                            // Server announcement
                            for (Player onlinePlayer : Server.getAllPlayers()) {
                                if (!onlinePlayer.equals(p))
                                    onlinePlayer.sendTextMessage(
                                            t.get("tc.announcement.area.claimed", onlinePlayer)
                                                    .replace("PH_AREA_NAME", createdArea.getName())
                                                    .replace("PH_CHUNK_POS",
                                                            createdArea.getStartChunkPosition().toString())
                                                    .replace("PH_PLAYER_NAME",
                                                            Server.getLastKnownPlayerName(p.getDbID())));
                            }
                            Area3DUtils.updateAreaFramesForAllPlayers();
        }
        openMainMenu(p);
    }

    private ClaimSaleListing activeSaleListing(Area area) {
        if (!ClaimModePolicy.salesAvailable(s.allowClaimSale, walletAvailable()) || area == null
                || LandClaim.claimSaleListingService() == null) {
            return null;
        }
        return LandClaim.claimSaleListingService().activeListing(area.getID()).orElse(null);
    }

    private String claimMenuLabel(Player player) {
        if (ClaimModePolicy.current() == ClaimMode.CITY) {
            Area virtualArea = ChunkClaimUtil.getVirtualAreaFromChunkVector(player.getChunkPosition());
            if (isCityPrivateClaim(virtualArea)) {
                return t.get("tc.menu.land.buy", player).replace("PH_PRICE", String.valueOf(cityPrivateClaimPrice(player)))
                        .replace("PH_CURRENCY", defaultCurrency());
            }
        }
        if (ClaimModePolicy.current() != ClaimMode.LAND_PRICING || LandClaim.landPriceService() == null)
            return t.get("tc.menu.claim", player);
        long price = LandClaim.landPriceService().price(player.getChunkPosition(), s.landPriceBase);
        return t.get("tc.menu.land.buy", player).replace("PH_PRICE", String.valueOf(price))
                .replace("PH_CURRENCY", defaultCurrency());
    }

    private boolean isCityPrivateClaim(Area area) {
        return ClaimModePolicy.current() == ClaimMode.CITY && area != null
                && s.defaultAreaPermission.equals(area.getDefaultPermission()) && LandClaim.cityService() != null
                && LandClaim.cityService().containingCity(area.getStartChunkPosition()).isPresent();
    }

    private long cityPrivateClaimPrice(Player player) {
        if (LandClaim.cityService() == null) return 0L;
        CityRecord city = LandClaim.cityService().containingCity(player.getChunkPosition()).orElse(null);
        return city == null ? 0L : city.privateClaimPriceOverride() == null
                ? Math.max(0L, s.cityPrivateClaimPrice) : Math.max(0L, city.privateClaimPriceOverride());
    }

    private String defaultCurrency() {
        return LandClaim.economyIntegration() == null ? ""
                : LandClaim.economyIntegration().defaultCurrencyIdentifier();
    }

    private boolean isOwner(Player player, Area area) {
        String areaPermission = area == null ? null : area.getPlayerPermission(player);
        return areaPermission != null && areaPermission.equals(s.ownerAreaPermission);
    }

    private boolean isRenewArea(Area area) {
        return area != null && s.specialRenewAreaPermission.equals(area.getDefaultPermission());
    }

}
