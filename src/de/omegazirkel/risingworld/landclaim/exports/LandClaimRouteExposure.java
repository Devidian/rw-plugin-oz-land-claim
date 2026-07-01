package de.omegazirkel.risingworld.landclaim.exports;

import de.omegazirkel.risingworld.landclaim.PluginSettings;

public record LandClaimRouteExposure(boolean claimSales) {

    public static LandClaimRouteExposure from(PluginSettings settings) {
        return new LandClaimRouteExposure(settings.exposeClaimSales);
    }
}
