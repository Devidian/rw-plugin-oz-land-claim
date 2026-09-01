package de.omegazirkel.risingworld.landclaim.ui;

import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.Position;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

/** Compact, non-interactive HUD for voluntary claim-time transparency. */
final class TimeMeasurementOverlay {
    private final UIElement root = new UIElement();
    private final UILabel label = new UILabel();
    private final ChunkInfoOverlayState state = new ChunkInfoOverlayState();

    TimeMeasurementOverlay() {
        root.setPivot(Pivot.UpperCenter);
        root.style.position.set(Position.Absolute);
        root.style.left.set(50, Unit.Percent);
        root.style.top.set(5.5f, Unit.Percent);
        root.style.width.set(240, Unit.Pixel);
        root.style.height.set(70, Unit.Pixel);
        root.setBackgroundColor(0f, 0f, 0f, 0.68f);
        root.setBorder(1);
        root.setBorderColor(0.95f, 0.75f, 0.25f, 0.52f);
        root.setBorderEdgeRadius(5, false);

        label.setRichTextEnabled(true);
        label.setFont(Font.Default);
        label.setFontSize(14);
        label.setFontColor(0xFFFFFFFF);
        label.setPivot(Pivot.MiddleCenter);
        label.setPosition(50, 50, true);
        label.style.width.set(92, Unit.Percent);
        label.style.height.set(92, Unit.Percent);
        label.setTextAlign(TextAnchor.MiddleLeft);
        root.addChild(label);
    }

    void update(Player player, String text) {
        if (state.updateText(text)) label.setText(text);
        if (state.markVisible()) player.addUIElement(root);
    }

    void hide(Player player) {
        if (state.markHidden()) player.removeUIElement(root);
    }
}
