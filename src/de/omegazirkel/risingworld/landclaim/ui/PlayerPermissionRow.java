package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.CancelButton;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import net.risingworld.api.Server;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

public class PlayerPermissionRow {
    private static final HashMap<Integer, OZUIElement> selectPanes = new HashMap<>();
    private static final int OPTION_HEIGHT = 30;
    private static final int OPTION_GAP = 2;
    private static final int OPTION_COUNT = 5;
    private static final int SELECT_PANE_PADDING = 4;
    private static final int SELECT_PANE_HEADER_HEIGHT = 28;
    private static final int OPTION_TOP_OFFSET = SELECT_PANE_PADDING + SELECT_PANE_HEADER_HEIGHT + OPTION_GAP;
    private static final int SELECT_PANE_HEIGHT = OPTION_TOP_OFFSET
            + (OPTION_COUNT * (OPTION_HEIGHT + OPTION_GAP))
            + SELECT_PANE_PADDING;
    private static final float SELECT_PANE_TOP_PERCENT = 31f;

    public static final PluginSettings s = PluginSettings.getInstance();

    public static final I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    public static TableRow build(
            Integer playerDBID,
            boolean isOnline,
            String currentPermission,
            String defaultPermission,
            Callback<String> callback,
            Player forPlayer,
            UIElement parentOverlay) {
        String ownerName = Server.getLastKnownPlayerName(playerDBID);
        if (ownerName == null || ownerName.isBlank()) {
            ownerName = "Unknown Player #" + playerDBID;
        }

        String[] ownerUID = Server.getLastKnownPlayerUIDs(ownerName);
        String uidLabelText = "DBID: " + playerDBID;
        if (ownerUID != null && ownerUID.length > 0)
            uidLabelText = ownerUID[0];

        UILabel nameLabel = new UILabel(ownerName);
        UILabel uidLabel = new UILabel(uidLabelText);
        UILabel statusLabel = new UILabel(
                isOnline
                        ? "<color=#44dd44>" + t().get("TC_UI_STATUS_ONLINE", forPlayer) + "</color>"
                        : "<color=#dd4444>" + t().get("TC_UI_STATUS_OFFLINE", forPlayer) + "</color>");

        TableCell cellName = new TableCell(nameLabel, 30);
        TableCell cellUID = new TableCell(uidLabel, 35);
        TableCell cellStatus = new TableCell(statusLabel, 15);

        if (currentPermission == null) {
            currentPermission = "";
        }

        if (currentPermission.equals(s.ownerAreaPermission)) {
            UILabel ownerLabel = new UILabel(t().get("TC_UI_PERMISSION_OWNER", forPlayer));
            TableCell cellOwner = new TableCell(ownerLabel, 20);
            return new TableRow(Arrays.asList(cellName, cellUID, cellStatus, cellOwner));
        }

        // List<DropdownOption> options = List.of(
        // // new DropdownOption(s.ownerAreaPermission,
        // t().get("TC_UI_PERMISSION_OWNER",
        // // forPlayer)),
        // new DropdownOption(s.residentAreaPermission,
        // t().get("TC_UI_PERMISSION_RESIDENT", forPlayer)),
        // new DropdownOption(s.friendAreaPermission, t().get("TC_UI_PERMISSION_FRIEND",
        // forPlayer)),
        // new DropdownOption(s.defaultAreaPermission, t().get("TC_UI_PERMISSION_GUEST",
        // forPlayer)),
        // new DropdownOption(s.prisonerAreaPermission,
        // t().get("TC_UI_PERMISSION_PRISONER", forPlayer)),
        // new DropdownOption(s.exiledAreaPermission, t().get("TC_UI_PERMISSION_EXILED",
        // forPlayer)));
        // Dropdown dropdown = new Dropdown(options, currentPermission, (selected) ->
        // callback.accept(selected));

        // TableCell cellDropdown = new TableCell(dropdown, 20); // 20%

        // area permissions
        Map<String, String> areaPermissionLabelMap = Map.of(
                s.defaultAreaPermission, t().get("TC_UI_PERMISSION_GUEST", forPlayer),
                s.specialAreaPermission, t().get("TC_UI_PERMISSION_SPECIAL", forPlayer),
                s.specialPvPAreaPermission, t().get("TC_UI_PERMISSION_PVP", forPlayer),
                s.specialRestAreaPermission, t().get("TC_UI_PERMISSION_REST", forPlayer),
                s.specialTrapAreaPermission, t().get("TC_UI_PERMISSION_TRAP", forPlayer));

        // WORKAROUND fix dropdown!
        Map<String, String> permissionLabelMap = Map.of(
                // s.ownerAreaPermission, t().get("TC_UI_PERMISSION_OWNER", forPlayer),
                s.residentAreaPermission, t().get("TC_UI_PERMISSION_RESIDENT", forPlayer),
                s.friendAreaPermission, t().get("TC_UI_PERMISSION_FRIEND", forPlayer),
                s.prisonerAreaPermission, t().get("TC_UI_PERMISSION_PRISONER", forPlayer),
                s.exiledAreaPermission, t().get("TC_UI_PERMISSION_EXILED", forPlayer));

        OZUIElement selectPane = new OZUIElement();
        selectPane.setBorder(2);
        selectPane.setPivot(Pivot.UpperRight);
        selectPane.setPosition(94, SELECT_PANE_TOP_PERCENT, true);
        selectPane.style.width.set(12, Unit.Percent);
        selectPane.style.height.set(SELECT_PANE_HEIGHT, Unit.Pixel);
        selectPane.setVisible(false);
        selectPane.setBackgroundColor(0, 0, 0, 0.85f);
        selectPane.setBorderColor(0.95f, 0.75f, 0.25f, 0.6f);
        selectPane.setClickable(true);
        selectPane.setClickAction(event -> {
            selectPane.setVisible(false);
        });

        UILabel selectPaneTitle = new UILabel(ownerName);
        selectPaneTitle.setPivot(Pivot.UpperCenter);
        selectPaneTitle.setPosition(50, SELECT_PANE_PADDING, true);
        selectPaneTitle.style.width.set(96, Unit.Percent);
        selectPaneTitle.style.height.set(SELECT_PANE_HEADER_HEIGHT, Unit.Pixel);
        selectPaneTitle.setFont(Font.DefaultBold);
        selectPaneTitle.setFontSize(14);
        selectPaneTitle.setTextAlign(TextAnchor.MiddleCenter);
        selectPane.addChild(selectPaneTitle);

        String buttonLabel = permissionLabelMap.get(currentPermission) == null
                ? areaPermissionLabelMap.get(currentPermission)
                : permissionLabelMap.get(currentPermission);

        CancelButton permissionButton = new CancelButton(buttonLabel, event -> {
            if (selectPanes.containsKey(forPlayer.getDbID())) {
                selectPanes.get(forPlayer.getDbID()).setVisible(false);
                selectPanes.get(forPlayer.getDbID()).removeFromParent();
                selectPanes.remove(forPlayer.getDbID());
            }
            selectPanes.put(forPlayer.getDbID(), selectPane);
            selectPane.removeFromParent();
            parentOverlay.addChild(selectPane);
            selectPane.setVisible(true);
        });
        permissionButton.setPivot(Pivot.MiddleCenter);
        permissionButton.setPosition(50, 50, false);
        permissionButton.setSize(98, 98, true);

        Integer row = 0;
        for (Map.Entry<String, String> entry : permissionLabelMap.entrySet()) {
            CancelButton cb = new CancelButton(entry.getValue(), event -> {
                selectPane.setVisible(false);
                callback.onCall(entry.getKey());
                permissionButton.setText(entry.getValue());
            });
            cb.style.width.set(98, Unit.Percent);
            cb.style.height.set(OPTION_HEIGHT, Unit.Pixel);
            cb.setPivot(Pivot.UpperCenter);
            cb.setPosition(50, 0, true);
            cb.style.top.set(OPTION_TOP_OFFSET + (row++ * (OPTION_HEIGHT + OPTION_GAP)), Unit.Pixel);
            selectPane.addChild(cb);
        }
        String defaultLabel = areaPermissionLabelMap.get(defaultPermission);
        CancelButton cb = new CancelButton(defaultLabel, event -> {
            selectPane.setVisible(false);
            callback.onCall(defaultPermission);
            permissionButton.setText(defaultLabel);
        });
        cb.style.width.set(98, Unit.Percent);
        cb.style.height.set(OPTION_HEIGHT, Unit.Pixel);
        cb.setPivot(Pivot.UpperCenter);
        cb.setPosition(50, 0, true);
        cb.style.top.set(OPTION_TOP_OFFSET + (row++ * (OPTION_HEIGHT + OPTION_GAP)), Unit.Pixel);
        selectPane.addChild(cb);

        TableCell workaroundCell = new TableCell(permissionButton, 20);

        return new TableRow(Arrays.asList(cellName, cellUID, cellStatus, workaroundCell));
    }
}
