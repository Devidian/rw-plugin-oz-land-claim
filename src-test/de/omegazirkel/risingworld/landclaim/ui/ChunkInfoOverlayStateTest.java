package de.omegazirkel.risingworld.landclaim.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChunkInfoOverlayStateTest {

    @Test
    public void keepsAnAlreadyVisibleOverlayMounted() {
        ChunkInfoOverlayState state = new ChunkInfoOverlayState();

        assertTrue(state.markVisible());
        assertFalse(state.markVisible());
    }

    @Test
    public void hidesAndShowsOnlyOnActualVisibilityTransitions() {
        ChunkInfoOverlayState state = new ChunkInfoOverlayState();

        assertFalse(state.markHidden());
        assertTrue(state.markVisible());
        assertTrue(state.markHidden());
        assertFalse(state.markHidden());
        assertTrue(state.markVisible());
    }

    @Test
    public void updatesOnlyChangedText() {
        ChunkInfoOverlayState state = new ChunkInfoOverlayState();

        assertTrue(state.updateText("00:00:10"));
        assertFalse(state.updateText("00:00:10"));
        assertTrue(state.updateText("00:00:09"));
    }
}
