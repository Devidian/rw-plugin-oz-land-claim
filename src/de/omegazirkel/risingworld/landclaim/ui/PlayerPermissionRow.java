package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.Dropdown;
import de.omegazirkel.risingworld.tools.ui.DropdownOption;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

public class PlayerPermissionRow {

    public static final PluginSettings s = PluginSettings.getInstance();

    public static final I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    public static TableRow build(
            Integer playerDBID,
            String currentPermission,
            Consumer<String> callback,
            Player forPlayer) {
        String ownerName = Server.getLastKnownPlayerName(playerDBID);
        String[] ownerUID = Server.getLastKnownPlayerUIDs(ownerName);
        String uidLabelText = "DBID: " + playerDBID;
        if (ownerUID != null && ownerUID.length > 0)
            uidLabelText = ownerUID[0];

        UILabel nameLabel = new UILabel(ownerName);
        UILabel uidLabel = new UILabel(uidLabelText);

        TableCell cellUID = new TableCell(uidLabel, 40); // 40%
        TableCell cellName = new TableCell(nameLabel, 40); // 40%

        if (currentPermission == null) {
            currentPermission = "";
        }

        if (currentPermission.equals(s.ownerAreaPermission)) {
            UILabel ownerLabel = new UILabel(t().get("TC_UI_PERMISSION_OWNER", forPlayer));
            TableCell cellOwner = new TableCell(ownerLabel, 20); // 20%
            return new TableRow(Arrays.asList(cellName, cellUID, cellOwner));
        }

        // Deprecated simple switch button friend / guest
        // SwitchButton switchButton = new
        // SwitchButton(currentPermission.equals(s.friendAreaPermission), callback);
        // TableCell cellSwitch = new TableCell(switchButton, 20); // 20%

        // TODO: new dropdown element to select permissions
        List<DropdownOption> options = List.of(
                // new DropdownOption(s.ownerAreaPermission, t().get("TC_UI_PERMISSION_OWNER", forPlayer)),
                new DropdownOption(s.residentAreaPermission, t().get("TC_UI_PERMISSION_RESIDENT", forPlayer)),
                new DropdownOption(s.friendAreaPermission, t().get("TC_UI_PERMISSION_FRIEND", forPlayer)),
                new DropdownOption(s.defaultAreaPermission, t().get("TC_UI_PERMISSION_GUEST", forPlayer)),
                new DropdownOption(s.prisonerAreaPermission, t().get("TC_UI_PERMISSION_PRISONER", forPlayer)),
                new DropdownOption(s.exiledAreaPermission, t().get("TC_UI_PERMISSION_EXILED", forPlayer)));
        Dropdown dropdown = new Dropdown(options, currentPermission, (selected) -> callback.accept(selected));

        TableCell cellDropdown = new TableCell(dropdown, 20); // 20%

        return new TableRow(Arrays.asList(cellName, cellUID, cellDropdown));
    }
}
