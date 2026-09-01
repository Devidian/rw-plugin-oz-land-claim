package de.omegazirkel.risingworld.landclaim;

import de.omegazirkel.risingworld.LandClaim;
import de.omegazirkel.risingworld.landclaim.db.ExtraClaimCapacityService;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProvider;
import net.risingworld.api.objects.Player;

public class LandClaimPluginInfoStatusProvider implements PluginInfoStatusProvider {
    private final LandClaim plugin;
    private final String pluginName;
    private final String version;

    public LandClaimPluginInfoStatusProvider(LandClaim plugin, String version) {
        this.plugin = plugin;
        this.pluginName = LandClaim.name == null || LandClaim.name.isBlank() ? "OZ - Land Claim" : LandClaim.name;
        this.version = version == null ? "" : version;
    }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public String getInfo(Player player) {
        return t().get("tc.landclaim.info.panel.info", player)
                .replace("PH_PLUGIN_NAME", pluginName)
                .replace("PH_VERSION", version)
                .replace("PH_PLUGIN_CMD", "lc");
    }

    @Override
    public String getStatus(Player player) {
        PluginSettings settings = PluginSettings.getInstance();
        EconomyIntegration economy = LandClaim.economyIntegration();
        return t().get("tc.landclaim.info.panel.status", player)
                .replace("PH_PLAYER_CLAIMS", String.valueOf(plugin.playerClaimCount(player)))
                .replace("PH_PLAYER_MAX_CLAIMS", String.valueOf(plugin.playerMaxClaims(player)))
                .replace("PH_PURCHASED_EXTRA_CLAIMS", purchasedExtraClaims(player))
                .replace("PH_CLAIM_MODE", settings.claimMode.name())
                .replace("PH_CLAIM_SALE", String.valueOf(settings.allowClaimSale))
                .replace("PH_EXTRA_CLAIM_SHOP", String.valueOf(settings.enableExtraClaimShopOffer))
                .replace("PH_WALLET_STATUS", available(economy != null && economy.hasSystemAccountApi()))
                .replace("PH_SHOP_STATUS", available(economy != null && economy.isShopAvailable()))
                .replace("PH_AUTO_REMOVAL", String.valueOf(settings.enableAutoClaimRemoval))
                .replace("PH_LANGUAGE", player.getLanguage() + " / " + de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player))
                .replace("PH_USEDLANG", t().getLanguageUsed(de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player)))
                .replace("PH_LANG_AVAILABLE", t().getLanguageAvailable());
    }

    private I18n t() {
        return I18n.getInstance(plugin);
    }

    private static String available(boolean value) {
        return value ? "available" : "missing";
    }

    private static String purchasedExtraClaims(Player player) {
        ExtraClaimCapacityService service = LandClaim.extraClaimCapacityService();
        return service == null ? "-" : String.valueOf(service.getPurchasedCapacity(player));
    }
}
