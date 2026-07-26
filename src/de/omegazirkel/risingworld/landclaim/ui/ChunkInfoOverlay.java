package de.omegazirkel.risingworld.landclaim.ui;

import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.Position;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

public class ChunkInfoOverlay {

    private final UIElement root;
    private final UILabel label;
    private final ChunkInfoOverlayState state = new ChunkInfoOverlayState();

    public ChunkInfoOverlay() {
        root = new UIElement();
        root.setPivot(Pivot.UpperCenter);
        root.style.position.set(Position.Absolute);
        root.style.left.set(50, Unit.Percent);
        root.style.top.set(1.5f, Unit.Percent);
        root.style.width.set(70, Unit.Percent);
        root.style.minWidth.set(50, Unit.Percent);
        root.style.maxWidth.set(90, Unit.Percent);
        root.style.height.set(34, Unit.Pixel);
        root.setBackgroundColor(0f, 0f, 0f, 0.78f);
        root.setBorder(1);
        root.setBorderColor(0.95f, 0.75f, 0.25f, 0.62f);
        root.setBorderEdgeRadius(6, false);

        label = new UILabel("...");
        label.setRichTextEnabled(true);
        label.setFont(Font.DefaultBold);
        label.setFontSize(18);
        label.setFontColor(0xFFFFFFFF);
        label.setPivot(Pivot.MiddleCenter);
        label.setPosition(50, 50, true);
        label.style.width.set(96, Unit.Percent);
        label.style.height.set(26, Unit.Pixel);
        label.setTextAlign(TextAnchor.MiddleCenter);
        label.setTextWrap(false);

        root.addChild(label);
    }

    public void updateText(String text) {
        if (state.updateText(text)) {
            label.setText(text);
        }
    }

    public void show(Player player) {
        if (state.markVisible()) {
            player.addUIElement(root);
        }
    }

    public void hide(Player player) {
        if (state.markHidden()) {
            player.removeUIElement(root);
        }
    }
}
