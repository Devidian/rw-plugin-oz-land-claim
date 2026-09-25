package de.omegazirkel.risingworld.landclaim.ui;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class UIDialogFactoryTest {
    @Test
    public void handlesOnlyTheFirstResponseFromOneModal() {
        AtomicBoolean responded = new AtomicBoolean();
        AtomicInteger calls = new AtomicInteger();

        ModalResponseGuard.runOnce(responded, calls::incrementAndGet);
        ModalResponseGuard.runOnce(responded, calls::incrementAndGet);

        assertEquals(1, calls.get());
    }

    @Test
    public void handlesOnlyTheFirstDelayedCloseCallback() {
        AtomicInteger calls = new AtomicInteger();
        Runnable delayedClose = ModalResponseGuard.once(calls::incrementAndGet);

        delayedClose.run();
        delayedClose.run();

        assertEquals(1, calls.get());
    }
}
