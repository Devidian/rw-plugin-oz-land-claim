package de.omegazirkel.risingworld.landclaim;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ClaimModeTest {
    @Test
    public void defaultsUnknownValuesToTimeBased() {
        assertEquals(ClaimMode.TIME_BASED, ClaimMode.parse(null));
        assertEquals(ClaimMode.TIME_BASED, ClaimMode.parse("future-mode"));
    }

    @Test
    public void acceptsConfiguredAndGermanAliases() {
        assertEquals(ClaimMode.ADMINISTRATIVE, ClaimMode.parse("ADMINISTRATIVE"));
        assertEquals(ClaimMode.LAND_PRICING, ClaimMode.parse("Grundstückspreise"));
        assertEquals(ClaimMode.CITY, ClaimMode.parse("Stadtmodus"));
    }
}
