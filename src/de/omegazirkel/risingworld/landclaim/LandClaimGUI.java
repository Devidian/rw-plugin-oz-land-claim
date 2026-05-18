package de.omegazirkel.risingworld.landclaim;

import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.ui.AdminCleanupOverlay;
import de.omegazirkel.risingworld.landclaim.ChunkClaimUtil.Direction;
import de.omegazirkel.risingworld.landclaim.ui.LandClaimPlayerPluginSettings;
import de.omegazirkel.risingworld.landclaim.ui.PermissionOverlay;
import de.omegazirkel.risingworld.landclaim.ui.UIDialogFactory;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.CursorManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
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

        // Icons for radial menu
        AssetManager.loadIconFromPlugin(p, "oz-lc-logo", "/assets/icons/oz-lc-logo-alt.png");
        AssetManager.loadIconFromPlugin(p, "debug", "/assets/icons/error-bug.png");
        AssetManager.loadIconFromPlugin(p, "hideCurrentChunk", "/assets/icons/hide-current-frame.png");
        AssetManager.loadIconFromPlugin(p, "showCurrentChunk", "/assets/icons/show-current-frame.png");
        AssetManager.loadIconFromPlugin(p, "hideOwnedAreas", "/assets/icons/hide-owned-area-frames.png");
        AssetManager.loadIconFromPlugin(p, "showOwnedAreas", "/assets/icons/show-owned-area-frames.png");
        AssetManager.loadIconFromPlugin(p, "hideOtherAreas", "/assets/icons/hide-other-area-frames.png");
        AssetManager.loadIconFromPlugin(p, "showOtherAreas", "/assets/icons/show-other-area-frames.png");
        AssetManager.loadIconFromPlugin(p, "visibilityMenu", "/assets/icons/visibility-menu.png");
        AssetManager.loadIconFromPlugin(p, "tools");
        AssetManager.loadIconFromPlugin(p, "claim-chunk");
        AssetManager.loadIconFromPlugin(p, "resize");
        AssetManager.loadIconFromPlugin(p, "grid");
        AssetManager.loadIconFromPlugin(p, "square-minus");
        AssetManager.loadIconFromPlugin(p, "users-gear");
        AssetManager.loadIconFromPlugin(p, "admin-cleanup", "/assets/icons/tools.png");
        // Area expansion menu
        AssetManager.loadIconFromPlugin(p, "compass-north");
        AssetManager.loadIconFromPlugin(p, "compass-east");
        AssetManager.loadIconFromPlugin(p, "compass-south");
        AssetManager.loadIconFromPlugin(p, "compass-west");
        AssetManager.loadIconFromPlugin(p, "up");
        AssetManager.loadIconFromPlugin(p, "down");
        // Special area menu (admin)
        AssetManager.loadIconFromPlugin(p, "square-star"); // open special area menu
        AssetManager.loadIconFromPlugin(p, "icon-star"); // create default special area
        AssetManager.loadIconFromPlugin(p, "user-forbidden"); // create trap area
        AssetManager.loadIconFromPlugin(p, "bed"); // create rest area
        AssetManager.loadIconFromPlugin(p, "sword"); // create pvp area

        return getInstance();
    }

    public static LandClaimGUI getInstance() {
        if (instance == null) {
            instance = new LandClaimGUI();
        }
        return instance;
    }

    private MenuItem menuItemSplitArea(Player uiPlayer, Area area, Callback<Player> onCancel) {
        return new MenuItem(
                AssetManager.getIcon("grid"),
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
        return new MenuItem(
                AssetManager.getIcon("rename"),
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
                                    //     LandClaim.wdbAreas.executeUpdate(
                                    //             "UPDATE areas SET name ='" + v.replace("'", "''") + "' WHERE id="
                                    //                     + area.getID());
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
        return new MenuItem(
                AssetManager.getIcon("square-minus"),
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

    private MenuItem menuItemCreateSpecialArea(
            Player uiPlayer,
            String iconKey,
            String labelKey,
            Area area,
            String permission,
            Callback<Player> onBack) {
        return new MenuItem(
                AssetManager.getIcon(iconKey),
                t.get(labelKey, uiPlayer),
                (p) -> {
                    Area createdArea = chunkClaimUtil.claimArea(uiPlayer, area, permission, null);

                    if (createdArea != null) {
                        createSpecialAreaAnnouncement(area, uiPlayer);
                        Area3DUtils.updateAreaFramesForAllPlayers();
                    }

                    openSpecialAreaMenu(uiPlayer, onBack);
                });
    }

    private MenuItem menuItemExpandArea(Player player, Area area, Direction direction, String iconKey, String labelKey,
            Callback<Player> onCancel, Callback<Player> onBack) {
        return new MenuItem(
                AssetManager.getIcon(iconKey),
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
        return new MenuItem(
                AssetManager.getIcon("users-gear"),
                t.get("TC_MENU_AREA_PERMISSIONS", player),
                (p) -> {
                    UIElement overlay = (UIElement) p.getAttribute("landclaim-overlay");
                    if (overlay != null) {
                        p.removeUIElement(overlay);
                        CursorManager.hide(p);
                    }
                    PermissionOverlay permissionOverlay = new PermissionOverlay(area, p, onResponse);
                    p.addUIElement(permissionOverlay, UITarget.HUD);
                    CursorManager.show(p);
                    p.setAttribute("landclaim-overlay", permissionOverlay);

                    p.hideRadialMenu(false);

                });
    }

    private MenuItem menuItemAdminCleanup(Player uiPlayer, Callback<Player> onResponse) {
        return new MenuItem(
                AssetManager.getIcon("admin-cleanup"),
                t.get("TC_MENU_ADMIN_CLEANUP", uiPlayer),
                (p) -> {
                    UIElement overlay = (UIElement) p.getAttribute("landclaim-admin-cleanup-overlay");
                    if (overlay != null) {
                        p.removeUIElement(overlay);
                        CursorManager.hide(p);
                    }
                    AdminCleanupOverlay cleanupOverlay = new AdminCleanupOverlay(p, cleanupService, onResponse);
                    p.addUIElement(cleanupOverlay, UITarget.HUD);
                    CursorManager.show(p);
                    p.setAttribute("landclaim-admin-cleanup-overlay", cleanupOverlay);
                    p.hideRadialMenu(false);
                });
    }

    public void openSpecialAreaMenu(Player uiPlayer, Callback<Player> onBack) {
        List<MenuItem> menuItems = new ArrayList<>();

        Area currentArea = uiPlayer.getCurrentArea();

        Callback<Player> onBackReopen = (Player player) -> openSpecialAreaMenu(player, onBack);

        if (currentArea == null) {
            Area virtualArea = ChunkClaimUtil.getVirtualAreaFromChunkVector(uiPlayer.getChunkPosition());
            // special areas
            menuItems.add(menuItemCreateSpecialArea(uiPlayer, "icon-star", "TC_MENU_SPECIAL_AREA_CREATE", virtualArea,
                    s.specialAreaPermission, onBack));
            menuItems.add(menuItemCreateSpecialArea(uiPlayer, "sword", "TC_MENU_SPECIAL_AREA_PVP", virtualArea,
                    s.specialPvPAreaPermission, onBack));
            menuItems.add(menuItemCreateSpecialArea(uiPlayer, "bed", "TC_MENU_SPECIAL_AREA_REST", virtualArea,
                    s.specialRestAreaPermission, onBack));
            menuItems
                    .add(menuItemCreateSpecialArea(uiPlayer, "user-forbidden", "TC_MENU_SPECIAL_AREA_TRAP", virtualArea,
                            s.specialTrapAreaPermission, onBack));
        }
        // show extend menu if area exist
        if (currentArea != null) {
            menuItems.add(new MenuItem(
                    AssetManager.getIcon("resize"),
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

            menuItems.add(new MenuItem(
                    AssetManager.getIcon("debug"),
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
            menuItems.add(new MenuItem(
                    AssetManager.getIcon("tools"),
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
            menuItems.add(menuItemPermissionManager(uiPlayer, currentArea, onBackReopen));
            menuItems.add(menuItemRemoveArea(uiPlayer, currentArea, onBackReopen));
        }
        if (!isDefaultArea)
            menuItems.add(new MenuItem(
                    AssetManager.getIcon("square-star"),
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
            menuItems.add(new MenuItem(
                    AssetManager.getIcon("resize"),
                    t.get("TC_MENU_AREA_EXPAND_OPTION", uiPlayer),
                    (p) -> {
                        openExpandAreaMenu(p, onBackReopen);
                    }));
            if (chunkCount > 1)
                menuItems.add(menuItemSplitArea(uiPlayer, currentArea, onBackReopen));

            menuItems.add(menuItemPermissionManager(uiPlayer, currentArea, onBackReopen));
            menuItems.add(menuItemRenameArea(uiPlayer, currentArea, onBackReopen));
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
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.NORTH, "compass-north",
                    "TC_MENU_AREA_EXPAND_NORTH", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.EAST, "compass-east",
                    "TC_MENU_AREA_EXPAND_EAST", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.SOUTH, "compass-south",
                    "TC_MENU_AREA_EXPAND_SOUTH", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.WEST, "compass-west",
                    "TC_MENU_AREA_EXPAND_WEST", onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.UP, "up", "TC_MENU_AREA_EXPAND_UP",
                    onBackReopen, onBackReopen));
            menuItems.add(menuItemExpandArea(uiPlayer, currentArea, Direction.DOWN, "down",
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

        menuItems.add(new MenuItem(
                AssetManager.getIcon(showCurrentChunkFrame ? "hideCurrentChunk" : "showCurrentChunk"),
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

        menuItems.add(new MenuItem(
                AssetManager.getIcon(showOwnedAreaFrames ? "hideOwnedAreas" : "showOwnedAreas"),
                t.get(showOwnedAreaFrames ? "TC_MENU_VISIBILITY_OWNED_HIDE" : "TC_MENU_VISIBILITY_OWNED_SHOW",
                        uiPlayer),
                (p) -> {
                    LandClaimPlayerPluginSettings.setBooleanValue(p,
                            LandClaimPlayerPluginSettings.SHOW_OWNED_AREA_FRAMES_KEY, !showOwnedAreaFrames);
                    // if we do not reopen the menu it seems to be frozen and unclickable
                    openVisibilitySettingsMenu(p);
                    Area3DUtils.updateAreaFramesForPlayer(p);
                }));

        menuItems.add(new MenuItem(
                AssetManager.getIcon(showOtherAreaFrames ? "hideOtherAreas" : "showOtherAreas"),
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

        List<MenuItem> menuItems = new ArrayList<>();

        menuItems.add(new MenuItem(
                AssetManager.getIcon("visibilityMenu"),
                t.get("TC_MENU_VISIBILITY", uiPlayer),
                (p) -> {
                    openVisibilitySettingsMenu(p);
                }));

        if (uiPlayer.isAdmin())
            menuItems.add(new MenuItem(
                    AssetManager.getIcon("admin-menu"),
                    t.get("TC_MENU_ADMIN", uiPlayer),
                    (p) -> {
                        openAdminMenu(p, (Player player) -> openMainMenu(player));
                    }));

        if (canClaimArea)
            menuItems.add(new MenuItem(
                    AssetManager.getIcon("claim-chunk"),
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
        if (currentArea != null) {
            menuItems.add(new MenuItem(
                    AssetManager.getIcon("claim-chunk"),
                    t.get("TC_MENU_AREA_OPTION", uiPlayer),
                    (p) -> {
                        openClaimOptionsMenu(p, (Player player) -> openMainMenu(player));
                    }));
        }

        menuItems.add(MenuItem.closeMenu(uiPlayer));

        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

}
