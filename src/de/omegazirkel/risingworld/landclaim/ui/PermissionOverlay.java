package de.omegazirkel.risingworld.landclaim.ui;

import de.omegazirkel.risingworld.tools.ui.CursorManager;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.style.Pivot;

public class PermissionOverlay extends OZUIElement {

    public PermissionOverlay(Area area, Player player, Callback<Player> onClose) {
        super();
        setClickable(false);
        setPivot(Pivot.UpperLeft);
        setSize(100, 100, true);
        setBackgroundColor(0, 0, 0, 0.4f);

        AreaPermissionPanel panel = new AreaPermissionPanel(area, player, p -> {
            p.removeUIElement(this);
            CursorManager.hide(p);
            onClose.onCall(p);
        }, this);
        addChild(panel);
    }

}
