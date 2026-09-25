package de.omegazirkel.risingworld.landclaim.ui;

import java.util.concurrent.atomic.AtomicBoolean;

final class ModalResponseGuard {
    private ModalResponseGuard() {
    }

    /**
     * A modal can receive the same pointer action twice before the engine has
     * removed it. Only the first response may mutate a claim or reopen a menu.
     */
    static void runOnce(AtomicBoolean responded, Runnable action) {
        if (responded.compareAndSet(false, true)) {
            action.run();
        }
    }

    static Runnable once(Runnable action) {
        AtomicBoolean called = new AtomicBoolean();
        return () -> runOnce(called, action);
    }
}
