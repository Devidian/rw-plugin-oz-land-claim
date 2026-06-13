package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.PlayerDatabaseHelper;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.style.Unit;

public class AreaPermissionPanel extends UIElement {

    private static final PluginSettings s = PluginSettings.getInstance();
    private static final float TABLE_SCROLL_BODY_HEIGHT = 400f;

    private static I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    private Player uiPlayer;

    public AreaPermissionPanel(Area area, Player player, UIElement parentOverlay) {
        this.style.width.set(100, Unit.Percent);
        this.style.height.set(100, Unit.Percent);
        this.uiPlayer = player;

        setupTable(area, player, parentOverlay);
    }

    private void setupTable(Area area, Player player, UIElement parentOverlay) {
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
        this.addChild(table);
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

}
