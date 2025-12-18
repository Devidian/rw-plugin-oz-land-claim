package de.omegazirkel.risingworld.landclaim.ui;

import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.Unit;

public class ChunkInfoOverlay {

    private final UIElement root;
    private final UILabel label;
    private final float fontSize = 18;
    private final float minWidth = 500;

    public ChunkInfoOverlay(Player player) {
        root = new UIElement();
        root.setPivot(Pivot.UpperLeft);
        root.setPosition(10, 2, true);
        root.style.width.set(minWidth, Unit.Pixel);
        root.style.height.set(40, Unit.Pixel);
        root.setSize(minWidth, 40, false);
        root.setBackgroundColor(0f, 0f, 0f, 0.6f);
        root.setBorder(1);
        root.setBorderColor(1f, 1f, 1f, 0.25f);

        label = new UILabel("…");
        label.setFontSize(fontSize);
        label.setPivot(Pivot.MiddleCenter);
        label.setPosition(50, 50, true);

        root.addChild(label);

        player.addUIElement(root);
    }

    public void updateText(String text) {
        label.setText(text);
        
        root.style.width.set(Math.max(minWidth, (text.length() * fontSize) + 30), Unit.Pixel);
    }

    public void remove(Player player) {
        player.removeUIElement(root);
    }
}
