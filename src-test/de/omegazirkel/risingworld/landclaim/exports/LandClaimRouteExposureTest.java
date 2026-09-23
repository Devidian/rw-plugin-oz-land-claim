package de.omegazirkel.risingworld.landclaim.exports;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import de.omegazirkel.risingworld.landclaim.PluginSettings;

public class LandClaimRouteExposureTest {

    @Test
    public void loadsClaimSaleExposureFlagFromSettings() throws Exception {
        Path directory = Files.createTempDirectory("oz-land-claim-settings-");
        Path settings = directory.resolve("settings.world.json");
        Files.writeString(directory.resolve("settings.default.json"),
                "{\"exposeClaimSales\":true,\"exposeRenewZones\":true}");
        Files.writeString(settings, "{\"exposeClaimSales\":false,\"exposeRenewZones\":false}");

        PluginSettings pluginSettings = PluginSettings.getInstance();
        pluginSettings.initSettings(settings.toString());

        assertFalse(LandClaimRouteExposure.from(pluginSettings).claimSales());
        assertFalse(LandClaimRouteExposure.from(pluginSettings).renewZones());

        Files.writeString(settings, "");
        pluginSettings.initSettings(settings.toString());

        assertTrue(LandClaimRouteExposure.from(pluginSettings).claimSales());
        assertTrue(LandClaimRouteExposure.from(pluginSettings).renewZones());
    }
}
