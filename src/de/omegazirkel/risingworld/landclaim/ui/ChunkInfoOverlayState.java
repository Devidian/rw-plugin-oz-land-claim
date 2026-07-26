package de.omegazirkel.risingworld.landclaim.ui;

import java.util.Objects;

final class ChunkInfoOverlayState {

    private boolean visible;
    private String text;

    boolean markVisible() {
        if (visible) {
            return false;
        }
        visible = true;
        return true;
    }

    boolean markHidden() {
        if (!visible) {
            return false;
        }
        visible = false;
        return true;
    }

    boolean updateText(String nextText) {
        if (Objects.equals(text, nextText)) {
            return false;
        }
        text = nextText;
        return true;
    }
}
