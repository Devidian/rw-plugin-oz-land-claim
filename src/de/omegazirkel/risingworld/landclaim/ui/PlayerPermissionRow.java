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
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.Unit;

public class PlayerPermissionRow {
    private static final HashMap<Integer, OZUIElement> selectPanes = new HashMap<>();

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
        selectPane.setPivot(Pivot.MiddleLeft);
        selectPane.setPosition(75, 50, true);
        selectPane.setSize(10, 50, true);
        selectPane.setVisible(false);
        selectPane.setBackgroundColor(0, 0, 0, 0.85f);
        selectPane.setBorderColor(1, 1, 1, 0.4f);
        selectPane.setClickable(true);
        selectPane.setClickAction(event -> {
            selectPane.setVisible(false);
        });
        parentOverlay.addChild(selectPane);
        // forPlayer.addUIElement(selectPane);

        String buttonLabel = permissionLabelMap.get(currentPermission) == null
                ? areaPermissionLabelMap.get(currentPermission)
                : permissionLabelMap.get(currentPermission);

        CancelButton permissionButton = new CancelButton(buttonLabel, event -> {
            if (selectPanes.containsKey(forPlayer.getDbID())) {
                selectPanes.get(forPlayer.getDbID()).setVisible(false);
                selectPanes.remove(forPlayer.getDbID());
            }
            selectPanes.put(forPlayer.getDbID(), selectPane);
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
            cb.style.height.set(30, Unit.Pixel);
            cb.setPivot(Pivot.UpperCenter);
            cb.setPosition(50, 0, true);
            cb.style.top.set(2 + (row++ * 32), Unit.Pixel);
            selectPane.addChild(cb);
        }
        String defaultLabel = areaPermissionLabelMap.get(defaultPermission);
        CancelButton cb = new CancelButton(defaultLabel, event -> {
            selectPane.setVisible(false);
            callback.onCall(defaultPermission);
            permissionButton.setText(defaultLabel);
        });
        cb.style.width.set(98, Unit.Percent);
        cb.style.height.set(30, Unit.Pixel);
        cb.setPivot(Pivot.UpperCenter);
        cb.setPosition(50, 0, true);
        cb.style.top.set(2 + (row++ * 32), Unit.Pixel);
        selectPane.addChild(cb);

        TableCell workaroundCell = new TableCell(permissionButton, 20);

        return new TableRow(Arrays.asList(cellName, cellUID, cellStatus, workaroundCell));
    }
}
