package de.omegazirkel.risingworld.landclaim;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PropertyClearanceServiceTest {
    @Test
    public void aggregatesRecipeOutputsBeforeRounding() {
        assertEquals(2, PropertyClearanceService.returnedRecipeAmount(4, 1, 2));
        assertEquals(2, PropertyClearanceService.returnedRecipeAmount(3, 1, 2));
    }
}
