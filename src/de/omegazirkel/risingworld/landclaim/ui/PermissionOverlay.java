package de.omegazirkel.risingworld.landclaim.ui;

import de.omegazirkel.risingworld.tools.ui.CursorManager;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.style.Pivot;

public class PermissionOverlay extends OZUIElement {
    public static final String ATTRIBUTE_KEY = "landclaim-overlay";

    public PermissionOverlay(Area area, Player player, Callback<Player> onClose) {
        super();
        setClickable(false);
        setPivot(Pivot.UpperLeft);
        setSize(100, 100, true);
        setBackgroundColor(0, 0, 0, 0.4f);

        AreaPermissionPanel panel = new AreaPermissionPanel(area, player, p -> {
            close(p, onClose);
        }, this);
        addChild(panel);
    }

    private void close(Player player, Callback<Player> onClose) {
        player.removeUIElement(this);
        player.deleteAttribute(ATTRIBUTE_KEY);
        CursorManager.hide(player);
        onClose.onCall(player);
    }

}
