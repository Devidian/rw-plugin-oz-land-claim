package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Map;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlay;
import net.risingworld.api.Server;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;

public class PermissionOverlay extends BasePluginOverlay {
    public static final String ATTRIBUTE_KEY = "landclaim-overlay";
    private static final PluginSettings SETTINGS = PluginSettings.getInstance();

    private final Area area;

    public PermissionOverlay(Area area, Player player, Callback<Player> onClose) {
        super(player, onClose);
        this.area = area;

        rebuild();
        body.addChild(new AreaPermissionPanel(area, player, this));
    }

    @Override
    protected I18n t() {
        return I18n.getInstance(LandClaim.name);
    }

    @Override
    protected String titleText() {
        return t().get("tc.ui.area.permissions.title", uiPlayer)
                .replace("PH_AREA_NAME", area.getName() != null ? area.getName() : "N/A");
    }

    @Override
    protected String descriptionText() {
        return t().get("tc.ui.area.permissions.subtitle", uiPlayer);
    }

    @Override
    protected String legendText() {
        return t().get("tc.ui.area.permissions.info", uiPlayer)
                .replace("PH_AREA_OWNER", resolveOwnerName());
    }

    @Override
    protected void close() {
        uiPlayer.deleteAttribute(ATTRIBUTE_KEY);
        super.close();
    }

    private String resolveOwnerName() {
        Map<Integer, String> ownerPermissions = area.getAllPlayerPermissions();
        if (ownerPermissions != null) {
            for (Map.Entry<Integer, String> entry : ownerPermissions.entrySet()) {
                if (SETTINGS.ownerAreaPermission.equals(entry.getValue())) {
                    String ownerName = Server.getLastKnownPlayerName(entry.getKey());
                    return ownerName != null ? ownerName : "SYSTEM";
                }
            }
        }
        return "SYSTEM";
    }
}
