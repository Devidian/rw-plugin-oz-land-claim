package de.omegazirkel.risingworld.landclaim.ui;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.PluginSettings;
import de.omegazirkel.risingworld.tools.ui.SharedIndicatorProvider;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;

public class ClaimSaleIndicatorProvider implements SharedIndicatorProvider {
    private final PluginSettings settings = PluginSettings.getInstance();

    @Override
    public boolean showIndicator(Player player) {
        if (player == null || !settings.allowClaimSale || LandClaim.claimSaleListingService() == null) {
            return false;
        }
        Area currentArea = player.getCurrentArea();
        return currentArea != null
                && LandClaim.claimSaleListingService().activeListing(currentArea.getID()).isPresent();
    }

    @Override
    public String getIcon(Player player) {
        return "zone-sale-indicator";
    }
}
