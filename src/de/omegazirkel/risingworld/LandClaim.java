package de.omegazirkel.risingworld;

import java.nio.file.Path;

import de.omegazirkel.risingworld.landclaim.EconomyIntegration;
import de.omegazirkel.risingworld.landclaim.RenewZoneResetService;
import de.omegazirkel.risingworld.landclaim.db.ClaimSaleListingService;
import de.omegazirkel.risingworld.landclaim.db.ExtraClaimCapacityService;
import de.omegazirkel.risingworld.landclaim.db.RenewZoneConfigService;
import de.omegazirkel.risingworld.landclaim.db.LandPriceService;
import de.omegazirkel.risingworld.landclaim.db.CityService;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDeathEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.events.player.ui.PlayerToggleInventoryEvent;

/** Rising World entry point; claim behavior lives in {@link LandClaimRuntime}. */
public final class LandClaim extends LandClaimRuntime implements Listener, FileChangeListener {
    public static OZLogger logger() { return LandClaimRuntime.logger(); }
    public static OZLogger eventLogger() { return LandClaimRuntime.eventLogger(); }
    public static LandClaim getInstance() { return LandClaimRuntime.getInstance(); }
    public static ExtraClaimCapacityService extraClaimCapacityService() {
        return LandClaimRuntime.extraClaimCapacityService();
    }
    public static ClaimSaleListingService claimSaleListingService() {
        return LandClaimRuntime.claimSaleListingService();
    }
    public static RenewZoneConfigService renewZoneConfigService() {
        return LandClaimRuntime.renewZoneConfigService();
    }
    public static RenewZoneResetService renewZoneResetService() {
        return LandClaimRuntime.renewZoneResetService();
    }
    public static EconomyIntegration economyIntegration() { return LandClaimRuntime.economyIntegration(); }
    public static LandPriceService landPriceService() { return LandClaimRuntime.landPriceService(); }
    public static CityService cityService() { return LandClaimRuntime.cityService(); }

    @Override
    public void onEnable() {
        super.onEnable();
        registerEventListener(this);
    }

    @Override public void onDisable() { super.onDisable(); }
    @Override public void onSettingsChanged(Path settingsPath) { super.onSettingsChanged(settingsPath); }

    @Override @EventMethod
    public void onPlayerCommand(PlayerCommandEvent event) { super.onPlayerCommand(event); }
    @Override @EventMethod
    public void onPlayerEnterChunkEvent(PlayerEnterChunkEvent event) { super.onPlayerEnterChunkEvent(event); }
    @Override @EventMethod
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) { super.onPlayerDisconnectEvent(event); }
    @Override @EventMethod
    public void onPlayerConnectEvent(PlayerConnectEvent event) { super.onPlayerConnectEvent(event); }
    @Override @EventMethod
    public void onPlayerSpawnEvent(PlayerSpawnEvent event) { super.onPlayerSpawnEvent(event); }
    @Override @EventMethod
    public void onPlayerDeathEvent(PlayerDeathEvent event) { super.onPlayerDeathEvent(event); }
    @Override @EventMethod
    public void onPlayerToggleInventoryEvent(PlayerToggleInventoryEvent event) {
        super.onPlayerToggleInventoryEvent(event);
    }
}
