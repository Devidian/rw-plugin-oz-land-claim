package de.omegazirkel.risingworld.landclaim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.EconomyIntegration.WalletOperationResult;
import de.omegazirkel.risingworld.landclaim.db.ClaimSaleListing;
import de.omegazirkel.risingworld.landclaim.db.RenewZoneConfig;
import de.omegazirkel.risingworld.landclaim.ui.AdminCleanupOverlay;
import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil.Direction;
import de.omegazirkel.risingworld.landclaim.ui.LandClaimPlayerPluginSettings;
import de.omegazirkel.risingworld.landclaim.ui.PermissionOverlay;
import de.omegazirkel.risingworld.landclaim.ui.UIDialogFactory;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.CursorManager;
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
                t.get("TC_MENU_AREA_SPLIT", uiPlayer),
                (p) -> {
                    UIElement confirmDialog = UIDialogFactory.getConfirmDangerDialog(p,
                            t.get("TC_DIALOG_AREA_SPLIT_TITLE", p),
                            t.get("TC_DIALOG_AREA_SPLIT_CONFIRM", p)
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

                    p.addUIElement(confirmDialog, UITarget.HUD);
                    CursorManager.show(p);
                    p.hideRadialMenu(false);
                });
    }

    private MenuItem menuItemRenameArea(Player player, Area area, Callback<Player> onCancel) {
        final String currentName = area.getName();
        return new MenuItem("zone-claim-rename",
                t.get("TC_MENU_AREA_RENAME", player),
                (p) -> {
                    UIElement renameWindow = UIDialogFactory.getTextInput(p,
                            t.get("TC_DIALOG_AREA_RENAME_TITLE", p),
                            area.getName(), (String v) -> {
                                if (v.isEmpty())
                                    onCancel.onCall(p);
                                else {
                                    area.setName(v);
                                    // WORKAROUND direct SQL
                                    // if (LandClaim.wdbAreas != null) {
                                    // LandClaim.wdbAreas.executeUpdate(
                                    // "UPDATE areas SET name ='" + v.replace("'", "''") + "' WHERE id="
                                    // + area.getID());
                                    // }
                                    p.sendTextMessage(t.get("TC_AREA_RENNAMED", p)
                                            .replace("PH_AREA_NAME", v)
                                            .replace("PH_OLD_NAME",
                                                    currentName != null ? currentName : "Unnamed Area"));
                                }
                            }, onCancel);

                    p.addUIElement(renameWindow, UITarget.HUD);
                    CursorManager.show(p);
                    p.hideRadialMenu(false);

                });
    }

    private MenuItem menuItemRemoveArea(Player player, Area area, Callback<Player> onCancel) {
        return new MenuItem("zone-claim-delete",
                t.get("TC_MENU_AREA_RELEASE", player),
                (p) -> {
                    UIElement confirmDialog = UIDialogFactory.getConfirmDangerDialog(p,
                            t.get("TC_DIALOG_AREA_RELEASE_TITLE", p),
                            t.get("TC_DIALOG_AREA_RELEASE_CONFIRM", p).replace("PH_AREA_NAME",
                                    area.getName() == null ? "Unnamed Area" : area.getName()),
                            (Boolean v) -> {
                                if (v) {
                                    chunkClaimUtil.releaseArea(p, area);
                                    Area3DUtils.updateAreaFramesForAllPlayers();
                                    p.sendTextMessage(t.get("TC_DIALOG_AREA_RELEASE_SUCCESS", p));
                                }
                            }, onCancel);

                    p.addUIElement(confirmDialog, UITarget.HUD);
                    CursorManager.show(p);
                    p.hideRadialMenu(false);

                });
    }

    private MenuItem menuItemListAreaForSale(Player player, Area area, Callback<Player> onCancel) {
        return new MenuItem("zone-sale",
                t.get("TC_MENU_AREA_SALE_LIST", player),
                (p) -> {
                    UIElement priceWindow = UIDialogFactory.getTextInput(p,
                            t.get("TC_DIALOG_AREA_SALE_LIST_TITLE", p),
                            "",
                            (String value) -> {
                                if (value.isBlank()) {
                                    onCancel.onCall(p);
                                    return;
                                }
                                long price = parsePositiveLong(value);
                                if (price <= 0 || LandClaim.claimSaleListingService() == null) {
                                    p.sendTextMessage(t.get("TC_AREA_SALE_INVALID_PRICE", p));
                                    onCancel.onCall(p);
                                    return;
                                }
                                ClaimSaleListing listing = LandClaim.claimSaleListingService()
                                        .listForSale(p, area.getID(), price);
                                if (listing == null) {
                                    p.sendTextMessage(t.get("TC_AREA_SALE_LIST_FAILED", p));
                                } else {
                                    p.sendTextMessage(t.get("TC_AREA_SALE_LISTED", p)
                                            .replace("PH_AREA_NAME", areaName(area))
                                            .replace("PH_PRICE", String.valueOf(listing.price())));
                                    Area3DUtils.updateAreaFramesForAllPlayers();
                                }
                                onCancel.onCall(p);
                            },
                            onCancel);

                    p.addUIElement(priceWindow, UITarget.HUD);
                    CursorManager.show(p);
                    p.hideRadialMenu(false);
                });
    }

    private MenuItem menuItemWithdrawAreaSale(Player player, Area area, ClaimSaleListing listing,
            Callback<Player> onCancel) {
        return new MenuItem("undo",
                t.get("TC_MENU_AREA_SALE_WITHDRAW", player),
                (p) -> {
                    UIElement confirmDialog = UIDialogFactory.getConfirmDialog(p,
                            t.get("TC_DIALOG_AREA_SALE_WITHDRAW_TITLE", p),
                            t.get("TC_DIALOG_AREA_SALE_WITHDRAW_CONFIRM", p)
                                    .replace("PH_AREA_NAME", areaName(area))
                                    .replace("PH_PRICE", String.valueOf(listing.price())),
                            (Boolean v) -> {
                                if (v && LandClaim.claimSaleListingService() != null
                                        && LandClaim.claimSaleListingService().withdrawActiveListing(area.getID())) {
                                    p.sendTextMessage(t.get("TC_AREA_SALE_WITHDRAWN", p)
                                            .replace("PH_AREA_NAME", areaName(area)));
                                    Area3DUtils.updateAreaFramesForAllPlayers();
                                }
                                onCancel.onCall(p);
                            },
                            onCancel);

                    p.addUIElement(confirmDialog, UITarget.HUD);
                    CursorManager.show(p);
                    p.hideRadialMenu(false);
                });
    }

    private MenuItem menuItemBuyArea(Player player, Area area, ClaimSaleListing listing, Callback<Player> onCancel) {
        return new MenuItem("zone-sale",
                t.get("TC_MENU_AREA_SALE_BUY", player),
                (p) -> {
                    UIElement confirmDialog = UIDialogFactory.getConfirmDialog(p,
                            t.get("TC_DIALOG_AREA_SALE_BUY_TITLE", p),
                            t.get("TC_DIALOG_AREA_SALE_BUY_CONFIRM", p)
                                    .replace("PH_AREA_NAME", areaName(area))
                                    .replace("PH_PRICE", String.valueOf(listing.price())),
                            (Boolean v) -> {
                                if (v) {
                                    purchaseArea(p, area, listing);
                                }
                                onCancel.onCall(p);
                            },
                            onCancel);

                    p.addUIElement(confirmDialog, UITarget.HUD);
                    CursorManager.show(p);
                    p.hideRadialMenu(false);
                });
    }

    private void purchaseArea(Player buyer, Area area, ClaimSaleListing listing) {
        if (LandClaim.claimSaleListingService() == null || LandClaim.economyIntegration() == null
                || !LandClaim.economyIntegration().isWalletAvailable()) {
            buyer.sendTextMessage(t.get("TC_AREA_SALE_PURCHASE_WALLET_MISSING", buyer));
            return;
        }
        ClaimSaleListing currentListing = activeSaleListing(area);
        if (currentListing == null || currentListing.id() != listing.id()) {
            buyer.sendTextMessage(t.get("TC_AREA_SALE_PURCHASE_STALE", buyer));
            return;
        }
        if (currentListing.ownerDbId() == buyer.getDbID()) {
            buyer.sendTextMessage(t.get("TC_AREA_SALE_PURCHASE_SELF", buyer));
            return;
        }
        if (!canBuyAreaWithinLimit(buyer, area)) {
            buyer.sendTextMessage(t.get("TC_AREA_SALE_PURCHASE_LIMIT", buyer)
                    .replace("PH_MAX_CLAIMS", String.valueOf(chunkClaimUtil.getPlayerMaxClaims(buyer))));
            return;
        }

        String displayAreaName = areaName(area);
        Map<Integer, String> originalPermissions = capturePermissions(area);
        String reason = "LandClaim area purchase: " + displayAreaName + " (#" + area.getID() + ")";
        WalletOperationResult withdrawal = LandClaim.economyIntegration().withdrawDefault(
                buyer.getDbID(), currentListing.price(), reason);
        if (!withdrawal.success()) {
            buyer.sendTextMessage(t.get("TC_AREA_SALE_PURCHASE_WITHDRAW_FAILED", buyer)
                    .replace("PH_REASON", walletMessage(withdrawal)));
            return;
        }

        if (!chunkClaimUtil.transferAreaOwnership(area, buyer)) {
            refundBuyer(buyer, currentListing.price(), reason);
            buyer.sendTextMessage(t.get("TC_AREA_SALE_PURCHASE_TRANSFER_FAILED", buyer));
            return;
        }

        WalletOperationResult sellerCredit = LandClaim.economyIntegration().depositDefault(
                currentListing.ownerDbId(), currentListing.price(), reason);
        if (!sellerCredit.success()) {
            rollbackAreaTransfer(area, currentListing, originalPermissions);
            refundBuyer(buyer, currentListing.price(), reason);
            buyer.sendTextMessage(t.get("TC_AREA_SALE_PURCHASE_SELLER_CREDIT_FAILED", buyer)
                    .replace("PH_REASON", walletMessage(sellerCredit)));
            Area3DUtils.updateAreaFramesForAllPlayers();
            return;
        }

        if (!LandClaim.claimSaleListingService().markPurchased(area.getID(), buyer)) {
            rollbackAreaTransfer(area, currentListing, originalPermissions);
            refundBuyer(buyer, currentListing.price(), reason);
            WalletOperationResult sellerDebit = LandClaim.economyIntegration().withdrawDefault(
                    currentListing.ownerDbId(), currentListing.price(),
                    "Rollback failed LandClaim area purchase: " + displayAreaName + " (#" + area.getID() + ")");
            if (!sellerDebit.success()) {
                LandClaim.logger().error("Could not reverse seller credit for failed claim purchase on area "
                        + area.getID() + ": " + walletMessage(sellerDebit));
            }
            buyer.sendTextMessage(t.get("TC_AREA_SALE_PURCHASE_LISTING_UPDATE_FAILED", buyer));
            Area3DUtils.updateAreaFramesForAllPlayers();
            return;
        }

        buyer.sendTextMessage(t.get("TC_AREA_SALE_PURCHASED", buyer)
                .replace("PH_AREA_NAME", displayAreaName)
                .replace("PH_PRICE", String.valueOf(currentListing.price())));
        Player seller = Server.getPlayerByDbID(currentListing.ownerDbId());
        if (seller != null) {
            seller.sendTextMessage(t.get("TC_AREA_SALE_SOLD", seller)
                    .replace("PH_AREA_NAME", displayAreaName)
                    .replace("PH_PRICE", String.valueOf(currentListing.price()))
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
        String message = t.get("TC_DISCORD_AREA_SOLD", DiscordConnect.botLang())
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
                onlinePlayer.sendTextMessage(t.get("TC_ANNOUNCEMENT_AREA_SOLD", onlinePlayer)
                        .replace("PH_AREA_NAME", areaName(area))
                        .replace("PH_PRICE", String.valueOf(listing.price()))
                        .replace("PH_BUYER_NAME", buyer.getName())
                        .replace("PH_SELLER_NAME", resolvedSellerName));
            }
        }
    }

    private long parsePositiveLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
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
                    UIElement confirmDialog = UIDialogFactory.getConfirmDangerDialog(p,
                            t.get("TC_DIALOG_AREA_EXPAND_TITLE", p),
                            t.get("TC_DIALOG_AREA_EXPAND_CONFIRM", p)
                                    .replace("PH_AREA_NAME", area.getName() == null ? "Unnamed Area" : area.getName())
                                    .replace("PH_DIRECTION",
                                            t.get("TC_DIRECTION_" + direction.name().toUpperCase(), p)),
                            (Boolean v) -> {
                                if (v) {
                                    Area expandedArea = chunkClaimUtil.expandClaim(area, direction, p);
                                    if (expandedArea != null) {
                                        expandClaimAnnouncement(expandedArea, p);
                                        Area3DUtils.updateAreaFramesForAllPlayers();
                                        openExpandAreaMenu(p, onBack, expandedArea);
                                    } else {
                                        // p.sendTextMessage(t.get("TC_DIALOG_AREA_EXPAND_FAILED", p));
                                    }
                                }
                            }, onCancel);

                    p.addUIElement(confirmDialog, UITarget.HUD);
                    CursorManager.show(p);
                    p.hideRadialMenu(false);

                });
    }

    private MenuItem menuItemPermissionManager(Player player, Area area, Callback<Player> onResponse) {
        return new MenuItem("menu-zone-permissions",
                t.get("TC_MENU_AREA_PERMISSIONS", player),
                (p) -> {
                    UIElement overlay = (UIElement) p.getAttribute(PermissionOverlay.ATTRIBUTE_KEY);
                    if (overlay != null) {
                        p.removeUIElement(overlay);
                        CursorManager.hide(p);
                    }
                    PermissionOverlay permissionOverlay = new PermissionOverlay(area, p, onResponse);
                    p.addUIElement(permissionOverlay, UITarget.HUD);
                    CursorManager.show(p);
                    p.setAttribute(PermissionOverlay.ATTRIBUTE_KEY, permissionOverlay);

                    p.hideRadialMenu(false);

                });
    }

    private MenuItem menuItemAdminCleanup(Player uiPlayer, Callback<Player> onResponse) {
        return new MenuItem("menu-zone-management",
                t.get("TC_MENU_ADMIN_CLEANUP", uiPlayer),
                (p) -> {
                    UIElement overlay = (UIElement) p.getAttribute(AdminCleanupOverlay.ATTRIBUTE_KEY);
                    if (overlay != null) {
                        p.removeUIElement(overlay);
                        CursorManager.hide(p);
                    }
                    AdminCleanupOverlay cleanupOverlay = new AdminCleanupOverlay(p, cleanupService, onResponse);
                    p.addUIElement(cleanupOverlay, UITarget.HUD);
                    CursorManager.show(p);
                    p.setAttribute(AdminCleanupOverlay.ATTRIBUTE_KEY, cleanupOverlay);
                    p.hideRadialMenu(false);
                });
    }

    private MenuItem menuItemAreaConfig(Player player, Callback<Player> onResponse) {
        return new MenuItem("zone-renew-create",
                t.get("TC_MENU_AREA_CONFIG", player),
                (p) -> openCurrentAreaConfig(p, onResponse));
    }

    public void openCurrentAreaConfig(Player player, Callback<Player> onCancel) {
        Area area = player.getCurrentArea();
        if (!isRenewArea(area)) {
            player.sendTextMessage(t.get("TC_AREA_CONFIG_UNAVAILABLE", player));
            onCancel.onCall(player);
            return;
        }
        openRenewZoneConfig(player, area, onCancel);
    }

    private void openRenewZoneConfig(Player player, Area area, Callback<Player> onCancel) {
        if (LandClaim.renewZoneConfigService() == null) {
            player.sendTextMessage(t.get("TC_AREA_CONFIG_UNAVAILABLE", player));
            onCancel.onCall(player);
            return;
        }
        RenewZoneConfig config = LandClaim.renewZoneConfigService()
                .find(area.getID())
                .orElse(new RenewZoneConfig(area.getID(), s.renewZoneDefaultIntervalHours, 0L));
        UIElement intervalWindow = UIDialogFactory.getTextInput(player,
                t.get("TC_DIALOG_RENEW_ZONE_CONFIG_TITLE", player),
                String.valueOf(config.intervalHours()),
                (String value) -> {
                    int intervalHours = parsePositiveInt(value);
                    if (intervalHours <= 0) {
                        player.sendTextMessage(t.get("TC_RENEW_ZONE_CONFIG_INVALID_INTERVAL", player));
                        onCancel.onCall(player);
                        return;
                    }
                    RenewZoneConfig saved = LandClaim.renewZoneConfigService().save(
                            area.getID(),
                            intervalHours,
                            config.lastResetAt());
                    if (saved == null) {
                        player.sendTextMessage(t.get("TC_RENEW_ZONE_CONFIG_SAVE_FAILED", player));
                    } else {
                        player.sendTextMessage(t.get("TC_RENEW_ZONE_CONFIG_SAVED", player)
                                .replace("PH_INTERVAL_HOURS", String.valueOf(saved.intervalHours())));
                    }
                    onCancel.onCall(player);
                },
                onCancel);

        player.addUIElement(intervalWindow, UITarget.HUD);
        CursorManager.show(player);
        player.hideRadialMenu(false);
    }

    public void openSpecialAreaMenu(Player uiPlayer, Callback<Player> onBack) {
        List<MenuItem> menuItems = new ArrayList<>();

        Area currentArea = uiPlayer.getCurrentArea();

        Callback<Player> onBackReopen = (Player player) -> openSpecialAreaMenu(player, onBack);

        if (currentArea == null) {
            Area virtualArea = ChunkClaimUtil.getVirtualAreaFromChunkVector(uiPlayer.getChunkPosition());
            // special areas
            menuItems.add(menuItemCreateSpecialArea(uiPlayer, "zone-neutral-create", "TC_MENU_SPECIAL_AREA_CREATE",
                    virtualArea,
                    s.specialAreaPermission, onBack));
            menuItems.add(menuItemCreateSpecialArea(uiPlayer, "zone-static-create", "TC_MENU_SPECIAL_AREA_STATIC",
                    virtualArea, s.specialStaticAreaPermission, onBack));
            menuItems.add(menuItemCreateSpecialArea(uiPlayer, "zone-combat-create", "TC_MENU_SPECIAL_AREA_PVP",
                    virtualArea, s.specialPvPAreaPermission, onBack));
            menuItems.add(
                    menuItemCreateSpecialArea(uiPlayer, "zone-rest-create", "TC_MENU_SPECIAL_AREA_REST", virtualArea,
                            s.specialRestAreaPermission, onBack));
            menuItems
                    .add(menuItemCreateSpecialArea(uiPlayer, "zone-trap-create", "TC_MENU_SPECIAL_AREA_TRAP",
                            virtualArea,
                            s.specialTrapAreaPermission, onBack));
            menuItems
                    .add(menuItemCreateSpecialArea(uiPlayer, "zone-renew-create", "TC_MENU_SPECIAL_AREA_RENEW",
                            virtualArea,
                            s.specialRenewAreaPermission, onBack));
        }
        // show extend menu if area exist
        if (currentArea != null) {
            menuItems.add(new MenuItem("menu-expand-zone",
                    t.get("TC_MENU_AREA_EXPAND_OPTION", uiPlayer),
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
                    t.get("TC_MENU_ADMIN_DEBUG", uiPlayer),
                    (p) -> {
                        Area3DUtils.updateAreaFramesForPlayer(p);
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
                    t.get("TC_MENU_ADMIN_SYNC_REPAIR", uiPlayer),
                    (p) -> {
                        if (p.isAdmin()) {
                            chunkClaimUtil.syncAndRepairAreas();
                            p.sendTextMessage(t.get("TC_CMD_REPAIR_SUCCESS"));
                        } else
                            p.sendTextMessage(t.get("TC_CMD_REPAIR_ERR_PERMISSION"));
                        // if we do not reopen the menu it seems to be frozen and unclickable
                        openAdminMenu(p, onBack);
                    }));
        }
        menuItems.add(menuItemAdminCleanup(uiPlayer, onBackReopen));
        if (currentArea != null) {
            menuItems.add(menuItemRenameArea(uiPlayer, currentArea, onBackReopen));
            if (isRenewArea(currentArea)) {
                menuItems.add(menuItemAreaConfig(uiPlayer, onBackReopen));
            }
            menuItems.add(menuItemPermissionManager(uiPlayer, currentArea, onBackReopen));
            menuItems.add(menuItemRemoveArea(uiPlayer, currentArea, onBackReopen));
        }
        if (!isDefaultArea)
            menuItems.add(new MenuItem("menu-zone-special",
                    t.get("TC_MENU_SPECIAL_AREA", uiPlayer),
                    (p) -> openSpecialAreaMenu(p, onBackReopen)));
        if (chunkCount > 1)
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

        if (isOwner) {
            menuItems.add(new MenuItem("menu-expand-zone",
                    t.get("TC_MENU_AREA_EXPAND_OPTION", uiPlayer),
                    (p) -> {
                        openExpandAreaMenu(p, onBackReopen);
                    }));
            if (chunkCount > 1)
                menuItems.add(menuItemSplitArea(uiPlayer, currentArea, onBackReopen));

            menuItems.add(menuItemPermissionManager(uiPlayer, currentArea, onBackReopen));
            menuItems.add(menuItemRenameArea(uiPlayer, currentArea, onBackReopen));
            if (s.allowClaimSale && LandClaim.claimSaleListingService() != null) {
                ClaimSaleListing listing = LandClaim.claimSaleListingService().activeListing(currentArea.getID())
                        .orElse(null);
                menuItems.add(listing == null
                        ? menuItemListAreaForSale(uiPlayer, currentArea, onBackReopen)
                        : menuItemWithdrawAreaSale(uiPlayer, currentArea, listing, onBackReopen));
            }
            menuItems.add(menuItemRemoveArea(uiPlayer, currentArea, onBackReopen));
        }

        menuItems.add(MenuItem.closeMenu(uiPlayer));
        menuItems.add(MenuItem.backMenu(uiPlayer, onBack));

        PluginMenuManager.showMenu(uiPlayer, menuItems);
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
                    "TC_MENU_AREA_EXPAND_NORTH", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.EAST, "zone-expand-east",
                    "TC_MENU_AREA_EXPAND_EAST", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.SOUTH, "zone-expand-south",
                    "TC_MENU_AREA_EXPAND_SOUTH", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.WEST, "zone-expand-west",
                    "TC_MENU_AREA_EXPAND_WEST", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.UP, "zone-expand-up",
                    "TC_MENU_AREA_EXPAND_UP",
                    onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.DOWN, "zone-expand-down",
                    "TC_MENU_AREA_EXPAND_DOWN", onBackReopen, onBackReopen));
        }
        menuItems.add(MenuItem.closeMenu(uiPlayer));
        menuItems.add(MenuItem.backMenu(uiPlayer, onBack));

        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

    private void expandClaimAnnouncement(Area area, Player player) {
        player.sendTextMessage(t.get("TC_CLAIM_EXPANDED", player).replace("PH_AREA_NAME", area.getName()));
        String message = t.get("TC_DISCORD_AREA_EXPANDED", DiscordConnect.botLang())
                .replace("PH_AREA_NAME", area.getName())
                .replace("PH_CHUNK_POS", area.getStartChunkPosition() + "")
                .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(player.getDbID()));
        DiscordConnect.sendDiscordExpandAnnouncement(message);
        // message to all players
        for (Player onlinePlayer : Server.getAllPlayers()) {
            if (!onlinePlayer.equals(player))
                onlinePlayer.sendTextMessage(
                        t.get("TC_ANNOUNCEMENT_AREA_EXPANDED", onlinePlayer)
                                .replace("PH_AREA_NAME", area.getName())
                                .replace("PH_CHUNK_POS", area.getStartChunkPosition().toString())
                                .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(player.getDbID())));
        }
    }

    private void createSpecialAreaAnnouncement(Area area, Player player) {
        player.sendYellMessage(t.get("TC_AREA_SPECIAL_CREATED", player), 5, false);
        // Discord announcement
        String message = t.get("TC_DISCORD_AREA_SPECIAL_CREATED", DiscordConnect.botLang())
                .replace("PH_AREA_NAME", area.getName())
                .replace("PH_CHUNK_POS", area.getStartChunkPosition().toString())
                .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(player.getDbID()));
        DiscordConnect.sendDiscordClaimAnnouncement(message);
        // Server announcement
        for (Player onlinePlayer : Server.getAllPlayers()) {
            if (!onlinePlayer.equals(player))
                onlinePlayer.sendTextMessage(
                        t.get("TC_ANNOUNCEMENT_AREA_SPECIAL_CREATED", onlinePlayer)
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
                t.get(showCurrentChunkFrame ? "TC_MENU_VISIBILITY_CURRENT_HIDE" : "TC_MENU_VISIBILITY_CURRENT_SHOW",
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
                t.get(showOwnedAreaFrames ? "TC_MENU_VISIBILITY_OWNED_HIDE" : "TC_MENU_VISIBILITY_OWNED_SHOW",
                        uiPlayer),
                (p) -> {
                    LandClaimPlayerPluginSettings.setBooleanValue(p,
                            LandClaimPlayerPluginSettings.SHOW_OWNED_AREA_FRAMES_KEY, !showOwnedAreaFrames);
                    // if we do not reopen the menu it seems to be frozen and unclickable
                    openVisibilitySettingsMenu(p);
                    Area3DUtils.updateAreaFramesForPlayer(p);
                }));

        menuItems.add(new MenuItem(showOtherAreaFrames ? "zone-visibility-others-on" : "zone-visibility-others-off",
                t.get(showOtherAreaFrames ? "TC_MENU_VISIBILITY_OTHER_HIDE" : "TC_MENU_VISIBILITY_OTHER_SHOW",
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
                t.get("TC_MENU_VISIBILITY", uiPlayer),
                (p) -> {
                    openVisibilitySettingsMenu(p);
                }));

        if (uiPlayer.isAdmin())
            menuItems.add(new MenuItem("menu-zone-admin",
                    t.get("TC_MENU_ADMIN", uiPlayer),
                    (p) -> {
                        openAdminMenu(p, (Player player) -> openMainMenu(player));
                    }));

        if (canClaimArea)
            menuItems.add(new MenuItem("zone-claim-create",
                    t.get("TC_MENU_CLAIM", uiPlayer),
                    (p) -> {
                        Area createdArea = chunkClaimUtil.claimArea(p,
                                ChunkClaimUtil.getVirtualAreaFromChunkVector(uiPlayer.getChunkPosition()));
                        if (createdArea != null) {
                            p.sendYellMessage(t.get("TC_CLAIM_CONGRATULATION", p), 5, true);
                            // Discord announcement
                            String message = t.get("TC_DISCORD_AREA_CLAIMED", DiscordConnect.botLang())
                                    .replace("PH_AREA_NAME", createdArea.getName())
                                    .replace("PH_CHUNK_POS", createdArea.getStartChunkPosition().toString())
                                    .replace("PH_PLAYER_NAME", Server.getLastKnownPlayerName(p.getDbID()));
                            DiscordConnect.sendDiscordClaimAnnouncement(message);
                            // Server announcement
                            for (Player onlinePlayer : Server.getAllPlayers()) {
                                if (!onlinePlayer.equals(p))
                                    onlinePlayer.sendTextMessage(
                                            t.get("TC_ANNOUNCEMENT_AREA_CLAIMED", onlinePlayer)
                                                    .replace("PH_AREA_NAME", createdArea.getName())
                                                    .replace("PH_CHUNK_POS",
                                                            createdArea.getStartChunkPosition().toString())
                                                    .replace("PH_PLAYER_NAME",
                                                            Server.getLastKnownPlayerName(p.getDbID())));
                            }
                            Area3DUtils.updateAreaFramesForAllPlayers();
                        }
                        openMainMenu(p);
                    }));
        if (activeSaleListing != null && !currentAreaOwner) {
            menuItems.add(menuItemBuyArea(uiPlayer, currentArea, activeSaleListing,
                    (Player player) -> openMainMenu(player)));
        }
        if (currentArea != null) {
            menuItems.add(new MenuItem("zone-claim-create",
                    t.get("TC_MENU_AREA_OPTION", uiPlayer),
                    (p) -> {
                        openClaimOptionsMenu(p, (Player player) -> openMainMenu(player));
                    }));
        }

        menuItems.add(PluginInfoStatusProviders.menuItem(t.get("TC_MENU_INFO_STATUS", uiPlayer), LandClaim.name));
        menuItems.add(MenuItem.closeMenu(uiPlayer));

        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

    private ClaimSaleListing activeSaleListing(Area area) {
        if (!s.allowClaimSale || area == null || LandClaim.claimSaleListingService() == null) {
            return null;
        }
        return LandClaim.claimSaleListingService().activeListing(area.getID()).orElse(null);
    }

    private boolean isOwner(Player player, Area area) {
        String areaPermission = area == null ? null : area.getPlayerPermission(player);
        return areaPermission != null && areaPermission.equals(s.ownerAreaPermission);
    }

    private boolean isRenewArea(Area area) {
        return area != null && s.specialRenewAreaPermission.equals(area.getDefaultPermission());
    }

}
