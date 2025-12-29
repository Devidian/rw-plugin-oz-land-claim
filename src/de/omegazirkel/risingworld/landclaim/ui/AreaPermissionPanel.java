package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Arrays;
import java.util.Map;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.CancelButton;
import de.omegazirkel.risingworld.tools.ui.CursorManager;
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

public class AreaPermissionPanel extends UIElement {

    private static final PluginSettings s = PluginSettings.getInstance();

    private static I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    private Player uiPlayer;

    public AreaPermissionPanel(Area area, Player player, Callback<Player> onClose, UIElement parentOverlay) {
        this.setPivot(Pivot.MiddleCenter);
        this.setPosition(50f, 50f, true);
        this.setSize(50, 50, true);
        this.setBackgroundColor(0, 0, 0, 0.85f);
        this.setBorderColor(1, 1, 1, 0.4f);
        this.setBorder(2);

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
        title.setPosition(2f, 2f, true);
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
        subtitle.setPosition(2f, 10f, true);
        subtitle.setFont(Font.Default);
        subtitle.setFontSize(12);
        this.addChild(subtitle);

        UILabel info = new UILabel(t().get("TC_UI_AREA_PERMISSIONS_INFO", player)
                .replace("PH_AREA_OWNER", ownerName));
        info.setPivot(Pivot.UpperRight);
        info.setPosition(96f, 2f, true);
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
                Arrays.asList(t().get("TC_UI_TH_LABEL_NAME", player), t().get("TC_UI_TH_LABEL_UID", player),
                        t().get("TC_UI_TH_LABEL_PERMISSION", player)),
                Arrays.asList(40f, 40f, 20f));
        Map<Integer, String> allPlayerPermissions = area.getAllPlayerPermissions();
        // 1. create a list of users from areapermission
        if (allPlayerPermissions != null)
            for (Map.Entry<Integer, String> entry : allPlayerPermissions.entrySet()) {
                int uid = entry.getKey();
                String permission = entry.getValue();

                TableRow row = PlayerPermissionRow.build(
                        uid,
                        permission,
                        area.getDefaultPermission(),
                        newPermission -> setPermission(area, uid, newPermission), player, parentOverlay);
                table.addRow(row);
            }
        // 2. create a list of online users
        for (Player p : Server.getAllPlayers()) {
            // skip players that are listed in areapermission
            if (area.getPlayerPermission(p) != null)
                continue;
            TableRow row = PlayerPermissionRow.build(
                    p.getDbID(),
                    area.getDefaultPermission(),
                    area.getDefaultPermission(),
                    newPermission -> setPermission(area, p.getDbID(), newPermission), player, parentOverlay);
            table.addRow(row);
        }
        OZUIElement body = new OZUIElement();
        body.setPivot(Pivot.UpperLeft);
        body.setPosition(2f, 20f, true);
        body.setSize(96, 70, true);
        body.setBackgroundColor(0, 0, 0, 0.85f);
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
        cb.setPosition(50f, 99f, true);
        this.addChild(cb);
    }

}
