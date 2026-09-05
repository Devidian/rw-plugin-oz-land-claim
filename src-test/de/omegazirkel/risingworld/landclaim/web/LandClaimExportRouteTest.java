package de.omegazirkel.risingworld.landclaim.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class LandClaimExportRouteTest {
    @Test
    public void cursorAcceptsMissingAndUnsignedLongValues() {
        assertNull(LandClaimExportRoute.cursor(null));
        assertEquals(Long.valueOf(42L), LandClaimExportRoute.cursor("42"));
    }

    @Test
    public void cursorRejectsNegativeAndOverflowValues() {
        assertInvalid("-1");
        assertInvalid("not-a-number");
        assertInvalid("9223372036854775808");
    }

    private void assertInvalid(String value) {
        try {
            LandClaimExportRoute.cursor(value);
            fail("Expected invalid cursor: " + value);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
