package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.PlayerDatabaseHelper;
import de.omegazirkel.risingworld.tools.ui.CancelButton;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.Server;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

public class AreaPermissionPanel extends UIElement {

    private static final PluginSettings s = PluginSettings.getInstance();
    private static final float BODY_HEIGHT = 369f;
    private static final float TABLE_SCROLL_BODY_HEIGHT = 333f;

    private static I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    private Player uiPlayer;

    public AreaPermissionPanel(Area area, Player player, Callback<Player> onClose, UIElement parentOverlay) {
        this.setPivot(Pivot.MiddleCenter);
        this.setPosition(50f, 50f, true);
        this.style.width.set(78, Unit.Percent);
        this.style.height.set(560, Unit.Pixel);
        this.setBackgroundColor(0, 0, 0, 0.86f);
        this.setBorderColor(0.95f, 0.75f, 0.25f, 0.6f);
        this.setBorder(1);

        this.uiPlayer = player;

        setupPanelHeader(area, player);
        setupPanelBody(area, player, parentOverlay);
        setupPanelFooter(area, player, onClose);
    }

    private void setupPanelHeader(Area area, Player player) {
        UILabel title = new UILabel(
                t().get("TC_UI_AREA_PERMISSIONS_TITLE", player)
                        .replace("PH_AREA_NAME", area.getName() != null ? area.getName() : "N/A"));
        title.setPivot(Pivot.UpperLeft);
        title.setPosition(24, 20, false);
        title.setFont(Font.DefaultBold);
        title.setFontSize(24);
        this.addChild(title);

        String ownerName = "SYSTEM";
        Map<Integer, String> ownerPermissions = area.getAllPlayerPermissions();
        if (ownerPermissions != null)
            for (Map.Entry<Integer, String> entry : ownerPermissions.entrySet()) {
                Integer dbid = entry.getKey();
                String permission = entry.getValue();
                if (s.ownerAreaPermission.equals(permission)) {
                    ownerName = Server.getLastKnownPlayerName(dbid);
                    break;
                }
            }

        UILabel subtitle = new UILabel(t().get("TC_UI_AREA_PERMISSIONS_SUBTITLE", player));
        subtitle.setPivot(Pivot.UpperLeft);
        subtitle.setPosition(24, 54, false);
        subtitle.setFont(Font.Default);
        subtitle.setFontSize(12);
        this.addChild(subtitle);

        UILabel info = new UILabel(t().get("TC_UI_AREA_PERMISSIONS_INFO", player)
                .replace("PH_AREA_OWNER", ownerName));
        info.setPivot(Pivot.UpperRight);
        info.setPosition(96f, 4f, true);
        info.setTextAlign(TextAnchor.UpperRight);
        info.setFont(Font.Default);
        info.setFontSize(12);
        this.addChild(info);

        // claimedAt, chunk-size, etc.
        // 1. bold title label left sided
        // 2. normal description label left sided
        // 3. info-box on the right side
        // - Owner
        // - Owned since
        // - Chunk-Size

    }

    private void setupPanelBody(Area area, Player player, UIElement parentOverlay) {

        TableScrollView table = new TableScrollView(
                Arrays.asList(
                        t().get("TC_UI_TH_LABEL_NAME", player),
                        t().get("TC_UI_TH_LABEL_UID", player),
                        t().get("TC_UI_TH_LABEL_STATUS", player),
                        t().get("TC_UI_TH_LABEL_PERMISSION", player)),
                Arrays.asList(30f, 35f, 15f, 20f));
        table.setPosition(0, 0, false);
        table.style.width.set(100, Unit.Percent);
        table.setScrollBodyHeight(TABLE_SCROLL_BODY_HEIGHT);

        LinkedHashMap<Integer, String> permissionCandidates = new LinkedHashMap<>();
        Map<Integer, String> allPlayerPermissions = area.getAllPlayerPermissions();

        // 1. players with explicit area permissions
        if (allPlayerPermissions != null)
            for (Map.Entry<Integer, String> entry : allPlayerPermissions.entrySet()) {
                permissionCandidates.put(entry.getKey(), entry.getValue());
            }

        // 2. currently online players
        for (Player p : Server.getAllPlayers()) {
            permissionCandidates.putIfAbsent(p.getDbID(), area.getDefaultPermission());
        }

        // 3. recently seen players from the Rising World players database
        if (s.recentlyOnlinePermissionListHours != null && s.recentlyOnlinePermissionListHours > 0) {
            long cutoffEpochSeconds = (System.currentTimeMillis() / 1000L)
                    - (s.recentlyOnlinePermissionListHours.longValue() * 3600L);
            List<Integer> recentlySeenPlayerDbIds = PlayerDatabaseHelper.findPlayersSeenSince(
                    LandClaim.getInstance(),
                    cutoffEpochSeconds);
            for (Integer playerDbId : recentlySeenPlayerDbIds) {
                if (playerDbId != null && playerDbId > 0) {
                    permissionCandidates.putIfAbsent(playerDbId, area.getDefaultPermission());
                }
            }
        }

        for (Map.Entry<Integer, String> entry : permissionCandidates.entrySet()) {
            int uid = entry.getKey();
            String permission = entry.getValue();
            boolean isOnline = Server.getPlayerByDbID(uid) != null;
            TableRow row = PlayerPermissionRow.build(
                    uid,
                    isOnline,
                    permission,
                    area.getDefaultPermission(),
                    newPermission -> setPermission(area, uid, newPermission), player, parentOverlay);
            table.addRow(row);
        }
        OZUIElement body = new OZUIElement();
        body.setPivot(Pivot.UpperLeft);
        body.setPosition(24, 120, false);
        body.style.width.set(96, Unit.Percent);
        body.style.height.set(BODY_HEIGHT, Unit.Pixel);
        body.setBackgroundColor(0.08f, 0.08f, 0.08f, 0.55f);
        body.setBorder(1);
        body.setBorderColor(0.95f, 0.75f, 0.25f, 0.48f);
        body.addChild(table);
        this.addChild(body);
    }

    private void setPermission(Area area, Integer playerDBID, String newPermission) {
        String playerName = Server.getLastKnownPlayerName(playerDBID);
        String areaDefault = area.getDefaultPermission();

        if (newPermission != null && !newPermission.isEmpty() && !newPermission.equals(areaDefault)) {
            area.setPlayerPermission(playerDBID, newPermission);
            String permissionText = newPermission;
            if (newPermission.equals(s.residentAreaPermission))
                permissionText = t().get("TC_UI_PERMISSION_RESIDENT", uiPlayer);
            if (newPermission.equals(s.friendAreaPermission))
                permissionText = t().get("TC_UI_PERMISSION_FRIEND", uiPlayer);
            if (newPermission.equals(s.defaultAreaPermission))
                permissionText = t().get("TC_UI_PERMISSION_GUEST", uiPlayer);
            if (newPermission.equals(s.prisonerAreaPermission))
                permissionText = t().get("TC_UI_PERMISSION_PRISONER", uiPlayer);
            if (newPermission.equals(s.exiledAreaPermission))
                permissionText = t().get("TC_UI_PERMISSION_EXILED", uiPlayer);
            uiPlayer.sendTextMessage(t().get("TC_UI_PLAYER_PERMISSION_SET", uiPlayer)
                    .replace("PH_PLAYER_NAME", playerName)
                    .replace("PH_PERMISSION", permissionText));
        } else {
            area.removePlayerPermission(playerDBID);
            uiPlayer.sendTextMessage(t().get("TC_UI_PLAYER_PERMISSION_UNSET", uiPlayer)
                    .replace("PH_PLAYER_NAME", playerName));
            uiPlayer.sendTextMessage("Player permission removed");
        }
    }

    private void setupPanelFooter(Area area, Player player, Callback<Player> onClose) {
        // button should remove this panel from player and call onClose
        CancelButton cb = new CancelButton(t().get("TC_UI_BTN_CLOSE", player), event -> {
            onClose.onCall(player);
        });
        cb.setPivot(Pivot.LowerCenter);
        cb.setPosition(50f, 96f, true);
        this.addChild(cb);
    }

}
